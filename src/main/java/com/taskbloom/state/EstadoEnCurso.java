package com.taskbloom.state;

public final class EstadoEnCurso implements EstadoTarea {
    @Override public boolean puedeTransicionar(EstadoTarea destino) {
        return destino instanceof EstadoEnRevision;
    }
    @Override public String obtenerNombre() { return "En curso"; }
}
