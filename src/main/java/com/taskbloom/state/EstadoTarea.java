package com.taskbloom.state;

/** Patrón State: la validación se delega al estado actual. */
public interface EstadoTarea {
    boolean puedeTransicionar(EstadoTarea destino);
    String obtenerNombre();
}
