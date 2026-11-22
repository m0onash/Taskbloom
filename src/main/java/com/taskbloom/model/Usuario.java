package com.taskbloom.model;

import java.util.Objects;

public final class Usuario {
    private final String id;
    private final String nombre;

    public Usuario(String id, String nombre) {
        this.id = Objects.requireNonNull(id);
        this.nombre = Objects.requireNonNull(nombre);
    }
    public String obtenerId() { return id; }
    public String obtenerNombre() { return nombre; }
}
