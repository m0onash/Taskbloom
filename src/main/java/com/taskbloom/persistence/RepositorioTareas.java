package com.taskbloom.persistence;

import com.taskbloom.model.Usuario;
import com.taskbloom.service.GestorTareas;
import java.util.List;

/** Contrato de persistencia de TaskBloom, separado de los patrones del dominio. */
public interface RepositorioTareas {
    DatosGuardados cargarEstado(GestorTareas gestor);
    void guardarEstado(GestorTareas gestor, List<Usuario> equipo, Usuario usuarioActual, boolean modoOscuro);
}
