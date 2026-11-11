package com.cursosvirtuales.app.talleres.mapas

import org.junit.Assert.*
import org.junit.Test

class RecorridoTest {
    @Test fun ordenaParadasSinPerderGuías() {
        val origen = Punto(0.0, 0.0)
        val paradas = listOf(Parada("Lejana", Punto(0.0, 3.0)),
            Parada("Cercana", Punto(0.0, 1.0)), Parada("Intermedia", Punto(0.0, 2.0)))
        val orden = Recorrido.ordenar(origen, paradas)
        assertEquals(listOf("Cercana", "Intermedia", "Lejana"), orden.map { it.nombre })
        assertTrue(Recorrido.distancia(origen, orden) < Recorrido.distancia(origen, paradas))
    }

    @Test fun enlazaTramosSinOmitirParadas() {
        val origen = Punto(4.6, -74.1)
        val paradas = (1..11).map { Parada("Guía $it", Punto(4.6 + it / 1000.0, -74.1)) }
        val tramos = Recorrido.tramos(origen, paradas)
        assertEquals(listOf(4, 4, 3), tramos.map { it.second.size })
        assertEquals(origen, tramos.first().first)
        assertEquals(paradas[3].punto, tramos[1].first)
        assertEquals(paradas[7].punto, tramos[2].first)
        assertEquals(paradas, tramos.flatMap { it.second })
    }
}
