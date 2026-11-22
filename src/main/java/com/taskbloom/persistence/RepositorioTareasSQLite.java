package com.taskbloom.persistence;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.TareaCompuesta;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.observer.GestorRecordatorios;
import com.taskbloom.observer.GestorNotificaciones;
import com.taskbloom.observer.Recordatorio;
import com.taskbloom.observer.Notificacion;
import com.taskbloom.service.GestorTareas;
import com.taskbloom.state.EstadoEnCurso;
import com.taskbloom.state.EstadoEnRevision;
import com.taskbloom.state.EstadoTerminada;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Almacenamiento local SQLite; no crea registros de demostración. */
public final class RepositorioTareasSQLite implements RepositorioTareas {
    private final String url;

    /** Base de datos del usuario del equipo, independiente de la carpeta del proyecto. */
    public RepositorioTareasSQLite() {
        this(Path.of(System.getProperty("user.home"), ".taskbloom", "taskbloom.db"));
    }

    /** Constructor con ruta configurable para pruebas y copias de seguridad. */
    public RepositorioTareasSQLite(Path archivo) {
        Objects.requireNonNull(archivo);
        try {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) Files.createDirectories(padre);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo crear la carpeta de datos", ex);
        }
        url = "jdbc:sqlite:" + archivo.toAbsolutePath();
        inicializar();
    }

    private Connection conectar() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return conexion;
    }

    private void inicializar() {
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS usuarios (
                        id TEXT PRIMARY KEY, nombre TEXT NOT NULL
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tareas (
                        id TEXT PRIMARY KEY,
                        parent_id TEXT REFERENCES tareas(id) ON DELETE CASCADE,
                        tipo TEXT NOT NULL CHECK (tipo IN ('simple','compuesta')),
                        titulo TEXT NOT NULL, descripcion TEXT NOT NULL,
                        prioridad INTEGER NOT NULL,
                        fecha_limite TEXT NOT NULL, estado TEXT NOT NULL
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS responsables (
                        tarea_id TEXT NOT NULL REFERENCES tareas(id) ON DELETE CASCADE,
                        usuario_id TEXT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                        PRIMARY KEY (tarea_id, usuario_id)
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS configuracion (
                        clave TEXT PRIMARY KEY, valor TEXT NOT NULL
                    )""");
            // Migración no destructiva de la BD creada en la etapa 3.
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS recordatorios (
                        id TEXT PRIMARY KEY,
                        tarea_id TEXT NOT NULL UNIQUE REFERENCES tareas(id) ON DELETE CASCADE,
                        fecha_aviso TEXT NOT NULL, anticipacion_horas INTEGER NOT NULL,
                        enviado INTEGER NOT NULL CHECK (enviado IN (0,1))
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS notificaciones (
                        id TEXT PRIMARY KEY,
                        recordatorio_id TEXT NOT NULL REFERENCES recordatorios(id) ON DELETE CASCADE,
                        usuario_id TEXT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                        mensaje TEXT NOT NULL, fecha_creacion TEXT NOT NULL,
                        leida INTEGER NOT NULL CHECK (leida IN (0,1)),
                        UNIQUE(recordatorio_id, usuario_id)
                    )""");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo inicializar SQLite", ex);
        }
    }

    @Override
    public DatosGuardados cargarEstado(GestorTareas gestor) {
        Objects.requireNonNull(gestor);
        List<Usuario> equipo = new ArrayList<>();
        Map<String, Usuario> usuariosPorId = new HashMap<>();
        List<RegistroTarea> registros = new ArrayList<>();
        Map<String, List<Usuario>> responsables = new HashMap<>();
        Map<String, String> preferencias = new HashMap<>();
        try (Connection c = conectar()) {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, nombre FROM usuarios ORDER BY rowid")) {
                while (rs.next()) {
                    Usuario usuario = new Usuario(rs.getString("id"), rs.getString("nombre"));
                    equipo.add(usuario);
                    usuariosPorId.put(usuario.obtenerId(), usuario);
                }
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id,parent_id,tipo,titulo,descripcion,prioridad,fecha_limite,estado FROM tareas ORDER BY rowid")) {
                while (rs.next()) {
                    registros.add(new RegistroTarea(rs.getString("id"), rs.getString("parent_id"),
                            rs.getString("tipo"), rs.getString("titulo"), rs.getString("descripcion"),
                            rs.getInt("prioridad"), LocalDateTime.parse(rs.getString("fecha_limite")),
                            rs.getString("estado")));
                }
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT tarea_id,usuario_id FROM responsables")) {
                while (rs.next()) {
                    Usuario usuario = usuariosPorId.get(rs.getString("usuario_id"));
                    if (usuario != null) responsables.computeIfAbsent(rs.getString("tarea_id"), k -> new ArrayList<>()).add(usuario);
                }
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT clave,valor FROM configuracion")) {
                while (rs.next()) preferencias.put(rs.getString("clave"), rs.getString("valor"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron leer los datos guardados", ex);
        }
        // Primero registrar todas las tareas; después reconstruir asociaciones y el árbol Composite.
        for (RegistroTarea r : registros) {
            ComponenteTarea tarea = switch (r.tipo()) {
                case "simple" -> new TareaSimple(r.id(), r.titulo(), r.descripcion(), r.prioridad(), r.fechaLimite());
                case "compuesta" -> new TareaCompuesta(r.id(), r.titulo(), r.descripcion(), r.prioridad(), r.fechaLimite());
                default -> throw new IllegalStateException("Tipo de tarea inválido: " + r.tipo());
            };
            restaurarEstado(tarea, r.estado());
            tarea.asignarResponsables(responsables.getOrDefault(r.id(), List.of()));
            gestor.registrarTareaRestaurada(tarea);
        }
        for (RegistroTarea r : registros) {
            if (r.padreId() != null) gestor.agregarSubtarea(r.padreId(), r.id());
        }
        return new DatosGuardados(List.copyOf(equipo), preferencias.get("usuario_actual"),
                Boolean.parseBoolean(preferencias.getOrDefault("modo_oscuro", "false")));
    }

    private static void restaurarEstado(ComponenteTarea tarea, String estado) {
        if ("Pendiente".equals(estado)) return;
        if (!tarea.cambiarEstado(new EstadoEnCurso())) throw new IllegalStateException("Estado inválido");
        if ("En curso".equals(estado)) return;
        if (!tarea.cambiarEstado(new EstadoEnRevision())) throw new IllegalStateException("Estado inválido");
        if ("En revisión".equals(estado)) return;
        if ("Terminada".equals(estado)) {
            if (!tarea.cambiarEstado(new EstadoTerminada())) throw new IllegalStateException("Estado inválido");
            return;
        }
        throw new IllegalStateException("Estado desconocido en SQLite: " + estado);
    }

    @Override
    public void cargarRecordatorios(GestorTareas gestor, GestorRecordatorios recordatorios,
                                    GestorNotificaciones notificaciones, List<Usuario> equipo) {
        Objects.requireNonNull(gestor);
        Objects.requireNonNull(recordatorios);
        Objects.requireNonNull(notificaciones);
        Map<String, Usuario> porId = new HashMap<>();
        for (Usuario usuario : equipo) porId.put(usuario.obtenerId(), usuario);
        try (Connection c = conectar()) {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT id,tarea_id,anticipacion_horas,enviado FROM recordatorios ORDER BY rowid")) {
                while (rs.next()) {
                    ComponenteTarea tarea = gestor.obtenerTarea(rs.getString("tarea_id"));
                    recordatorios.registrarRecordatorioRestaurado(new Recordatorio(
                            rs.getString("id"), tarea, rs.getInt("anticipacion_horas"), rs.getInt("enviado") != 0));
                }
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT id,recordatorio_id,usuario_id,mensaje,fecha_creacion,leida FROM notificaciones ORDER BY rowid")) {
                while (rs.next()) {
                    Usuario usuario = porId.get(rs.getString("usuario_id"));
                    if (usuario == null) throw new IllegalStateException("Destinatario de notificación desconocido");
                    notificaciones.registrarNotificacionRestaurada(new Notificacion(
                            rs.getString("id"), usuario, rs.getString("recordatorio_id"),
                            rs.getString("mensaje"), LocalDateTime.parse(rs.getString("fecha_creacion")),
                            rs.getInt("leida") != 0));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron cargar los recordatorios", ex);
        }
    }

    @Override
    public void guardarEstado(GestorTareas gestor, List<Usuario> equipo, Usuario usuarioActual, boolean modoOscuro) {
        // Compatibilidad con las llamadas de la etapa 3; la aplicación usa la nueva sobrecarga.
        guardarEstado(gestor, equipo, usuarioActual, modoOscuro,
                new GestorRecordatorios(), new GestorNotificaciones());
    }

    @Override
    public void guardarEstado(GestorTareas gestor, List<Usuario> equipo, Usuario usuarioActual, boolean modoOscuro,
                              GestorRecordatorios recordatorios, GestorNotificaciones notificaciones) {
        Objects.requireNonNull(gestor);
        Objects.requireNonNull(equipo);
        Objects.requireNonNull(recordatorios);
        Objects.requireNonNull(notificaciones);
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try {
                // Instantánea transaccional: eliminaciones persisten y una falla revierte todo.
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("DELETE FROM notificaciones");
                    st.executeUpdate("DELETE FROM recordatorios");
                    st.executeUpdate("DELETE FROM responsables");
                    st.executeUpdate("DELETE FROM tareas");
                    st.executeUpdate("DELETE FROM usuarios");
                    st.executeUpdate("DELETE FROM configuracion");
                }
                try (PreparedStatement st = c.prepareStatement("INSERT INTO usuarios(id,nombre) VALUES(?,?)")) {
                    for (Usuario usuario : equipo) {
                        st.setString(1, usuario.obtenerId());
                        st.setString(2, usuario.obtenerNombre());
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                try (PreparedStatement tareas = c.prepareStatement("""
                            INSERT INTO tareas(id,parent_id,tipo,titulo,descripcion,prioridad,fecha_limite,estado)
                            VALUES(?,?,?,?,?,?,?,?)""");
                     PreparedStatement asignaciones = c.prepareStatement("""
                            INSERT INTO responsables(tarea_id,usuario_id) VALUES(?,?)""")) {
                    for (ComponenteTarea tarea : gestor.obtenerTareasPrincipales())
                        insertarTarea(tarea, null, tareas, asignaciones);
                    tareas.executeBatch();
                    asignaciones.executeBatch();
                }
                try (PreparedStatement st = c.prepareStatement("""
                        INSERT INTO recordatorios(id,tarea_id,fecha_aviso,anticipacion_horas,enviado)
                        VALUES(?,?,?,?,?)""")) {
                    for (Recordatorio r : recordatorios.obtenerRecordatorios()) {
                        st.setString(1, r.obtenerId());
                        st.setString(2, r.obtenerTarea().obtenerId());
                        st.setString(3, r.obtenerFechaAviso().toString());
                        st.setInt(4, r.obtenerAnticipacionHoras());
                        st.setInt(5, r.estaEnviado() ? 1 : 0);
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                try (PreparedStatement st = c.prepareStatement("""
                        INSERT INTO notificaciones(id,recordatorio_id,usuario_id,mensaje,fecha_creacion,leida)
                        VALUES(?,?,?,?,?,?)""")) {
                    for (Notificacion n : notificaciones.obtenerTodas()) {
                        st.setString(1, n.obtenerId());
                        st.setString(2, n.obtenerIdRecordatorio());
                        st.setString(3, n.obtenerDestinatario().obtenerId());
                        st.setString(4, n.obtenerMensaje());
                        st.setString(5, n.obtenerFechaCreacion().toString());
                        st.setInt(6, n.estaLeida() ? 1 : 0);
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                try (PreparedStatement st = c.prepareStatement("INSERT INTO configuracion(clave,valor) VALUES(?,?)")) {
                    st.setString(1, "usuario_actual");
                    st.setString(2, usuarioActual == null ? "" : usuarioActual.obtenerId());
                    st.addBatch();
                    st.setString(1, "modo_oscuro");
                    st.setString(2, Boolean.toString(modoOscuro));
                    st.addBatch();
                    st.executeBatch();
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron guardar los cambios en SQLite", ex);
        }
    }

    private static void insertarTarea(ComponenteTarea tarea, String padreId,
                                     PreparedStatement tareas, PreparedStatement responsables) throws SQLException {
        tareas.setString(1, tarea.obtenerId());
        tareas.setString(2, padreId);
        tareas.setString(3, tarea instanceof TareaCompuesta ? "compuesta" : "simple");
        tareas.setString(4, tarea.obtenerTitulo());
        tareas.setString(5, tarea.obtenerDescripcion());
        tareas.setInt(6, tarea.obtenerPrioridad());
        tareas.setString(7, tarea.obtenerFechaLimite().toString());
        tareas.setString(8, tarea.obtenerEstado().obtenerNombre());
        tareas.addBatch();
        for (Usuario usuario : tarea.obtenerResponsables()) {
            responsables.setString(1, tarea.obtenerId());
            responsables.setString(2, usuario.obtenerId());
            responsables.addBatch();
        }
        for (ComponenteTarea hija : tarea.obtenerSubtareas()) insertarTarea(hija, tarea.obtenerId(), tareas, responsables);
    }

    private record RegistroTarea(String id, String padreId, String tipo, String titulo,
                                 String descripcion, int prioridad, LocalDateTime fechaLimite, String estado) { }
}
