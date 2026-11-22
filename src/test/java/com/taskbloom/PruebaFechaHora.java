package com.taskbloom;

import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.observer.GestorNotificaciones;
import com.taskbloom.observer.GestorRecordatorios;
import com.taskbloom.observer.ObservadorUsuario;
import com.taskbloom.observer.Recordatorio;
import com.taskbloom.strategy.OrdenarPorFecha;
import java.time.LocalDateTime;
import java.util.List;

/** Ejecutar con -ea: vencimientos con minutos y reprogramación Observer. */
public final class PruebaFechaHora {
    public static void main(String[] args) {
        Usuario persona = new Usuario("u1", "Mi perfil");
        LocalDateTime fecha = LocalDateTime.of(2030, 8, 23, 15, 30);
        TareaSimple tarea = new TareaSimple("t1", "Entrega", "", 2, fecha);
        tarea.asignarResponsables(List.of(persona));
        GestorNotificaciones bandeja = new GestorNotificaciones();
        GestorRecordatorios recordatorios = new GestorRecordatorios();
        recordatorios.suscribir(new ObservadorUsuario(persona, bandeja));

        Recordatorio unaHoraAntes = recordatorios.programarRecordatorio(tarea, 1);
        assert unaHoraAntes.obtenerFechaAviso().equals(LocalDateTime.of(2030, 8, 23, 14, 30));
        assert recordatorios.verificarVencimientos(LocalDateTime.of(2030, 8, 23, 14, 29)) == 0;
        assert recordatorios.verificarVencimientos(LocalDateTime.of(2030, 8, 23, 14, 30)) == 1;
        assert recordatorios.verificarVencimientos(LocalDateTime.of(2030, 8, 23, 14, 31)) == 0;
        assert bandeja.obtenerTodas().size() == 1;
        assert bandeja.obtenerTodas().getFirst().obtenerMensaje().contains("15:30");

        // Editar solo la hora: aunque el día sea el mismo, es una nueva programación.
        tarea.actualizarDatos("Entrega", "", 2, LocalDateTime.of(2030, 8, 23, 16, 45));
        Recordatorio nuevo = recordatorios.programarRecordatorio(tarea, 1);
        assert !nuevo.obtenerId().equals(unaHoraAntes.obtenerId());
        assert !nuevo.estaEnviado();
        assert nuevo.obtenerFechaAviso().equals(LocalDateTime.of(2030, 8, 23, 15, 45));

        // Al vencer: anticipación = cero, distinta de «Sin recordatorio» (null en la UI).
        Recordatorio enVencimiento = recordatorios.programarRecordatorio(tarea, 0);
        assert enVencimiento.obtenerFechaAviso().equals(tarea.obtenerFechaLimite());
        assert recordatorios.verificarVencimientos(LocalDateTime.of(2030, 8, 23, 16, 44)) == 0;
        assert recordatorios.verificarVencimientos(LocalDateTime.of(2030, 8, 23, 16, 45)) == 1;

        // Strategy por fecha usa también la hora, no solo el día.
        TareaSimple anterior = new TareaSimple("t2", "Anterior", "", 2,
                LocalDateTime.of(2030, 8, 23, 16, 40));
        assert new OrdenarPorFecha().ordenar(List.of(tarea, anterior)).getFirst() == anterior;

        boolean negativaRechazada = false;
        try { recordatorios.programarRecordatorio(tarea, -1); }
        catch (IllegalArgumentException ex) { negativaRechazada = true; }
        assert negativaRechazada;
        assert recordatorios.buscarPorTarea(tarea.obtenerId()) == enVencimiento;
        System.out.println("OK: fecha/hora por minuto, Strategy y reprogramación Observer.");
    }
}
