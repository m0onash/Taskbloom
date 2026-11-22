package com.taskbloom.state;

public final class EstadoEnRevision implements EstadoTarea {
    @Override public boolean puedeTransicionar(EstadoTarea destino) {
        return destino instanceof EstadoEnCurso || destino instanceof EstadoTerminada;
    }
    @Override public String obtenerNombre() { return "En revisión"; }
}
