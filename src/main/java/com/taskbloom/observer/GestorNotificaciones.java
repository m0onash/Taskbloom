package com.taskbloom.observer;

import com.taskbloom.model.Usuario;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Almacena notificaciones, sin duplicar el mismo recordatorio para la misma persona. */
public final class GestorNotificaciones {
    private final List<Notificacion> notificaciones = new ArrayList<>();

    public void crearNotificacion(Usuario usuario, Recordatorio recordatorio) {
        Objects.requireNonNull(usuario);
        Objects.requireNonNull(recordatorio);
        boolean yaExiste = notificaciones.stream().anyMatch(n ->
                n.obtenerIdRecordatorio().equals(recordatorio.obtenerId())
                        && n.obtenerDestinatario().obtenerId().equals(usuario.obtenerId()));
        if (!yaExiste) notificaciones.add(new Notificacion(usuario, recordatorio));
    }

    public void registrarNotificacionRestaurada(Notificacion notificacion) {
        Objects.requireNonNull(notificacion);
        if (notificaciones.stream().anyMatch(n -> n.obtenerId().equals(notificacion.obtenerId())))
            throw new IllegalArgumentException("Notificación duplicada");
        notificaciones.add(notificacion);
    }

    public List<Notificacion> consultarNotificaciones(Usuario usuario) {
        if (usuario == null) return List.of();
        return notificaciones.stream()
                .filter(n -> n.obtenerDestinatario().obtenerId().equals(usuario.obtenerId()))
                .sorted(Comparator.comparing(Notificacion::obtenerFechaCreacion).reversed())
                .toList();
    }

    public long contarNoLeidas(Usuario usuario) {
        return consultarNotificaciones(usuario).stream().filter(n -> !n.estaLeida()).count();
    }

    public void marcarTodasComoLeidas(Usuario usuario) {
        consultarNotificaciones(usuario).forEach(Notificacion::marcarComoLeida);
    }

    public List<Notificacion> obtenerTodas() { return List.copyOf(notificaciones); }

    public void eliminarPorRecordatorios(Set<String> idsRecordatorios) {
        notificaciones.removeIf(n -> idsRecordatorios.contains(n.obtenerIdRecordatorio()));
    }

    public void eliminarPorUsuario(String idUsuario) {
        notificaciones.removeIf(n -> n.obtenerDestinatario().obtenerId().equals(idUsuario));
    }
}
