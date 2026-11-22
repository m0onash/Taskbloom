package com.taskbloom;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.Usuario;
import com.taskbloom.observer.*;
import com.taskbloom.persistence.DatosGuardados;
import com.taskbloom.persistence.RepositorioTareasSQLite;
import com.taskbloom.service.GestorTareas;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/** Ejecutar con -ea y sqlite-jdbc en classpath: prueba migración y persistencia de Observer. */
public final class PruebaObserverSQLite {
    public static void main(String[] args) throws Exception {
        Path archivo = Files.createTempFile("taskbloom-observer-", ".db");
        try {
            // Simular el esquema antiguo de la etapa 3, con una tarea y un perfil real.
            try (var c = DriverManager.getConnection("jdbc:sqlite:" + archivo); Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE usuarios(id TEXT PRIMARY KEY, nombre TEXT NOT NULL)");
                s.executeUpdate("CREATE TABLE tareas(id TEXT PRIMARY KEY, parent_id TEXT REFERENCES tareas(id) ON DELETE CASCADE, tipo TEXT NOT NULL, titulo TEXT NOT NULL, descripcion TEXT NOT NULL, prioridad INTEGER NOT NULL, fecha_limite TEXT NOT NULL, estado TEXT NOT NULL)");
                s.executeUpdate("CREATE TABLE responsables(tarea_id TEXT NOT NULL REFERENCES tareas(id) ON DELETE CASCADE, usuario_id TEXT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, PRIMARY KEY(tarea_id, usuario_id))");
                s.executeUpdate("CREATE TABLE configuracion(clave TEXT PRIMARY KEY, valor TEXT NOT NULL)");
                s.executeUpdate("INSERT INTO usuarios VALUES('u1','Mi perfil')");
                s.executeUpdate("INSERT INTO tareas VALUES('t1',NULL,'simple','Tarea conservada','',3,'2030-05-15T18:00:00','Pendiente')");
                s.executeUpdate("INSERT INTO responsables VALUES('t1','u1')");
                s.executeUpdate("INSERT INTO configuracion VALUES('usuario_actual','u1')");
            }
            RepositorioTareasSQLite repo = new RepositorioTareasSQLite(archivo);
            GestorTareas gestor = new GestorTareas();
            DatosGuardados datos = repo.cargarEstado(gestor);
            assert datos.equipo().size() == 1;
            assert gestor.obtenerTareasPrincipales().getFirst().obtenerTitulo().equals("Tarea conservada");
            ComponenteTarea tarea = gestor.obtenerTarea("t1");
            Usuario usuario = datos.equipo().getFirst();
            GestorRecordatorios recordatorios = new GestorRecordatorios();
            GestorNotificaciones notificaciones = new GestorNotificaciones();
            recordatorios.suscribir(new ObservadorUsuario(usuario, notificaciones));
            repo.cargarRecordatorios(gestor, recordatorios, notificaciones, datos.equipo());
            assert recordatorios.obtenerRecordatorios().isEmpty();
            recordatorios.programarRecordatorio(tarea, 24);
            assert recordatorios.verificarVencimientos(LocalDateTime.of(2030, 5, 14, 18, 0)) == 1;
            assert notificaciones.contarNoLeidas(usuario) == 1;
            repo.guardarEstado(gestor, datos.equipo(), usuario, true, recordatorios, notificaciones);

            // Simular reinicio: aviso enviado y bandeja se restauran sin repetir envíos.
            GestorTareas reinicio = new GestorTareas();
            DatosGuardados restaurados = repo.cargarEstado(reinicio);
            GestorRecordatorios recordatorios2 = new GestorRecordatorios();
            GestorNotificaciones notificaciones2 = new GestorNotificaciones();
            Usuario usuario2 = restaurados.equipo().getFirst();
            recordatorios2.suscribir(new ObservadorUsuario(usuario2, notificaciones2));
            repo.cargarRecordatorios(reinicio, recordatorios2, notificaciones2, restaurados.equipo());
            assert recordatorios2.obtenerRecordatorios().size() == 1;
            assert recordatorios2.obtenerRecordatorios().getFirst().estaEnviado();
            assert notificaciones2.contarNoLeidas(usuario2) == 1;
            assert recordatorios2.verificarVencimientos(LocalDateTime.of(2030, 5, 20, 18, 0)) == 0;
            notificaciones2.marcarTodasComoLeidas(usuario2);
            repo.guardarEstado(reinicio, restaurados.equipo(), usuario2, true, recordatorios2, notificaciones2);

            GestorTareas reinicio2 = new GestorTareas();
            DatosGuardados datos2 = repo.cargarEstado(reinicio2);
            GestorRecordatorios recordatorios3 = new GestorRecordatorios();
            GestorNotificaciones notificaciones3 = new GestorNotificaciones();
            repo.cargarRecordatorios(reinicio2, recordatorios3, notificaciones3, datos2.equipo());
            assert notificaciones3.contarNoLeidas(datos2.equipo().getFirst()) == 0;
            assert reinicio2.obtenerTareasPrincipales().size() == 1;

            // Borrar la tarea borra también sus avisos y mensajes guardados.
            reinicio2.eliminarTarea("t1");
            recordatorios3.quitarRecordatorio("t1");
            notificaciones3.eliminarPorRecordatorios(java.util.Set.of(recordatorios2.obtenerRecordatorios().getFirst().obtenerId()));
            repo.guardarEstado(reinicio2, datos2.equipo(), datos2.equipo().getFirst(), true,
                    recordatorios3, notificaciones3);
            GestorTareas finalGestor = new GestorTareas();
            DatosGuardados finalDatos = repo.cargarEstado(finalGestor);
            GestorRecordatorios finalRecordatorios = new GestorRecordatorios();
            GestorNotificaciones finalNotificaciones = new GestorNotificaciones();
            repo.cargarRecordatorios(finalGestor, finalRecordatorios, finalNotificaciones, finalDatos.equipo());
            assert finalGestor.obtenerTareasPrincipales().isEmpty();
            assert finalRecordatorios.obtenerRecordatorios().isEmpty();
            assert finalNotificaciones.obtenerTodas().isEmpty();
            System.out.println("OK: migración de etapa 3, avisos, bandeja y borrado persisten en SQLite.");
        } finally {
            Files.deleteIfExists(archivo);
        }
    }
}
