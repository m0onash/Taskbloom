package com.taskbloom;

import com.taskbloom.model.TareaCompuesta;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.service.GestorTareas;
import com.taskbloom.state.EstadoEnCurso;
import com.taskbloom.state.EstadoTerminada;
import java.time.LocalDateTime;

/** Ejecutar con java -ea para comprobar Composite y State sin interfaz. */
public final class PruebaDominio {
    public static void main(String[] args) {
        GestorTareas gestor = new GestorTareas();
        TareaCompuesta padre = gestor.crearTareaCompuesta();
        TareaSimple hija = gestor.crearTareaSimple();
        gestor.agregarSubtarea(padre.obtenerId(), hija.obtenerId());
        assert gestor.consultarSubtareas(padre.obtenerId()).size() == 1;
        assert gestor.obtenerTareasPrincipales().size() == 1;
        assert !hija.cambiarEstado(new EstadoTerminada());
        assert hija.cambiarEstado(new EstadoEnCurso());
        assert hija.obtenerEstado().obtenerNombre().equals("En curso");
        gestor.modificarTarea(hija.obtenerId(), "Subtarea editada", "", 3, LocalDateTime.now());
        assert hija.obtenerTitulo().equals("Subtarea editada");
        gestor.eliminarTarea(padre.obtenerId());
        assert gestor.obtenerTareasPrincipales().isEmpty();
        System.out.println("OK: Composite y State funcionan.");
    }
}
