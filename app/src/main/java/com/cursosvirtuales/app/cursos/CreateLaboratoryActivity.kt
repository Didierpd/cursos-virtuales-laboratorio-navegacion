package com.cursosvirtuales.app.cursos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class CreateLaboratoryActivity : AppCompatActivity() {
    private val courseId by lazy { intent.getLongExtra(CourseDetailActivity.EXTRA_CURSO_ID,-1) }
    private lateinit var titleField: EditText
    private lateinit var objectiveField: EditText
    private lateinit var guideField: EditText
    private lateinit var filesLabel: TextView
    private lateinit var errorLabel: TextView
    private lateinit var save: Button
    private lateinit var choose: Button
    private var selected=arrayListOf<Uri>()
    private var busy=false
    private val picker=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if(uris.size > 10) errorLabel.text="Puedes seleccionar como máximo 10 recursos."
        else if(uris.isNotEmpty()) {
            selected=ArrayList(uris.distinct())
            selected.forEach { uri -> try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch(_: SecurityException) {} }
            showFiles()
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); title="Crear práctica de laboratorio"
        if(!CourseAccess.allow(this,courseId,true)) { finish(); return }
        val form=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,16,24,24) }
        setContentView(ScrollView(this).apply { addView(form) })
        form.addView(TextView(this).apply { text="${CourseRepository(this@CreateLaboratoryActivity).buscarPorId(courseId).nombre}\nEl título, el objetivo y la guía son obligatorios." })
        fun field(hintText: String, lines: Int): EditText = EditText(this).apply {
            hint=hintText; minLines=lines; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            form.addView(this)
        }
        titleField=field("Título de la práctica",1)
        objectiveField=field("Objetivo",2)
        guideField=field("Guía: materiales, pasos y resultado esperado",5)
        titleField.setText(state?.getString("title") ?: "")
        objectiveField.setText(state?.getString("objective") ?: "")
        guideField.setText(state?.getString("guide") ?: "")
        selected=state?.getStringArrayList("files")?.mapTo(arrayListOf()) { Uri.parse(it) } ?: arrayListOf()
        filesLabel=TextView(this); form.addView(filesLabel); showFiles()
        choose=Button(this).apply { text="Seleccionar recursos"; isAllCaps=false; setOnClickListener { picker.launch(arrayOf("*/*")) } }; form.addView(choose)
        errorLabel=TextView(this).apply { setTextColor(android.graphics.Color.RED) }; form.addView(errorLabel)
        save=Button(this).apply { text="Publicar práctica"; isAllCaps=false; setOnClickListener { publish() } }; form.addView(save)
    }
    override fun onResume() { super.onResume(); if(!CourseAccess.allow(this,courseId,true)) finish() }
    override fun onSaveInstanceState(state: Bundle) {
        if(::titleField.isInitialized) {
            state.putString("title",titleField.text.toString()); state.putString("objective",objectiveField.text.toString()); state.putString("guide",guideField.text.toString())
            state.putStringArrayList("files",ArrayList(selected.map { it.toString() }))
        }
        super.onSaveInstanceState(state)
    }
    private fun showFiles() { filesLabel.text=if(selected.isEmpty()) "Recursos opcionales: hasta 10 archivos de 20 MB cada uno." else selected.joinToString("\n") { runCatching { LearningRepository(this).displayName(it) }.getOrDefault("Archivo") } }
    private fun publish() {
        if(busy) return
        val labTitle=titleField.text.toString(); val objective=objectiveField.text.toString(); val guide=guideField.text.toString()
        if(labTitle.isBlank() || objective.isBlank() || guide.isBlank()) { errorLabel.text="Completa el título, el objetivo y la guía."; return }
        busy=true; save.isEnabled=false; choose.isEnabled=false; errorLabel.text="Guardando práctica…"
        requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        val resources=selected.toList(); val executor=Executors.newSingleThreadExecutor()
        executor.execute {
            val result=runCatching { LaboratoryRepository(applicationContext).create(courseId,labTitle,objective,guide,resources) }
            runOnUiThread {
                busy=false; requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                if(!isFinishing && !isDestroyed) {
                    if(result.isSuccess) { Toast.makeText(this,"Práctica publicada para los estudiantes inscritos.",Toast.LENGTH_LONG).show(); finish() }
                    else { save.isEnabled=true; choose.isEnabled=true; errorLabel.text=result.exceptionOrNull()?.message ?: "No se pudo guardar la práctica." }
                }
            }
        }
        executor.shutdown()
    }
}
