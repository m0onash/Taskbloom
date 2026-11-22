package com.taskbloom.service;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.TareaCompuesta;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.state.EstadoEnCurso;
import com.taskbloom.state.EstadoEnRevision;
import com.taskbloom.state.EstadoTerminada;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Administra las raíces y el árbol de tareas sin duplicar subtareas en la vista principal. */
public final class GestorTareas {
    private final Map<String, ComponenteTarea> registro = new LinkedHashMap<>();
    private final List<ComponenteTarea> tareasPrincipales = new ArrayList<>();

    public TareaSimple crearTareaSimple() {
        return crearTareaSimple("Nueva tarea", "", 2, LocalDateTime.now().plusDays(7));
    }
    public TareaCompuesta crearTareaCompuesta() {
        return crearTareaCompuesta("Nueva tarea compuesta", "", 2, LocalDateTime.now().plusDays(7));
    }

    public TareaSimple crearTareaSimple(String titulo, String descripcion, int prioridad, LocalDateTime fechaLimite) {
        TareaSimple tarea = new TareaSimple(UUID.randomUUID().toString(), titulo, descripcion, prioridad, fechaLimite);
        registrar(tarea);
        return tarea;
    }

    public TareaCompuesta crearTareaCompuesta(String titulo, String descripcion, int prioridad, LocalDateTime fechaLimite) {
        TareaCompuesta tarea = new TareaCompuesta(UUID.randomUUID().toString(), titulo, descripcion, prioridad, fechaLimite);
        registrar(tarea);
        return tarea;
    }

    /** Carga objetos que ya tienen identificadores persistentes, sin generar IDs nuevos. */
    public void registrarTareaRestaurada(ComponenteTarea tarea) {
        if (registro.containsKey(tarea.obtenerId()))
            throw new IllegalArgumentException("ID de tarea duplicado: " + tarea.obtenerId());
        registrar(tarea);
    }

    private void registrar(ComponenteTarea tarea) {
        registro.put(tarea.obtenerId(), tarea);
        tareasPrincipales.add(tarea);
    }

    public void agregarSubtarea(String idPadre, String idSubtarea) {
        ComponenteTarea padre = obtenerTarea(idPadre);
        ComponenteTarea subtarea = obtenerTarea(idSubtarea);
        if (!(padre instanceof TareaCompuesta compuesta)) {
            throw new IllegalArgumentException("La tarea padre debe ser compuesta");
        }
        if (!tareasPrincipales.contains(subtarea)) {
            throw new IllegalArgumentException("La subtarea ya pertenece a otra tarea");
        }
        compuesta.agregarSubtarea(subtarea);
        tareasPrincipales.remove(subtarea);
    }

    /** Convierte una tarea simple principal en compuesta conservando ID, datos y estado.
     *  Así la UI permite añadir subtareas a tareas creadas sin ellas. */
    public TareaCompuesta convertirEnCompuesta(String id) {
        ComponenteTarea existente = obtenerTarea(id);
        if (existente instanceof TareaCompuesta compuesta) return compuesta;
        int posicion = tareasPrincipales.indexOf(existente);
        if (posicion < 0) throw new IllegalArgumentException(
                "Solo se puede convertir una tarea principal");
        TareaCompuesta nueva = new TareaCompuesta(existente.obtenerId(),
                existente.obtenerTitulo(), existente.obtenerDescripcion(),
                existente.obtenerPrioridad(), existente.obtenerFechaLimite());
        nueva.asignarResponsables(existente.obtenerResponsables());
        if (existente.obtenerEstado() instanceof EstadoEnCurso
                || existente.obtenerEstado() instanceof EstadoEnRevision
                || existente.obtenerEstado() instanceof EstadoTerminada) {
            nueva.cambiarEstado(new EstadoEnCurso());
        }
        if (existente.obtenerEstado() instanceof EstadoEnRevision
                || existente.obtenerEstado() instanceof EstadoTerminada) {
            nueva.cambiarEstado(new EstadoEnRevision());
        }
        if (existente.obtenerEstado() instanceof EstadoTerminada) {
            nueva.cambiarEstado(new EstadoTerminada());
        }
        registro.put(id, nueva);
        tareasPrincipales.set(posicion, nueva);
        return nueva;
    }

    public void modificarTarea(String id, String titulo, String descripcion,
                               int prioridad, LocalDateTime fechaLimite) {
        obtenerTarea(id).actualizarDatos(titulo, descripcion, prioridad, fechaLimite);
    }

    public void eliminarTarea(String id) {
        ComponenteTarea tarea = obtenerTarea(id);
        tareasPrincipales.remove(tarea);
        for (ComponenteTarea otra : registro.values()) {
            if (otra instanceof TareaCompuesta compuesta) compuesta.eliminarSubtarea(id);
        }
        borrarDelRegistro(tarea);
    }

    private void borrarDelRegistro(ComponenteTarea tarea) {
        for (ComponenteTarea subtarea : tarea.obtenerSubtareas()) borrarDelRegistro(subtarea);
        registro.remove(tarea.obtenerId());
    }

    public List<ComponenteTarea> consultarSubtareas(String id) {
        return obtenerTarea(id).obtenerSubtareas();
    }
    public ComponenteTarea obtenerTarea(String id) {
        ComponenteTarea tarea = registro.get(id);
        if (tarea == null) throw new NoSuchElementException("Tarea no encontrada: " + id);
        return tarea;
    }
    public List<ComponenteTarea> obtenerTareasPrincipales() { return List.copyOf(tareasPrincipales); }
}
