package com.taskbloom.strategy;

import com.taskbloom.model.ComponenteTarea;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Prioridad Alta (3), Media (2), Baja (1); orden estable para empates. */
public final class OrdenarPorPrioridad implements EstrategiaOrdenamiento {
    @Override
    public List<ComponenteTarea> ordenar(List<ComponenteTarea> tareas) {
        List<ComponenteTarea> copia = new ArrayList<>(Objects.requireNonNull(tareas));
        copia.sort(Comparator.comparingInt(ComponenteTarea::obtenerPrioridad).reversed());
        return List.copyOf(copia);
    }
}
