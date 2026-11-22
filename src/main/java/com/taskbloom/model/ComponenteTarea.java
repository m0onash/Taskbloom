package com.taskbloom.model;

import com.taskbloom.state.EstadoPendiente;
import com.taskbloom.state.EstadoTarea;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Component del patrón Composite y contexto del patrón State. */
public abstract class ComponenteTarea {
    protected final String id;
    protected String titulo;
    protected String descripcion;
    protected int prioridad; // 3 = Alta, 2 = Media, 1 = Baja
    protected LocalDateTime fechaLimite;
    protected EstadoTarea estado;
    protected final List<Usuario> responsables = new ArrayList<>();

    protected ComponenteTarea(String id, String titulo, String descripcion,
                              int prioridad, LocalDateTime fechaLimite) {
        this.id = Objects.requireNonNull(id);
        this.titulo = Objects.requireNonNull(titulo);
        this.descripcion = Objects.requireNonNull(descripcion);
        if (prioridad < 1 || prioridad > 3) throw new IllegalArgumentException("Prioridad inválida");
        this.prioridad = prioridad;
        this.fechaLimite = Objects.requireNonNull(fechaLimite);
        this.estado = new EstadoPendiente();
    }

    public String obtenerId() { return id; }
    public String obtenerTitulo() { return titulo; }
    public String obtenerDescripcion() { return descripcion; }
    public int obtenerPrioridad() { return prioridad; }
    public LocalDateTime obtenerFechaLimite() { return fechaLimite; }
    public EstadoTarea obtenerEstado() { return estado; }
    public List<Usuario> obtenerResponsables() { return List.copyOf(responsables); }

    public void actualizarDatos(String titulo, String descripcion, int prioridad, LocalDateTime fechaLimite) {
        if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("El título es obligatorio");
        if (prioridad < 1 || prioridad > 3) throw new IllegalArgumentException("Prioridad inválida");
        this.titulo = titulo;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.prioridad = prioridad;
        this.fechaLimite = Objects.requireNonNull(fechaLimite);
    }

    public void asignarResponsables(List<Usuario> personas) {
        responsables.clear();
        responsables.addAll(Objects.requireNonNull(personas));
    }

    public boolean cambiarEstado(EstadoTarea nuevoEstado) {
        Objects.requireNonNull(nuevoEstado);
        if (!estado.puedeTransicionar(nuevoEstado)) return false;
        establecerEstado(nuevoEstado);
        return true;
    }

    /** Uso interno durante transiciones válidas. */
    public void establecerEstado(EstadoTarea nuevoEstado) {
        Objects.requireNonNull(nuevoEstado);
        if (!estado.puedeTransicionar(nuevoEstado)) {
            throw new IllegalStateException("Transición no permitida: "
                    + estado.obtenerNombre() + " → " + nuevoEstado.obtenerNombre());
        }
        this.estado = nuevoEstado;
    }

    public abstract List<ComponenteTarea> obtenerSubtareas();
}
