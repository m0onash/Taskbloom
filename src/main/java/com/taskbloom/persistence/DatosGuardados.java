package com.taskbloom.persistence;

import com.taskbloom.model.Usuario;
import java.util.List;

/** Datos de configuración y equipo cargados desde la BD. Las tareas van al GestorTareas. */
public record DatosGuardados(List<Usuario> equipo, String idUsuarioActual, boolean modoOscuro) { }
