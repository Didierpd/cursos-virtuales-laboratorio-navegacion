package com.cursosvirtuales.app.cursos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.cursosvirtuales.app.model.Role
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class TaskDetailActivity : AppCompatActivity() {
    private val repo by lazy { LearningRepository(applicationContext) }
    private val taskId by lazy { intent.getLongExtra("task_id",-1) }
    private var selected = arrayListOf<Uri>()
    private var busy = false
    private lateinit var layout: LinearLayout
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            if (uris.size > 10) message("Puedes seleccionar como máximo 10 archivos.")
            else {
                selected = ArrayList(uris.distinct())
                selected.forEach { uri ->
                    try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
                }
            }
        }
        render()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getStringArrayList("selected")?.mapTo(arrayListOf()) { Uri.parse(it) } ?: arrayListOf()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("selected",ArrayList(selected.map { it.toString() })); super.onSaveInstanceState(outState)
    }
    override fun onResume() { super.onResume(); render() }
    private fun label(value: String, heading: Boolean = false) {
        layout.addView(TextView(this).apply { text = value; textSize = if (heading) 21f else 16f; setPadding(0,12,0,12); setTextIsSelectable(true) })
    }
    private fun button(value: String, enabled: Boolean = true, action: () -> Unit) {
        layout.addView(Button(this).apply { text = value; isAllCaps = false; isEnabled = enabled && !busy; setOnClickListener { action() } })
    }
    private fun date(value: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(value))
    private fun render() {
        if (isFinishing || isDestroyed) return
        val task = repo.task(taskId)
        if (task == null || !CourseAccess.allow(this,task.cursoId,false)) { message("La tarea no está disponible o tu sesión expiró."); finish(); return }
        title = "Detalle de tarea"
        layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,16,32,32) }
        setContentView(ScrollView(this).apply { addView(layout) })
        label(task.titulo,true); label(task.enunciado)
        label("Fecha límite: ${date(task.fechaLimite)}\nPuntaje máximo: ${task.puntaje}\n${LearningRepository.status(this,task)}")
        val teacher = CourseAccess.user(this)?.role == Role.DOCENTE
        label(if(repo.acceptsLate(task.cursoId)) "Política del curso: se aceptan entregas tardías; quedará registrado el retraso." else "Política del curso: no se aceptan entregas fuera de plazo.")
        val submissions = repo.submissions(task)
        if (!teacher && submissions.isEmpty()) {
            val active = repo.canSubmit(task)
            label(if (busy) "Guardando archivos…" else if (selected.isEmpty()) "Adjunta de 1 a 10 archivos (máximo 20 MB cada uno)." else selected.joinToString("\n") { runCatching { repo.displayName(it) }.getOrDefault("Archivo") })
            button("Seleccionar archivos",active) { picker.launch(arrayOf("*/*")) }
            button("Entregar tarea",active && selected.isNotEmpty()) {
                AlertDialog.Builder(this).setTitle("Confirmar entrega")
                    .setMessage("Se enviarán ${selected.size} archivo(s). La entrega quedará registrada con fecha y hora y no podrá reemplazarse.")
                    .setNegativeButton("Cancelar",null).setPositiveButton("Entregar") { _,_ -> submit() }.show()
            }
        }
        if (teacher) label("Entregas recibidas (${submissions.size})",true)
        if (teacher && submissions.isEmpty()) label("Todavía no hay entregas.")
        submissions.forEach { submission ->
            label(if (teacher) submission.name else "Tu entrega",true)
            label("Entregada el ${date(submission.date)}\n${repo.timing(submission)}\n" + (submission.grade?.let { "Calificación: $it / ${task.puntaje}\nRetroalimentación: ${submission.feedback}" } ?: "Pendiente de calificación"))
            submission.files.forEach { a -> button("Abrir: ${a.name}") {
                try {
                    val uri = FileProvider.getUriForFile(this,"$packageName.files",File(filesDir,"entregas/${a.path}"),a.name)
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,a.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                } catch (_: android.content.ActivityNotFoundException) { message("No hay una aplicación instalada que abra este tipo de archivo.") }
                  catch (_: Exception) { message("No se pudo abrir el archivo.") }
            } }
            if (teacher) button(if(submission.grade == null) "Calificar entrega" else "Editar calificación") { grade(task,submission) }
        }
    }
    private fun submit() {
        if (busy) return
        busy = true
        // Keep the current Activity while the bounded file copy runs off the UI thread.
        @Suppress("DEPRECATION")
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        render()
        val uris = selected.toList()
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val result = runCatching { repo.submit(taskId,uris) }
            runOnUiThread {
                busy = false
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                if (!isDestroyed && !isFinishing) {
                    if(result.isSuccess) { selected.clear(); message("Entrega registrada correctamente con fecha y hora.") }
                    else message(result.exceptionOrNull()?.message ?: "No se pudo guardar la entrega.")
                    render()
                }
            }
        }
        executor.shutdown()
    }
    private fun grade(task: CourseTask, submission: LearningRepository.Submission) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val score = EditText(this).apply { hint = "Nota entre 0 y ${task.puntaje}"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setText(submission.grade?.toString() ?: "") }
        val feedback = EditText(this).apply { hint = "Retroalimentación obligatoria"; minLines = 3; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; setText(submission.feedback) }
        form.addView(score); form.addView(feedback)
        val dialog = AlertDialog.Builder(this).setTitle("Calificar a ${submission.name}").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                val value = score.text.toString().replace(',','.').toDoubleOrNull() ?: error("Escribe una nota válida.")
                repo.grade(task.id,submission.id,value,feedback.text.toString())
                dialog.dismiss(); message("Calificación guardada."); render()
            } catch(e: Exception) { message(e.message ?: "No se pudo calificar.") }
        } }
        dialog.show()
    }
    private fun message(value: String) { Toast.makeText(this,value,Toast.LENGTH_LONG).show() }
}
