package com.taskbloom.strategy;

import com.taskbloom.model.ComponenteTarea;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Orden cronológico: primero la fecha límite más cercana. */
public final class OrdenarPorFecha implements EstrategiaOrdenamiento {
    @Override
    public List<ComponenteTarea> ordenar(List<ComponenteTarea> tareas) {
        List<ComponenteTarea> copia = new ArrayList<>(Objects.requireNonNull(tareas));
        copia.sort(Comparator.comparing(ComponenteTarea::obtenerFechaLimite));
        return List.copyOf(copia);
    }
}
