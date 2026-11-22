package com.taskbloom.state;

public final class EstadoTerminada implements EstadoTarea {
    @Override public boolean puedeTransicionar(EstadoTarea destino) { return false; }
    @Override public String obtenerNombre() { return "Terminada"; }
}
