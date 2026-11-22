package com.taskbloom;

import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.observer.GestorNotificaciones;
import com.taskbloom.observer.GestorRecordatorios;
import com.taskbloom.persistence.DatosGuardados;
import com.taskbloom.persistence.RepositorioTareasSQLite;
import com.taskbloom.service.GestorTareas;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/** Ejecutar con -ea y el driver SQLite: exactitud de la hora después de reiniciar. */
public final class PruebaFechaHoraSQLite {
    public static void main(String[] args) throws Exception {
        Path db = Files.createTempFile("taskbloom-fecha-hora-", ".db");
        try {
            RepositorioTareasSQLite repo = new RepositorioTareasSQLite(db);
            GestorTareas gestor = new GestorTareas();
            Usuario usuario = new Usuario("perfil", "Mi nombre");
            LocalDateTime primeraHora = LocalDateTime.of(2032, 12, 12, 15, 30);
            TareaSimple tarea = gestor.crearTareaSimple("Trabajo", "", 2, primeraHora);
            tarea.asignarResponsables(List.of(usuario));
            GestorRecordatorios gestorAvisos = new GestorRecordatorios();
            GestorNotificaciones bandeja = new GestorNotificaciones();
            gestorAvisos.programarRecordatorio(tarea, 1);
            repo.guardarEstado(gestor, List.of(usuario), usuario, false, gestorAvisos, bandeja);

            GestorTareas reinicio = new GestorTareas();
            DatosGuardados datos = repo.cargarEstado(reinicio);
            GestorRecordatorios avisosReiniciados = new GestorRecordatorios();
            GestorNotificaciones bandejaReiniciada = new GestorNotificaciones();
            repo.cargarRecordatorios(reinicio, avisosReiniciados, bandejaReiniciada, datos.equipo());
            var restaurada = reinicio.obtenerTareasPrincipales().getFirst();
            assert restaurada.obtenerFechaLimite().equals(primeraHora);
            assert avisosReiniciados.obtenerRecordatorios().getFirst().obtenerFechaAviso()
                    .equals(LocalDateTime.of(2032, 12, 12, 14, 30));

            reinicio.modificarTarea(restaurada.obtenerId(), restaurada.obtenerTitulo(), "", 2,
                    LocalDateTime.of(2032, 12, 12, 16, 45));
            avisosReiniciados.programarRecordatorio(restaurada, 0);
            repo.guardarEstado(reinicio, datos.equipo(), datos.equipo().getFirst(), false,
                    avisosReiniciados, bandejaReiniciada);

            GestorTareas otraVez = new GestorTareas();
            DatosGuardados datos2 = repo.cargarEstado(otraVez);
            GestorRecordatorios avisos2 = new GestorRecordatorios();
            repo.cargarRecordatorios(otraVez, avisos2, new GestorNotificaciones(), datos2.equipo());
            LocalDateTime editada = LocalDateTime.of(2032, 12, 12, 16, 45);
            assert otraVez.obtenerTareasPrincipales().getFirst().obtenerFechaLimite().equals(editada);
            assert avisos2.obtenerRecordatorios().getFirst().obtenerFechaAviso().equals(editada);
            assert avisos2.obtenerRecordatorios().getFirst().obtenerAnticipacionHoras() == 0;
            System.out.println("OK: SQLite conserva fecha y hora exactas al editar y reiniciar.");
        } finally {
            Files.deleteIfExists(db);
        }
    }
}
