package com.taskbloom.strategy;

import com.taskbloom.model.ComponenteTarea;
import java.util.List;

/** Strategy: contrato para ordenar tareas sin alterar los datos originales. */
public interface EstrategiaOrdenamiento {
    List<ComponenteTarea> ordenar(List<ComponenteTarea> tareas);
}
