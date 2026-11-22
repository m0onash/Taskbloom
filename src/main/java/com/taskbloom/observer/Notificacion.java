package com.taskbloom.observer;

import com.taskbloom.model.Usuario;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Mensaje persistente de la bandeja de notificaciones de un usuario local. */
public final class Notificacion {
    private final String id;
    private final String mensaje;
    private final LocalDateTime fechaCreacion;
    private boolean leida;
    private final Usuario destinatario;
    private final String idRecordatorio;

    public Notificacion(Usuario usuario, Recordatorio recordatorio) {
        this(UUID.randomUUID().toString(), usuario, recordatorio.obtenerId(),
                "La tarea «" + recordatorio.obtenerTarea().obtenerTitulo() + "» vence el "
                        + recordatorio.obtenerTarea().obtenerFechaLimite().toLocalDate() + ".",
                LocalDateTime.now(), false);
    }

    /** Restauración desde SQLite sin alterar la fecha original. */
    public Notificacion(String id, Usuario destinatario, String idRecordatorio,
                        String mensaje, LocalDateTime fechaCreacion, boolean leida) {
        this.id = Objects.requireNonNull(id);
        this.destinatario = Objects.requireNonNull(destinatario);
        this.idRecordatorio = Objects.requireNonNull(idRecordatorio);
        this.mensaje = Objects.requireNonNull(mensaje);
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        this.leida = leida;
    }

    public String obtenerId() { return id; }
    public String obtenerMensaje() { return mensaje; }
    public LocalDateTime obtenerFechaCreacion() { return fechaCreacion; }
    public boolean estaLeida() { return leida; }
    public Usuario obtenerDestinatario() { return destinatario; }
    public String obtenerIdRecordatorio() { return idRecordatorio; }
    public void marcarComoLeida() { leida = true; }
}
