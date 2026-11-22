package com.taskbloom;

import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.observer.*;
import com.taskbloom.state.EstadoEnCurso;
import com.taskbloom.state.EstadoEnRevision;
import com.taskbloom.state.EstadoTerminada;
import java.time.LocalDateTime;
import java.util.List;

/** Prueba independiente de JavaFX y SQLite. Ejecutar con java -ea. */
public final class PruebaObserver {
    public static void main(String[] args) {
        LocalDateTime vencimiento = LocalDateTime.of(2030, 5, 15, 18, 0);
        Usuario ana = new Usuario("ana", "Ana");
        Usuario carlos = new Usuario("carlos", "Carlos");
        Usuario otro = new Usuario("otro", "Fuera de la tarea");
        TareaSimple tarea = new TareaSimple("t1", "Preparar entrega", "", 3, vencimiento);
        tarea.asignarResponsables(List.of(ana, carlos));
        GestorRecordatorios gestor = new GestorRecordatorios();
        GestorNotificaciones mensajes = new GestorNotificaciones();
        gestor.suscribir(new ObservadorUsuario(ana, mensajes));
        gestor.suscribir(new ObservadorUsuario(carlos, mensajes));
        gestor.suscribir(new ObservadorUsuario(otro, mensajes));

        Recordatorio r = gestor.programarRecordatorio(tarea, 24);
        assert r.obtenerFechaAviso().equals(vencimiento.minusHours(24));
        assert gestor.verificarVencimientos(vencimiento.minusHours(25)) == 0;
        assert mensajes.obtenerTodas().isEmpty();
        assert gestor.verificarVencimientos(vencimiento.minusHours(24)) == 1;
        assert r.estaEnviado();
        assert mensajes.obtenerTodas().size() == 2;
        assert mensajes.consultarNotificaciones(otro).isEmpty();
        assert gestor.verificarVencimientos(vencimiento) == 0;
        assert mensajes.obtenerTodas().size() == 2;

        mensajes.marcarTodasComoLeidas(ana);
        assert mensajes.contarNoLeidas(ana) == 0;
        assert mensajes.contarNoLeidas(carlos) == 1;
        assert gestor.programarRecordatorio(tarea, 24) == r;
        assert gestor.verificarVencimientos(vencimiento) == 0;

        tarea.actualizarDatos(tarea.obtenerTitulo(), "", 3, vencimiento.plusDays(2));
        Recordatorio r2 = gestor.programarRecordatorio(tarea, 24);
        assert !r.obtenerId().equals(r2.obtenerId());
        assert !r2.estaEnviado();
        mensajes.eliminarPorRecordatorios(java.util.Set.of(r.obtenerId()));
        assert mensajes.obtenerTodas().isEmpty();
        assert gestor.verificarVencimientos(vencimiento.plusDays(1)) == 1;
        assert mensajes.obtenerTodas().size() == 2;

        // Una tarea terminada no debe emitir avisos pendientes.
        TareaSimple finalizada = new TareaSimple("t2", "Terminada", "", 2, vencimiento);
        finalizada.asignarResponsables(List.of(ana));
        finalizada.cambiarEstado(new EstadoEnCurso());
        finalizada.cambiarEstado(new EstadoEnRevision());
        finalizada.cambiarEstado(new EstadoTerminada());
        gestor.programarRecordatorio(finalizada, 24);
        assert gestor.verificarVencimientos(vencimiento.plusDays(10)) == 0;
        System.out.println("OK: Observer notifica a los responsables, sin duplicados, y permite reprogramar.");
    }
}
