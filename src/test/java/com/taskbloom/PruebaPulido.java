package com.taskbloom;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.TareaCompuesta;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.observer.GestorNotificaciones;
import com.taskbloom.observer.GestorRecordatorios;
import com.taskbloom.observer.ObservadorUsuario;
import com.taskbloom.observer.Recordatorio;
import com.taskbloom.service.GestorTareas;
import com.taskbloom.state.EstadoEnCurso;
import com.taskbloom.state.EstadoEnRevision;
import java.time.LocalDateTime;
import java.util.List;

/** Prueba de regresión para edición de subtareas y compatibilidad con Observer. */
public final class PruebaPulido {
    public static void main(String[] args) {
        GestorTareas gestor = new GestorTareas();
        Usuario duena = new Usuario("u-1", "Luna");
        LocalDateTime fecha = LocalDateTime.of(2027, 1, 18, 15, 30);
        TareaSimple simple = gestor.crearTareaSimple("Preparar informe", "Datos", 3, fecha);
        simple.asignarResponsables(List.of(duena));
        assert simple.cambiarEstado(new EstadoEnCurso());
        assert simple.cambiarEstado(new EstadoEnRevision());
        String idOriginal = simple.obtenerId();
        GestorRecordatorios recordatorios = new GestorRecordatorios();
        GestorNotificaciones notificaciones = new GestorNotificaciones();
        recordatorios.suscribir(new ObservadorUsuario(duena, notificaciones));
        Recordatorio anterior = recordatorios.programarRecordatorio(simple, 24);

        TareaCompuesta compuesta = gestor.convertirEnCompuesta(idOriginal);
        recordatorios.actualizarReferenciaTarea(compuesta);
        assert gestor.obtenerTarea(idOriginal) == compuesta;
        assert gestor.obtenerTareasPrincipales().size() == 1;
        assert compuesta.obtenerEstado() instanceof EstadoEnRevision;
        assert compuesta.obtenerResponsables().getFirst() == duena;
        assert anterior.obtenerTarea() == compuesta;
        assert anterior.obtenerFechaAviso().equals(fecha.minusHours(24));
        assert gestor.convertirEnCompuesta(idOriginal) == compuesta;

        TareaSimple hija = gestor.crearTareaSimple("Borrador", "", 2, fecha);
        gestor.agregarSubtarea(compuesta.obtenerId(), hija.obtenerId());
        assert gestor.consultarSubtareas(idOriginal).size() == 1;
        assert gestor.obtenerTareasPrincipales().size() == 1;
        gestor.modificarTarea(hija.obtenerId(), "Borrador corregido", "", 2, fecha);
        assert hija.obtenerTitulo().equals("Borrador corregido");
        gestor.eliminarTarea(hija.obtenerId());
        assert gestor.consultarSubtareas(idOriginal).isEmpty();
        assert gestor.obtenerTareasPrincipales().size() == 1;

        gestor.modificarTarea(idOriginal, "Informe final", "Datos", 3, fecha.plusMinutes(15));
        Recordatorio nuevo = recordatorios.programarRecordatorio(compuesta, 24);
        assert !nuevo.obtenerId().equals(anterior.obtenerId());
        assert nuevo.obtenerFechaAviso().equals(fecha.plusMinutes(15).minusHours(24));
        recordatorios.verificarVencimientos(nuevo.obtenerFechaAviso());
        assert notificaciones.consultarNotificaciones(duena).size() == 1;
        recordatorios.verificarVencimientos(nuevo.obtenerFechaAviso().plusHours(1));
        assert notificaciones.consultarNotificaciones(duena).size() == 1;
        System.out.println("OK: conversión, edición y Observer sin duplicados.");
    }
}
