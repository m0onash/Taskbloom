package com.taskbloom;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.strategy.FiltrarPorResponsable;
import com.taskbloom.strategy.OrdenarPorFecha;
import com.taskbloom.strategy.OrdenarPorPrioridad;
import com.taskbloom.strategy.OrganizadorTareas;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ejecutar con java -ea para verificar los criterios Strategy y la inmutabilidad de la lista fuente. */
public final class PruebaStrategy {
    public static void main(String[] args) {
        Usuario ana = new Usuario("u1", "Ana");
        Usuario carlos = new Usuario("u2", "Carlos");
        TareaSimple media = new TareaSimple("m", "Media", "", 2, LocalDateTime.of(2027, 1, 8, 10, 0));
        media.asignarResponsables(List.of(ana, carlos));
        TareaSimple alta = new TareaSimple("a", "Alta", "", 3, LocalDateTime.of(2027, 1, 20, 10, 0));
        alta.asignarResponsables(List.of(ana));
        TareaSimple baja = new TareaSimple("b", "Baja", "", 1, LocalDateTime.of(2027, 1, 2, 10, 0));
        baja.asignarResponsables(List.of(carlos));
        List<ComponenteTarea> fuente = new ArrayList<>(List.of(media, alta, baja));
        OrganizadorTareas organizador = new OrganizadorTareas();

        organizador.establecerOrdenamiento(new OrdenarPorPrioridad());
        assert organizador.organizar(fuente).stream().map(ComponenteTarea::obtenerId).toList()
                .equals(List.of("a", "m", "b"));
        organizador.establecerOrdenamiento(new OrdenarPorFecha());
        assert organizador.organizar(fuente).stream().map(ComponenteTarea::obtenerId).toList()
                .equals(List.of("b", "m", "a"));
        organizador.establecerFiltro(new FiltrarPorResponsable("u1"));
        assert organizador.organizar(fuente).stream().map(ComponenteTarea::obtenerId).toList()
                .equals(List.of("m", "a"));
        organizador.establecerFiltro(new FiltrarPorResponsable("u2"));
        assert organizador.organizar(fuente).stream().map(ComponenteTarea::obtenerId).toList()
                .equals(List.of("b", "m"));
        organizador.establecerFiltro(null);
        assert organizador.organizar(fuente).size() == 3;
        assert fuente.stream().map(ComponenteTarea::obtenerId).toList().equals(List.of("m", "a", "b"));
        System.out.println("OK: Strategy filtra y ordena sin modificar la lista fuente.");
    }
}
