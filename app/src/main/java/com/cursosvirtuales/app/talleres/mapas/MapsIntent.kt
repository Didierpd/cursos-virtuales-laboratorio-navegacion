package com.cursosvirtuales.app.talleres.mapas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

internal object MapsIntent {
    private fun abrir(contexto: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            contexto.startActivity(Intent(intent).setPackage("com.google.android.apps.maps"))
        } catch (_: ActivityNotFoundException) {
            try {
                contexto.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(contexto, "Instala Google Maps o un navegador para abrir la ruta.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun url(accion: String) = Uri.Builder().scheme("https").authority("www.google.com")
        .appendPath("maps").appendPath(accion).appendPath("").appendQueryParameter("api", "1")

    fun ubicar(contexto: Context, punto: Punto) =
        abrir(contexto, url("search").appendQueryParameter("query", punto.coordenadas).build())

    fun ruta(contexto: Context, origen: Punto, paradas: List<Parada>, modo: String) {
        val enlace = url("dir").appendQueryParameter("origin", origen.coordenadas)
            .appendQueryParameter("destination", paradas.last().punto.coordenadas)
            .appendQueryParameter("travelmode", modo)
        if (paradas.size > 1) {
            enlace.appendQueryParameter("waypoints", paradas.dropLast(1).joinToString("|") { it.punto.coordenadas })
        }
        abrir(contexto, enlace.build())
    }
}
