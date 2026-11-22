package com.taskbloom.observer;

import com.taskbloom.model.Usuario;
import java.util.Objects;

/** Concrete Observer: convierte el aviso en una notificación para un responsable. */
public final class ObservadorUsuario implements ObservadorRecordatorio {
    private final Usuario usuario;
    private final GestorNotificaciones gestor;

    public ObservadorUsuario(Usuario usuario, GestorNotificaciones gestor) {
        this.usuario = Objects.requireNonNull(usuario);
        this.gestor = Objects.requireNonNull(gestor);
    }

    public Usuario obtenerUsuario() { return usuario; }

    @Override public void actualizar(Recordatorio recordatorio) {
        gestor.crearNotificacion(usuario, recordatorio);
    }
}
