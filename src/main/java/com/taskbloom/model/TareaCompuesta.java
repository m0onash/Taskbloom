package com.taskbloom.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Composite: puede contener tareas simples u otras tareas compuestas. */
public final class TareaCompuesta extends ComponenteTarea {
    private final List<ComponenteTarea> subtareas = new ArrayList<>();

    public TareaCompuesta(String id, String titulo, String descripcion,
                          int prioridad, LocalDateTime fechaLimite) {
        super(id, titulo, descripcion, prioridad, fechaLimite);
    }

    public void agregarSubtarea(ComponenteTarea subtarea) {
        Objects.requireNonNull(subtarea);
        if (subtarea == this || contieneId(subtarea, obtenerId())) {
            throw new IllegalArgumentException("Una tarea no puede contenerse a sí misma");
        }
        if (subtareas.stream().anyMatch(t -> t.obtenerId().equals(subtarea.obtenerId()))) {
            throw new IllegalArgumentException("La subtarea ya existe");
        }
        subtareas.add(subtarea);
    }

    public void eliminarSubtarea(String id) {
        subtareas.removeIf(t -> t.obtenerId().equals(id));
    }

    @Override public List<ComponenteTarea> obtenerSubtareas() {
        return List.copyOf(subtareas);
    }

    private static boolean contieneId(ComponenteTarea tarea, String id) {
        if (tarea.obtenerId().equals(id)) return true;
        return tarea.obtenerSubtareas().stream().anyMatch(t -> contieneId(t, id));
    }
}
