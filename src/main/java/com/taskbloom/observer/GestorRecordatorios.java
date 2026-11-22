package com.taskbloom.observer;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.Usuario;
import com.taskbloom.state.EstadoTerminada;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Subject: revisa vencimientos y publica únicamente a responsables de cada tarea. */
public final class GestorRecordatorios implements SujetoRecordatorio {
    private final List<ObservadorRecordatorio> observadores = new ArrayList<>();
    private final List<Recordatorio> recordatorios = new ArrayList<>();

    @Override public void suscribir(ObservadorRecordatorio observador) {
        Objects.requireNonNull(observador);
        if (!observadores.contains(observador)) observadores.add(observador);
    }

    @Override public void desuscribir(ObservadorRecordatorio observador) {
        observadores.remove(observador);
    }

    @Override public void notificarObservadores(Recordatorio recordatorio) {
        Set<String> destinatarios = recordatorio.obtenerTarea().obtenerResponsables().stream()
                .map(Usuario::obtenerId).collect(java.util.stream.Collectors.toSet());
        for (ObservadorRecordatorio observador : List.copyOf(observadores)) {
            if (observador instanceof ObservadorUsuario observadorUsuario
                    && destinatarios.contains(observadorUsuario.obtenerUsuario().obtenerId())) {
                observador.actualizar(recordatorio);
            }
        }
    }

    /** Un recordatorio activo por tarea: el mismo registro se reprograma al editar la fecha. */
    public Recordatorio programarRecordatorio(ComponenteTarea tarea, int horasAnticipacion) {
        Objects.requireNonNull(tarea);
        if (horasAnticipacion < 0) throw new IllegalArgumentException("La anticipación no puede ser negativa");
        Recordatorio existente = buscarPorTarea(tarea.obtenerId());
        if (existente != null) {
            // Sin cambios: mantener el aviso ya enviado y evitar notificaciones repetidas.
            if (existente.obtenerAnticipacionHoras() == horasAnticipacion
                    && existente.obtenerFechaAviso().equals(tarea.obtenerFechaLimite().minusHours(horasAnticipacion)))
                return existente;
            // Fecha o anticipación cambiadas: nuevo ciclo con un ID diferente.
            quitarRecordatorio(tarea.obtenerId());
        }
        Recordatorio nuevo = new Recordatorio(tarea, horasAnticipacion);
        recordatorios.add(nuevo);
        return nuevo;
    }

    public void registrarRecordatorioRestaurado(Recordatorio recordatorio) {
        if (buscarPorTarea(recordatorio.obtenerTarea().obtenerId()) != null)
            throw new IllegalArgumentException("La tarea ya tiene recordatorio");
        recordatorios.add(Objects.requireNonNull(recordatorio));
    }

    public Recordatorio buscarPorTarea(String tareaId) {
        return recordatorios.stream().filter(r -> r.obtenerTarea().obtenerId().equals(tareaId))
                .findFirst().orElse(null);
    }

    /** Mantiene el objeto recordatorio y sus notificaciones al convertir una tarea. */
    public void actualizarReferenciaTarea(ComponenteTarea tarea) {
        Recordatorio recordatorio = buscarPorTarea(tarea.obtenerId());
        if (recordatorio != null) recordatorio.actualizarReferencia(tarea);
    }

    public void quitarRecordatorio(String tareaId) {
        recordatorios.removeIf(r -> r.obtenerTarea().obtenerId().equals(tareaId));
    }

    public List<Recordatorio> obtenerRecordatorios() { return List.copyOf(recordatorios); }

    /** Usado por la UI y en pruebas con fecha inyectada, sin depender de esperar minutos. */
    public int verificarVencimientos(LocalDateTime ahora) {
        Objects.requireNonNull(ahora);
        int generados = 0;
        for (Recordatorio r : recordatorios) {
            if (!r.estaEnviado() && !r.obtenerFechaAviso().isAfter(ahora)
                    && !r.obtenerTarea().obtenerResponsables().isEmpty()
                    && !(r.obtenerTarea().obtenerEstado() instanceof EstadoTerminada)) {
                generarRecordatorio(r);
                generados++;
            }
        }
        return generados;
    }

    public int verificarVencimientos() { return verificarVencimientos(LocalDateTime.now()); }

    public void generarRecordatorio(Recordatorio recordatorio) {
        if (recordatorio.estaEnviado()) return;
        notificarObservadores(recordatorio);
        recordatorio.marcarComoEnviado();
    }
}
