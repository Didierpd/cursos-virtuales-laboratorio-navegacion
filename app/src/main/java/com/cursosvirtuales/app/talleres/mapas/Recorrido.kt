package com.cursosvirtuales.app.talleres.mapas

import kotlin.math.*

data class Punto(val latitud: Double, val longitud: Double) {
    init {
        require(latitud in -90.0..90.0 && longitud in -180.0..180.0)
    }

    val coordenadas get() = "$latitud,$longitud"

    fun kilometrosHasta(otro: Punto): Double {
        val lat1 = Math.toRadians(latitud)
        val lat2 = Math.toRadians(otro.latitud)
        val deltaLon = Math.toRadians(otro.longitud - longitud)
        val h = sin((lat2 - lat1) / 2).pow(2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        return 6371.0088 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    companion object {
        fun leer(latitud: String, longitud: String): Punto? {
            val lat = latitud.trim().replace(',', '.').toDoubleOrNull() ?: return null
            val lon = longitud.trim().replace(',', '.').toDoubleOrNull() ?: return null
            return if (lat in -90.0..90.0 && lon in -180.0..180.0) Punto(lat, lon) else null
        }
    }
}

data class Parada(val nombre: String, val punto: Punto, val detalle: String = "")

object Recorrido {
    fun distancia(origen: Punto, paradas: List<Parada>): Double =
        (listOf(origen) + paradas.map { it.punto }).zipWithNext().sumOf { (a, b) -> a.kilometrosHasta(b) }

    fun ordenar(origen: Punto, paradas: List<Parada>): List<Parada> {
        val pendientes = paradas.toMutableList()
        val orden = mutableListOf<Parada>()
        var actual = origen
        while (pendientes.isNotEmpty()) {
            val siguiente = pendientes.minBy { actual.kilometrosHasta(it.punto) }
            pendientes.remove(siguiente)
            orden.add(siguiente)
            actual = siguiente.punto
        }
        val mejor = (if (distancia(origen, orden) <= distancia(origen, paradas)) orden else paradas).toMutableList()
        // Invertir un tramo conserva sus distancias internas. Solo cambian los bordes.
        for (pasada in 0 until 8) {
            var cambio = false
            for (i in mejor.indices) for (j in i + 1 until mejor.size) {
                val previo = if (i == 0) origen else mejor[i - 1].punto
                val inicio = mejor[i].punto
                val fin = mejor[j].punto
                val siguiente = mejor.getOrNull(j + 1)?.punto
                val antes = previo.kilometrosHasta(inicio) + (siguiente?.let(fin::kilometrosHasta) ?: 0.0)
                val despues = previo.kilometrosHasta(fin) + (siguiente?.let(inicio::kilometrosHasta) ?: 0.0)
                if (despues + 1e-8 < antes) {
                    mejor.subList(i, j + 1).reverse()
                    cambio = true
                }
            }
            if (!cambio) break
        }
        return mejor
    }

    // Hasta tres puntos intermedios por enlace, también en navegadores móviles.
    fun tramos(origen: Punto, paradas: List<Parada>): List<Pair<Punto, List<Parada>>> {
        var inicio = origen
        return paradas.chunked(4).map { grupo ->
            (inicio to grupo).also { inicio = grupo.last().punto }
        }
    }
}
