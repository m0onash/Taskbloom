package com.taskbloom.strategy;

import com.taskbloom.model.ComponenteTarea;
import java.util.List;
import java.util.Objects;

/** Incluye tareas que tengan al usuario indicado entre sus responsables. */
public final class FiltrarPorResponsable implements EstrategiaFiltrado {
    private final String idResponsable;

    public FiltrarPorResponsable(String idResponsable) {
        this.idResponsable = Objects.requireNonNull(idResponsable, "El ID es obligatorio");
        if (idResponsable.isBlank()) throw new IllegalArgumentException("El ID no puede estar vacío");
    }

    @Override
    public List<ComponenteTarea> filtrar(List<ComponenteTarea> tareas) {
        return Objects.requireNonNull(tareas).stream()
                .filter(tarea -> tarea.obtenerResponsables().stream()
                        .anyMatch(usuario -> usuario.obtenerId().equals(idResponsable)))
                .toList();
    }
}
