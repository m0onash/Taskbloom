package com.taskbloom.persistence;

import com.taskbloom.model.Usuario;
import com.taskbloom.service.GestorTareas;
import com.taskbloom.observer.GestorRecordatorios;
import com.taskbloom.observer.GestorNotificaciones;
import java.util.List;

/** Contrato de persistencia de TaskBloom, separado de los patrones del dominio. */
public interface RepositorioTareas {
    DatosGuardados cargarEstado(GestorTareas gestor);
    void guardarEstado(GestorTareas gestor, List<Usuario> equipo, Usuario usuarioActual, boolean modoOscuro);
    void cargarRecordatorios(GestorTareas gestor, GestorRecordatorios recordatorios,
                             GestorNotificaciones notificaciones, List<Usuario> equipo);
    void guardarEstado(GestorTareas gestor, List<Usuario> equipo, Usuario usuarioActual, boolean modoOscuro,
                       GestorRecordatorios recordatorios, GestorNotificaciones notificaciones);
}
