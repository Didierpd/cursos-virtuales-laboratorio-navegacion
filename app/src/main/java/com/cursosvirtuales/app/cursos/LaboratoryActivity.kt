package com.cursosvirtuales.app.cursos

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class LaboratoryActivity : AppCompatActivity() {
    private val courseId by lazy { intent.getLongExtra(CourseDetailActivity.EXTRA_CURSO_ID,-1) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); title="Laboratorio del curso" }
    override fun onResume() {
        super.onResume()
        if(!CourseAccess.allow(this,courseId,false)) { finish(); return }
        val form=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,16,24,24) }
        setContentView(ScrollView(this).apply { addView(form) })
        fun label(value: String, size: Float=16f) { form.addView(TextView(this).apply { text=value; textSize=size; setPadding(0,12,0,12); setTextIsSelectable(true) }) }
        label(CourseRepository(this).buscarPorId(courseId).nombre,22f)
        if(CourseAccess.allow(this,courseId,true)) form.addView(Button(this).apply {
            text="Nueva práctica"; isAllCaps=false
            setOnClickListener { startActivity(Intent(this@LaboratoryActivity,CreateLaboratoryActivity::class.java).putExtra(CourseDetailActivity.EXTRA_CURSO_ID,courseId)) }
        })
        val labs=LaboratoryRepository(this).list(courseId)
        if(labs.isEmpty()) label("Todavía no hay prácticas publicadas en este curso.")
        labs.forEach { lab ->
            label(lab.title,21f)
            label("Objetivo\n${lab.objective}")
            label("Guía de la práctica\n${lab.guide}")
            label("Recursos (${lab.resources.size})",18f)
            if(lab.resources.isEmpty()) label("Esta práctica no tiene archivos adjuntos.")
            lab.resources.forEach { resource -> form.addView(Button(this).apply {
                text="Abrir: ${resource.name}"; isAllCaps=false
                setOnClickListener {
                    if(!CourseAccess.allow(this@LaboratoryActivity,courseId,false)) { finish(); return@setOnClickListener }
                    try {
                        val uri=FileProvider.getUriForFile(this@LaboratoryActivity,"$packageName.files",File(filesDir,"laboratorios/${resource.path}"),resource.name)
                        startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,resource.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    } catch(e: android.content.ActivityNotFoundException) { Toast.makeText(this@LaboratoryActivity,"No hay un visor instalado para este tipo de archivo.",Toast.LENGTH_LONG).show() }
                      catch(e: Exception) { Toast.makeText(this@LaboratoryActivity,"No se pudo abrir el recurso.",Toast.LENGTH_LONG).show() }
                }
            }) }
        }
    }
}
