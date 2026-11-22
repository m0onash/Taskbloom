package com.taskbloom.strategy;

import com.taskbloom.model.ComponenteTarea;
import java.util.List;

/** Strategy: contrato para filtrar tareas. */
public interface EstrategiaFiltrado {
    List<ComponenteTarea> filtrar(List<ComponenteTarea> tareas);
}
