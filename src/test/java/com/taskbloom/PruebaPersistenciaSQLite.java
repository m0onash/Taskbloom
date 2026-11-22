package com.taskbloom;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.TareaCompuesta;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.persistence.DatosGuardados;
import com.taskbloom.persistence.RepositorioTareasSQLite;
import com.taskbloom.service.GestorTareas;
import com.taskbloom.state.EstadoEnCurso;
import com.taskbloom.state.EstadoEnRevision;
import com.taskbloom.state.EstadoTerminada;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/** Ejecutar con -ea y sqlite-jdbc en classpath para probar persistencia local. */
public final class PruebaPersistenciaSQLite {
    public static void main(String[] args) throws Exception {
        Path archivo = Files.createTempFile("taskbloom-test-", ".db");
        try {
            RepositorioTareasSQLite repo = new RepositorioTareasSQLite(archivo);
            GestorTareas gestor = new GestorTareas();
            DatosGuardados nuevos = repo.cargarEstado(gestor);
            assert nuevos.equipo().isEmpty();
            assert gestor.obtenerTareasPrincipales().isEmpty();

            Usuario yo = new Usuario("u1", "Mi nombre");
            Usuario colega = new Usuario("u2", "Compañero");
            TareaCompuesta padre = gestor.crearTareaCompuesta("Proyecto", "Detalle", 3, LocalDateTime.now().plusDays(3));
            padre.asignarResponsables(List.of(yo, colega));
            padre.cambiarEstado(new EstadoEnCurso());
            TareaSimple hijo = gestor.crearTareaSimple("Subtarea", "", 2, LocalDateTime.now().plusDays(1));
            hijo.cambiarEstado(new EstadoEnCurso());
            hijo.cambiarEstado(new EstadoEnRevision());
            hijo.cambiarEstado(new EstadoTerminada());
            gestor.agregarSubtarea(padre.obtenerId(), hijo.obtenerId());
            repo.guardarEstado(gestor, List.of(yo, colega), yo, true);

            // Simula cerrar el programa y volver a abrirlo.
            GestorTareas reabierto = new GestorTareas();
            DatosGuardados almacenados = repo.cargarEstado(reabierto);
            assert almacenados.equipo().size() == 2;
            assert almacenados.idUsuarioActual().equals("u1");
            assert almacenados.modoOscuro();
            assert reabierto.obtenerTareasPrincipales().size() == 1;
            ComponenteTarea tarea = reabierto.obtenerTareasPrincipales().getFirst();
            assert tarea.obtenerTitulo().equals("Proyecto");
            assert tarea.obtenerEstado().obtenerNombre().equals("En curso");
            assert tarea.obtenerResponsables().size() == 2;
            assert tarea.obtenerSubtareas().size() == 1;
            assert tarea.obtenerSubtareas().getFirst().obtenerEstado() instanceof EstadoTerminada;

            almacenados.equipo().getFirst().actualizarNombre("Nuevo nombre");
            reabierto.eliminarTarea(tarea.obtenerId());
            repo.guardarEstado(reabierto, almacenados.equipo(), almacenados.equipo().getFirst(), false);
            GestorTareas reabiertoOtraVez = new GestorTareas();
            DatosGuardados finalDatos = repo.cargarEstado(reabiertoOtraVez);
            assert reabiertoOtraVez.obtenerTareasPrincipales().isEmpty();
            assert finalDatos.equipo().getFirst().obtenerNombre().equals("Nuevo nombre");
            assert !finalDatos.modoOscuro();
            System.out.println("OK: la creación, edición y eliminación persisten en SQLite.");
        } finally {
            Files.deleteIfExists(archivo);
        }
    }
}
