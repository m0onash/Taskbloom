package com.taskbloom.state;

public final class EstadoPendiente implements EstadoTarea {
    @Override public boolean puedeTransicionar(EstadoTarea destino) {
        return destino instanceof EstadoEnCurso;
    }
    @Override public String obtenerNombre() { return "Pendiente"; }
}
