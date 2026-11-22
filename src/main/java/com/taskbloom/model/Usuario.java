package com.taskbloom.model;

import java.util.Objects;

/** Usuario del equipo: su ID permanece estable aunque cambie el nombre. */
public final class Usuario {
    private final String id;
    private String nombre;

    public Usuario(String id, String nombre) {
        this.id = Objects.requireNonNull(id);
        actualizarNombre(nombre);
    }

    public String obtenerId() { return id; }
    public String obtenerNombre() { return nombre; }

    public void actualizarNombre(String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        this.nombre = nuevoNombre.trim();
    }

    @Override public String toString() { return nombre; }
}
