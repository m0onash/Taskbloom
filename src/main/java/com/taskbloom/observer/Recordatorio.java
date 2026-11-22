package com.taskbloom.observer;

import com.taskbloom.model.ComponenteTarea;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Recordatorio asociado a una tarea, con un único envío por programación. */
public final class Recordatorio {
    private final String id;
    private final LocalDateTime fechaAviso;
    private boolean enviado;
    private final ComponenteTarea tarea;
    private final int anticipacionHoras;

    public Recordatorio(ComponenteTarea tarea, int anticipacionHoras) {
        this(UUID.randomUUID().toString(), tarea, anticipacionHoras, false);
    }

    /** Constructor de restauración: conserva el ID y estado de envío. */
    public Recordatorio(String id, ComponenteTarea tarea, int anticipacionHoras, boolean enviado) {
        this.id = Objects.requireNonNull(id);
        this.tarea = Objects.requireNonNull(tarea);
        if (anticipacionHoras < 0) throw new IllegalArgumentException("La anticipación no puede ser negativa");
        this.anticipacionHoras = anticipacionHoras;
        this.fechaAviso = tarea.obtenerFechaLimite().minusHours(anticipacionHoras);
        this.enviado = enviado;
    }

    public String obtenerId() { return id; }
    public LocalDateTime obtenerFechaAviso() { return fechaAviso; }
    public boolean estaEnviado() { return enviado; }
    public ComponenteTarea obtenerTarea() { return tarea; }
    public int obtenerAnticipacionHoras() { return anticipacionHoras; }
    public void marcarComoEnviado() { enviado = true; }


}
