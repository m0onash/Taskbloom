package com.taskbloom.model;

import java.time.LocalDateTime;
import java.util.List;

/** Leaf del patrón Composite. */
public final class TareaSimple extends ComponenteTarea {
    public TareaSimple(String id, String titulo, String descripcion,
                       int prioridad, LocalDateTime fechaLimite) {
        super(id, titulo, descripcion, prioridad, fechaLimite);
    }

    @Override public List<ComponenteTarea> obtenerSubtareas() {
        return List.of();
    }
}
