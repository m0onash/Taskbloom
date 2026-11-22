package com.taskbloom.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Proyecto {
    private final String id;
    private String nombre;
    private final List<ComponenteTarea> tareas = new ArrayList<>();

    public Proyecto(String id, String nombre) {
        this.id = Objects.requireNonNull(id);
        this.nombre = Objects.requireNonNull(nombre);
    }
    public String obtenerId() { return id; }
    public String obtenerNombre() { return nombre; }
    public List<ComponenteTarea> obtenerTareas() { return List.copyOf(tareas); }
    public void agregarTarea(ComponenteTarea tarea) { tareas.add(Objects.requireNonNull(tarea)); }
}
