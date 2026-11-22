package com.taskbloom.strategy;

import com.taskbloom.model.ComponenteTarea;
import java.util.List;
import java.util.Objects;

/** Contexto Strategy: primero filtra, después ordena; no cambia la fuente. */
public final class OrganizadorTareas {
    private EstrategiaOrdenamiento estrategiaOrden;
    private EstrategiaFiltrado estrategiaFiltro;

    public void establecerOrdenamiento(EstrategiaOrdenamiento estrategiaOrden) {
        this.estrategiaOrden = estrategiaOrden; // null equivale a conservar el orden
    }

    public void establecerFiltro(EstrategiaFiltrado estrategiaFiltro) {
        this.estrategiaFiltro = estrategiaFiltro; // null equivale a mostrar todos
    }

    public List<ComponenteTarea> organizar(List<ComponenteTarea> tareas) {
        List<ComponenteTarea> resultado = List.copyOf(Objects.requireNonNull(tareas));
        if (estrategiaFiltro != null) resultado = estrategiaFiltro.filtrar(resultado);
        if (estrategiaOrden != null) resultado = estrategiaOrden.ordenar(resultado);
        return List.copyOf(resultado);
    }
}
