package com.taskbloom;

import com.taskbloom.model.ComponenteTarea;
import com.taskbloom.model.TareaCompuesta;
import com.taskbloom.model.TareaSimple;
import com.taskbloom.model.Usuario;
import com.taskbloom.service.GestorTareas;
import com.taskbloom.observer.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import com.taskbloom.persistence.DatosGuardados;
import com.taskbloom.persistence.RepositorioTareas;
import com.taskbloom.persistence.RepositorioTareasSQLite;
import javafx.application.Platform;
import com.taskbloom.strategy.FiltrarPorResponsable;
import com.taskbloom.strategy.OrdenarPorFecha;
import com.taskbloom.strategy.OrdenarPorPrioridad;
import com.taskbloom.strategy.OrganizadorTareas;
import com.taskbloom.state.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Comparator;
import java.util.HashSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Presentación JavaFX: delega Composite, State, Strategy y Observer al dominio. */
public final class TaskBloomApp extends Application {
    private final GestorTareas gestor = new GestorTareas();
    private final OrganizadorTareas organizador = new OrganizadorTareas();
    private final GestorRecordatorios gestorRecordatorios = new GestorRecordatorios();
    private final GestorNotificaciones gestorNotificaciones = new GestorNotificaciones();
    private final Map<String, ObservadorUsuario> observadoresEquipo = new HashMap<>();
    private final Button botonNotificaciones = new Button("🔔  Avisos");
    private Timeline comprobadorRecordatorios;
    private final List<Usuario> equipo = new ArrayList<>();
    private final RepositorioTareas repositorio = new RepositorioTareasSQLite();
    private Usuario usuarioActual;
    private final Button botonPerfil = new Button();
    private final Label tituloBienvenida = new Label();
    private final VBox lista = new VBox(11);
    private final VBox proximoAviso = new VBox(6);
    private final Label contadorTotal = new Label();
    private final Label contadorPendientes = new Label();
    private final Label contadorActivas = new Label();
    private final Label contadorTerminadas = new Label();
    private final Label progresoJardin = new Label();
    private final ProgressBar barraJardin = new ProgressBar(0);
    private final TextField busqueda = new TextField();
    private final ComboBox<String> filtro = new ComboBox<>();
    private final ComboBox<String> filtroResponsable = new ComboBox<>();
    private final ComboBox<String> ordenamiento = new ComboBox<>();
    private BorderPane raiz;
    private Scene escena;
    private boolean modoOscuro = false;

    @Override public void start(Stage stage) {
        // Sin datos precargados: restaurar únicamente lo que el usuario haya guardado.
        try {
            DatosGuardados datos = repositorio.cargarEstado(gestor);
            equipo.addAll(datos.equipo());
            usuarioActual = equipo.stream().filter(u -> u.obtenerId().equals(datos.idUsuarioActual()))
                    .findFirst().orElse(null);
            modoOscuro = datos.modoOscuro();
            sincronizarObservadores();
            repositorio.cargarRecordatorios(gestor, gestorRecordatorios, gestorNotificaciones, equipo);
        } catch (RuntimeException error) {
            mostrarError("Error al abrir los datos", error);
            throw error; // No abrir con datos vacíos si la BD existente no se puede leer.
        }
        organizador.establecerOrdenamiento(new OrdenarPorPrioridad());
        raiz = new BorderPane();
        raiz.getStyleClass().add("root-app");
        raiz.setLeft(crearBarraLateral());
        raiz.setCenter(crearContenido());
        escena = new Scene(raiz, 1180, 770);
        escena.getStylesheets().add(getClass().getResource("/styles/taskbloom.css").toExternalForm());
        if (modoOscuro) raiz.getStyleClass().add("dark");
        stage.setTitle("TaskBloom · Tu jardín de tareas");
        stage.setScene(escena);
        stage.setMinWidth(850);
        stage.setMinHeight(580);
        stage.show();
        actualizarFiltroResponsables();
        actualizarPerfil();
        actualizarVista();
        comprobarRecordatorios();
        comprobadorRecordatorios = new Timeline(new KeyFrame(Duration.seconds(60), e -> comprobarRecordatorios()));
        comprobadorRecordatorios.setCycleCount(Timeline.INDEFINITE);
        comprobadorRecordatorios.play();
        if (usuarioActual == null) Platform.runLater(this::abrirPerfil);
    }

    private VBox crearBarraLateral() {
        VBox lateral = new VBox(12);
        lateral.getStyleClass().add("sidebar");
        lateral.setPrefWidth(218);
        lateral.setMinWidth(218);
        Label marca = new Label("✿  TaskBloom");
        marca.getStyleClass().add("brand");
        Label grupo = new Label("ESPACIO DE TRABAJO");
        grupo.getStyleClass().add("eyebrow");
        Button inicio = menu("⌂    Inicio", true);
        Button tareas = menu("☷    Mis tareas", false);
        tareas.setOnAction(e -> busqueda.requestFocus());
        Button calendario = menu("▢    Calendario", false);
        calendario.setOnAction(e -> abrirCalendario());
        Button personas = menu("♙    Equipo", false);
        personas.setOnAction(e -> abrirEquipo());
        Button avisos = menu("♧    Recordatorios", false);
        avisos.setOnAction(e -> abrirRecordatorios());
        Button exportar = menu("↓    Exportar CSV", false);
        exportar.setOnAction(e -> exportarCSV());
        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        progresoJardin.getStyleClass().add("garden-progress");
        barraJardin.setMaxWidth(Double.MAX_VALUE);
        barraJardin.getStyleClass().add("garden-bar");
        VBox tarjeta = new VBox(9, new Label("🌸"), new Label("Tu jardín está creciendo"),
                progresoJardin, barraJardin);
        tarjeta.getStyleClass().add("garden-box");
        botonPerfil.getStyleClass().addAll("nav-item", "profile");
        botonPerfil.setMaxWidth(Double.MAX_VALUE);
        botonPerfil.setOnAction(e -> abrirPerfil());
        lateral.getChildren().addAll(marca, grupo, inicio, tareas, calendario,
                personas, avisos, exportar, espacio, tarjeta, botonPerfil);
        return lateral;
    }

    private Button menu(String texto, boolean activo) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("nav-item");
        if (activo) boton.getStyleClass().add("active");
        boton.setMaxWidth(Double.MAX_VALUE);
        return boton;
    }

    private Node crearContenido() {
        BorderPane centro = new BorderPane();
        HBox cabecera = new HBox(13);
        cabecera.getStyleClass().add("topbar");
        cabecera.setAlignment(Pos.CENTER_LEFT);
        busqueda.setPromptText("⌕   Buscar tareas...");
        busqueda.getStyleClass().add("search-field");
        busqueda.setPrefWidth(315);
        busqueda.textProperty().addListener((obs, viejo, nuevo) -> actualizarVista());
        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);
        Button tema = new Button(modoOscuro ? "☀  Tema" : "☾  Tema");
        tema.getStyleClass().add("secondary-button");
        tema.setOnAction(e -> {
            modoOscuro = !modoOscuro;
            if (modoOscuro) raiz.getStyleClass().add("dark");
            else raiz.getStyleClass().remove("dark");
            tema.setText(modoOscuro ? "☀  Tema" : "☾  Tema");
            persistir();
        });
        Button nueva = new Button("＋  Nueva tarea");
        nueva.getStyleClass().add("primary-button");
        nueva.setOnAction(e -> abrirFormulario(null));
        botonNotificaciones.getStyleClass().add("secondary-button");
        botonNotificaciones.setOnAction(e -> abrirNotificaciones());
        cabecera.getChildren().addAll(busqueda, separador, tema, botonNotificaciones, nueva);
        centro.setTop(cabecera);

        VBox cuerpo = new VBox(20);
        cuerpo.getStyleClass().add("content");
        Label fecha = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es", "ES"))).toUpperCase());
        fecha.getStyleClass().add("eyebrow");
        tituloBienvenida.getStyleClass().add("page-title");
        Label subtitulo = new Label("Cada tarea completada hace florecer tu jardín.");
        subtitulo.getStyleClass().add("muted");
        VBox bienvenida = new VBox(6, fecha, tituloBienvenida, subtitulo);
        HBox estadisticas = new HBox(12,
                tarjetaEstadistica("Total de tareas", contadorTotal, "✿"),
                tarjetaEstadistica("Pendientes", contadorPendientes, "▢"),
                tarjetaEstadistica("En progreso", contadorActivas, "◷"),
                tarjetaEstadistica("Completadas", contadorTerminadas, "✓"));
        for (Node nodo : estadisticas.getChildren()) HBox.setHgrow(nodo, Priority.ALWAYS);
        Label seccion = new Label("Mis tareas");
        seccion.getStyleClass().add("section-title");
        Label descripcion = new Label("Cuida cada tarea y observa cómo florece.");
        descripcion.getStyleClass().add("muted");
        VBox encabezado = new VBox(5, seccion, descripcion);
        FlowPane filtros = new FlowPane(12, 8);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtro.getItems().addAll("Todas", "Pendiente", "En curso", "En revisión", "Terminada");
        filtro.setValue("Todas");
        filtro.setOnAction(e -> actualizarVista());
        filtroResponsable.getItems().add("Todos");
        filtroResponsable.setValue("Todos");
        filtroResponsable.setOnAction(e -> {
            Usuario elegido = equipo.stream()
                    .filter(u -> u.obtenerNombre().equals(filtroResponsable.getValue()))
                    .findFirst().orElse(null);
            organizador.establecerFiltro(elegido == null ? null : new FiltrarPorResponsable(elegido.obtenerId()));
            actualizarVista();
        });
        ordenamiento.getItems().addAll("Prioridad", "Fecha límite");
        ordenamiento.setValue("Prioridad");
        ordenamiento.setOnAction(e -> {
            organizador.establecerOrdenamiento("Fecha límite".equals(ordenamiento.getValue())
                    ? new OrdenarPorFecha() : new OrdenarPorPrioridad());
            actualizarVista();
        });
        filtros.getChildren().addAll(new Label("Estado:"), filtro,
                new Label("Responsable:"), filtroResponsable,
                new Label("Ordenar por:"), ordenamiento);
        proximoAviso.getStyleClass().add("reminder-card");
        cuerpo.getChildren().addAll(bienvenida, estadisticas, encabezado, filtros, lista, proximoAviso);
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        centro.setCenter(scroll);
        return centro;
    }

    private VBox tarjetaEstadistica(String descripcion, Label valor, String icono) {
        VBox tarjeta = new VBox(6);
        tarjeta.getStyleClass().add("stat-card");
        Label figura = new Label(icono + "   " + descripcion);
        figura.getStyleClass().add("muted");
        valor.getStyleClass().add("stat-number");
        tarjeta.getChildren().addAll(figura, valor);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    private void actualizarVista() {
        if (lista == null || filtro.getValue() == null) return;
        List<ComponenteTarea> tareas = gestor.obtenerTareasPrincipales();
        contadorTotal.setText(String.valueOf(tareas.size()));
        contadorPendientes.setText(String.valueOf(tareas.stream().filter(t -> t.obtenerEstado() instanceof EstadoPendiente).count()));
        contadorActivas.setText(String.valueOf(tareas.stream().filter(t -> t.obtenerEstado() instanceof EstadoEnCurso || t.obtenerEstado() instanceof EstadoEnRevision).count()));
        long completadas = tareas.stream().filter(t -> t.obtenerEstado() instanceof EstadoTerminada).count();
        contadorTerminadas.setText(String.valueOf(completadas));
        double proporcion = tareas.isEmpty() ? 0 : (double) completadas / tareas.size();
        progresoJardin.setText(tareas.isEmpty() ? "Tu jardín comienza con la primera tarea"
                : "Has completado " + completadas + " de " + tareas.size() + " tareas (" + Math.round(proporcion * 100) + "%)");
        barraJardin.setProgress(proporcion);
        String texto = busqueda.getText() == null ? "" : busqueda.getText().trim().toLowerCase(Locale.ROOT);
        actualizarContadorAvisos();
        actualizarProximoAviso();
        lista.getChildren().clear();
        organizador.organizar(tareas).stream()
                .filter(t -> texto.isEmpty()
                        || t.obtenerTitulo().toLowerCase(Locale.ROOT).contains(texto)
                        || t.obtenerDescripcion().toLowerCase(Locale.ROOT).contains(texto)
                        || t.obtenerSubtareas().stream().anyMatch(h -> h.obtenerTitulo().toLowerCase(Locale.ROOT).contains(texto))
                        || t.obtenerResponsables().stream().anyMatch(u -> u.obtenerNombre().toLowerCase(Locale.ROOT).contains(texto)))
                .filter(t -> filtro.getValue().equals("Todas") || t.obtenerEstado().obtenerNombre().equals(filtro.getValue()))
                .forEach(t -> lista.getChildren().add(crearTarjetaTarea(t)));
        if (lista.getChildren().isEmpty()) {
            Label vacio = new Label(tareas.isEmpty()
                    ? "Aún no tienes tareas. ¡Planta tu primer brote!"
                    : "No hay tareas que coincidan con los filtros.");
            vacio.getStyleClass().add("empty");
            Button accion = new Button(tareas.isEmpty() ? "＋ Crear tarea" : "Limpiar filtros");
            accion.getStyleClass().add("primary-button");
            accion.setOnAction(e -> {
                if (tareas.isEmpty()) abrirFormulario(null);
                else {
                    busqueda.clear();
                    filtro.setValue("Todas");
                    filtroResponsable.setValue("Todos");
                    ordenamiento.setValue("Prioridad");
                    actualizarVista();
                }
            });
            VBox estadoVacio = new VBox(12, vacio, accion);
            estadoVacio.setAlignment(Pos.CENTER);
            estadoVacio.getStyleClass().add("empty-box");
            lista.getChildren().add(estadoVacio);
        }
    }

    private VBox crearTarjetaTarea(ComponenteTarea tarea) {
        VBox caja = new VBox(10);
        caja.getStyleClass().add("task-card");
        if (!(tarea.obtenerEstado() instanceof EstadoTerminada)
                && tarea.obtenerFechaLimite().isBefore(LocalDateTime.now())) {
            caja.getStyleClass().add("task-overdue");
        }
        HBox fila = new HBox(15);
        fila.setAlignment(Pos.CENTER_LEFT);
        Node flor = crearFlor(calcularProgreso(tarea));
        VBox contenido = new VBox(5);
        HBox titulo = new HBox(10);
        titulo.setAlignment(Pos.CENTER_LEFT);
        Label nombre = new Label(tarea.obtenerTitulo());
        nombre.getStyleClass().add("task-title");
        nombre.setWrapText(true);
        Label prioridad = new Label(etiquetaPrioridad(tarea.obtenerPrioridad()));
        prioridad.getStyleClass().addAll("priority-tag", "priority-" + etiquetaPrioridad(tarea.obtenerPrioridad()).toLowerCase(Locale.ROOT));
        titulo.getChildren().addAll(nombre, prioridad);
        LocalDateTime ahora = LocalDateTime.now();
        if (!(tarea.obtenerEstado() instanceof EstadoTerminada)) {
            Label urgencia = null;
            if (tarea.obtenerFechaLimite().isBefore(ahora)) {
                urgencia = new Label("Vencida");
                urgencia.getStyleClass().add("overdue-tag");
            } else if (!tarea.obtenerFechaLimite().isAfter(ahora.plusHours(24))) {
                urgencia = new Label("Vence pronto");
                urgencia.getStyleClass().add("soon-tag");
            }
            if (urgencia != null) titulo.getChildren().add(urgencia);
        }
        Label detalle = new Label(tarea.obtenerDescripcion());
        detalle.getStyleClass().add("muted");
        detalle.setWrapText(true);
        Label fecha = new Label("▢  " + tarea.obtenerFechaLimite().format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", new Locale("es", "ES")))
                + "     •     " + nombresResponsables(tarea));
        fecha.getStyleClass().add("card-meta");
        contenido.getChildren().addAll(titulo, detalle, fecha);
        HBox.setHgrow(contenido, Priority.ALWAYS);
        VBox acciones = new VBox(8);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        ComboBox<String> estados = new ComboBox<>();
        estados.getItems().addAll("Pendiente", "En curso", "En revisión", "Terminada");
        estados.setValue(tarea.obtenerEstado().obtenerNombre());
        estados.setOnAction(e -> {
            String nombreEstado = estados.getValue();
            if (nombreEstado.equals(tarea.obtenerEstado().obtenerNombre())) return;
            if (!tarea.cambiarEstado(estadoPorNombre(nombreEstado))) {
                mostrarMensaje("Transición no permitida", "No es posible pasar de "
                        + tarea.obtenerEstado().obtenerNombre() + " a " + nombreEstado + ".");
            } else {
                persistir();
            }
            comprobarRecordatorios();
            actualizarVista();
        });
        HBox botones = new HBox(5);
        Button editar = new Button("Editar");
        editar.getStyleClass().add("small-button");
        editar.setOnAction(e -> abrirFormulario(tarea));
        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("small-button");
        eliminar.setOnAction(e -> {
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar «" + tarea.obtenerTitulo() + "»?", ButtonType.YES, ButtonType.NO);
            if (confirmar.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                quitarAvisoConNotificaciones(tarea.obtenerId());
                gestor.eliminarTarea(tarea.obtenerId());
                persistir();
                actualizarVista();
            }
        });
        botones.getChildren().addAll(editar, eliminar);
        acciones.getChildren().addAll(estados, botones);
        fila.getChildren().addAll(flor, contenido, acciones);
        caja.getChildren().add(fila);
        if (!tarea.obtenerSubtareas().isEmpty()) {
            VBox hijos = new VBox(7);
            hijos.getStyleClass().add("subtask-list");
            for (ComponenteTarea hija : tarea.obtenerSubtareas()) {
                HBox linea = new HBox(9);
                linea.setAlignment(Pos.CENTER_LEFT);
                Label marca = new Label(hija.obtenerEstado() instanceof EstadoTerminada ? "☑" : "☐");
                Label texto = new Label(hija.obtenerTitulo() + "  ·  " + hija.obtenerEstado().obtenerNombre());
                texto.getStyleClass().add("muted");
                Button avanzar = new Button("Avanzar");
                avanzar.getStyleClass().add("small-button");
                avanzar.setDisable(hija.obtenerEstado() instanceof EstadoTerminada);
                avanzar.setOnAction(e -> { avanzarEstado(hija); persistir(); actualizarVista(); });
                Region aire = new Region();
                HBox.setHgrow(aire, Priority.ALWAYS);
                linea.getChildren().addAll(marca, texto, aire, avanzar);
                hijos.getChildren().add(linea);
            }
            caja.getChildren().add(hijos);
        }
        return caja;
    }

    private Node crearFlor(int progreso) {
        Pane lienzo = new Pane();
        lienzo.setMinSize(64, 68);
        lienzo.setPrefSize(64, 68);
        Line tallo = new Line(32, 31, 32, 65);
        tallo.setStroke(Color.web("#7d9374"));
        tallo.setStrokeWidth(2.5);
        Ellipse hoja = new Ellipse(40, 49, 10, 4);
        hoja.setRotate(-28);
        hoja.setFill(Color.web("#7d9374"));
        lienzo.getChildren().addAll(tallo, hoja);
        int petalos = (int) Math.ceil(progreso / 20.0);
        for (int i = 0; i < 5; i++) {
            double angulo = Math.toRadians(i * 72 - 90);
            Ellipse petalo = new Ellipse(32 + Math.cos(angulo) * 15, 25 + Math.sin(angulo) * 15, 8, 13);
            petalo.setRotate(i * 72);
            petalo.setFill(i < petalos ? Color.web("#b68999") : Color.web("#e4e2d9"));
            lienzo.getChildren().add(petalo);
        }
        Circle centro = new Circle(32, 25, 7, Color.web("#b79c65"));
        lienzo.getChildren().add(centro);
        return lienzo;
    }

    private int calcularProgreso(ComponenteTarea tarea) {
        if (tarea.obtenerEstado() instanceof EstadoTerminada) return 100;
        List<ComponenteTarea> hijas = tarea.obtenerSubtareas();
        if (!hijas.isEmpty()) {
            return (int) Math.round(hijas.stream().filter(t -> t.obtenerEstado() instanceof EstadoTerminada).count() * 100.0 / hijas.size());
        }
        if (tarea.obtenerEstado() instanceof EstadoEnRevision) return 80;
        if (tarea.obtenerEstado() instanceof EstadoEnCurso) return 45;
        return 10;
    }

    private void avanzarEstado(ComponenteTarea tarea) {
        if (tarea.obtenerEstado() instanceof EstadoPendiente) tarea.cambiarEstado(new EstadoEnCurso());
        else if (tarea.obtenerEstado() instanceof EstadoEnCurso) tarea.cambiarEstado(new EstadoEnRevision());
        else if (tarea.obtenerEstado() instanceof EstadoEnRevision) tarea.cambiarEstado(new EstadoTerminada());
    }

    private EstadoTarea estadoPorNombre(String nombre) {
        return switch (nombre) {
            case "En curso" -> new EstadoEnCurso();
            case "En revisión" -> new EstadoEnRevision();
            case "Terminada" -> new EstadoTerminada();
            default -> new EstadoPendiente();
        };
    }

    private String etiquetaPrioridad(int valor) {
        return switch (valor) { case 3 -> "Alta"; case 2 -> "Media"; default -> "Baja"; };
    }
    private String nombresResponsables(ComponenteTarea tarea) {
        if (tarea.obtenerResponsables().isEmpty()) return "Sin responsable";
        return String.join(", ", tarea.obtenerResponsables().stream().map(Usuario::obtenerNombre).toList());
    }

    private void abrirFormulario(ComponenteTarea tarea) {
        if (equipo.isEmpty()) {
            mostrarMensaje("Configura tu perfil", "Primero crea tu perfil para asignar responsables a las tareas.");
            abrirPerfil();
            if (equipo.isEmpty()) return;
        }
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(tarea == null ? "Nueva tarea" : "Editar tarea");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button guardar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        guardar.setText("Guardar");
        TextField titulo = new TextField(tarea == null ? "" : tarea.obtenerTitulo());
        TextArea descripcion = new TextArea(tarea == null ? "" : tarea.obtenerDescripcion());
        descripcion.setPrefRowCount(2);
        ComboBox<String> prioridad = new ComboBox<>();
        prioridad.getItems().addAll("Alta", "Media", "Baja");
        prioridad.setValue(tarea == null ? "Media" : etiquetaPrioridad(tarea.obtenerPrioridad()));
        DatePicker fecha = new DatePicker(tarea == null ? LocalDate.now().plusDays(7) : tarea.obtenerFechaLimite().toLocalDate());
        // Selector horario de 24 horas, con precisión de un minuto. Las tareas anteriores
        // conservan su hora de 23:59 (o cualquier hora guardada anteriormente).
        LocalTime horaInicial = tarea == null ? LocalTime.of(18, 0) : tarea.obtenerFechaLimite().toLocalTime();
        ComboBox<String> hora = new ComboBox<>();
        ComboBox<String> minuto = new ComboBox<>();
        for (int h = 0; h < 24; h++) hora.getItems().add(String.format("%02d", h));
        for (int m = 0; m < 60; m++) minuto.getItems().add(String.format("%02d", m));
        hora.setValue(String.format("%02d", horaInicial.getHour()));
        minuto.setValue(String.format("%02d", horaInicial.getMinute()));
        hora.setPrefWidth(80);
        minuto.setPrefWidth(80);
        HBox selectorHora = new HBox(7, hora, new Label(":"), minuto);
        selectorHora.setAlignment(Pos.CENTER_LEFT);
        Label notaHora = new Label("Formato de 24 horas. Los avisos se calculan desde esta fecha y hora.");
        notaHora.getStyleClass().add("muted");
        notaHora.setWrapText(true);
        // Cada campo mantiene la referencia de la subtarea original; así editar su
        // título no destruye su ID ni el estado que ya tenía guardado.
        List<FilaSubtarea> filasSubtareas = new ArrayList<>();
        VBox editorSubtareas = new VBox(7);
        if (tarea != null) {
            for (ComponenteTarea hija : tarea.obtenerSubtareas())
                agregarFilaSubtarea(editorSubtareas, filasSubtareas, hija);
        }
        Button anadirSubtarea = new Button("＋ Añadir subtarea");
        anadirSubtarea.getStyleClass().add("small-button");
        anadirSubtarea.setOnAction(e -> agregarFilaSubtarea(editorSubtareas, filasSubtareas, null));
        Label notaSubtareas = new Label("Puedes renombrar, añadir o quitar subtareas. Sus estados se conservan al renombrar.");
        notaSubtareas.setWrapText(true);
        notaSubtareas.getStyleClass().add("muted");
        FlowPane responsables = new FlowPane(10, 8);
        List<CheckBox> seleccionResponsables = equipo.stream().map(usuario -> {
            CheckBox casilla = new CheckBox(usuario.obtenerNombre());
            casilla.setUserData(usuario);
            casilla.setSelected(tarea == null
                    ? usuarioActual != null && usuario.obtenerId().equals(usuarioActual.obtenerId())
                    : tarea.obtenerResponsables().stream().anyMatch(u -> u.obtenerId().equals(usuario.obtenerId())));
            return casilla;
        }).toList();
        responsables.getChildren().addAll(seleccionResponsables);
        ComboBox<String> aviso = new ComboBox<>();
        aviso.getItems().addAll("Sin recordatorio", "Al vencer", "1 hora antes", "6 horas antes", "24 horas antes",
                "48 horas antes", "7 días antes");
        Recordatorio previo = tarea == null ? null : gestorRecordatorios.buscarPorTarea(tarea.obtenerId());
        aviso.setValue(previo == null ? (tarea == null ? "24 horas antes" : "Sin recordatorio")
                : etiquetaAnticipacion(previo.obtenerAnticipacionHoras()));
        VBox formulario = new VBox(9,
                new Label("Título"), titulo,
                new Label("Descripción"), descripcion,
                new Label("Prioridad"), prioridad,
                new Label("Fecha límite"), fecha,
                new Label("Hora límite (HH:mm)"), selectorHora, notaHora,
                new Label("Recordatorio"), aviso,
                new Label("Responsables (selecciona al menos uno)"), responsables,
                new Label("Subtareas"), editorSubtareas, anadirSubtarea, notaSubtareas);
        formulario.setPadding(new Insets(14));
        formulario.setPrefWidth(360);
        ScrollPane scrollFormulario = new ScrollPane(formulario);
        scrollFormulario.setFitToWidth(true);
        scrollFormulario.setPrefViewportHeight(510);
        scrollFormulario.setPrefViewportWidth(430);
        dialogo.getDialogPane().setContent(scrollFormulario);
        prepararDialogo(dialogo);
        // La ventana permanece abierta si los datos son inválidos.
        guardar.addEventFilter(ActionEvent.ACTION, e -> {
            String mensaje = null;
            if (titulo.getText().trim().isEmpty()) mensaje = "Escribe un título para la tarea.";
            else if (fecha.getValue() == null) mensaje = "Selecciona una fecha límite.";
            else if (seleccionResponsables.stream().noneMatch(CheckBox::isSelected))
                mensaje = "Selecciona al menos un responsable.";
            else if (filasSubtareas.stream().anyMatch(f -> f.campo().getText().trim().isEmpty()))
                mensaje = "Escribe un título en cada subtarea o quita las filas vacías.";
            if (mensaje != null) {
                e.consume();
                mostrarMensaje("Revisa el formulario", mensaje);
            }
        });
        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        int valorPrioridad = switch (prioridad.getValue()) { case "Alta" -> 3; case "Baja" -> 1; default -> 2; };
        LocalDateTime vencimiento = fecha.getValue().atTime(
                Integer.parseInt(hora.getValue()), Integer.parseInt(minuto.getValue()));
        List<Usuario> personasElegidas = seleccionResponsables.stream()
                .filter(CheckBox::isSelected)
                .map(c -> (Usuario) c.getUserData())
                .toList();
        if (personasElegidas.isEmpty()) {
            mostrarMensaje("Responsable requerido", "Selecciona al menos un responsable.");
            return;
        }
        ComponenteTarea tareaGuardada;
        if (tarea != null) {
            tareaGuardada = tarea;
            if (!(tarea instanceof TareaCompuesta) && !filasSubtareas.isEmpty()) {
                // De simple a compuesta sin cambiar el ID ni perder su estado.
                tareaGuardada = gestor.convertirEnCompuesta(tarea.obtenerId());
                gestorRecordatorios.actualizarReferenciaTarea(tareaGuardada);
            }
            gestor.modificarTarea(tareaGuardada.obtenerId(), titulo.getText().trim(),
                    descripcion.getText(), valorPrioridad, vencimiento);
            tareaGuardada.asignarResponsables(personasElegidas);
            if (tareaGuardada instanceof TareaCompuesta) {
                Set<String> mantenerIds = new HashSet<>();
                for (FilaSubtarea fila : filasSubtareas) {
                    if (fila.original() != null) mantenerIds.add(fila.original().obtenerId());
                }
                for (ComponenteTarea hija : List.copyOf(tareaGuardada.obtenerSubtareas())) {
                    if (!mantenerIds.contains(hija.obtenerId())) {
                        quitarAvisoConNotificaciones(hija.obtenerId());
                        gestor.eliminarTarea(hija.obtenerId());
                    }
                }
            }
        } else {
            tareaGuardada = filasSubtareas.isEmpty()
                    ? gestor.crearTareaSimple(titulo.getText().trim(), descripcion.getText(), valorPrioridad, vencimiento)
                    : gestor.crearTareaCompuesta(titulo.getText().trim(), descripcion.getText(), valorPrioridad, vencimiento);
            tareaGuardada.asignarResponsables(personasElegidas);
        }
        for (FilaSubtarea fila : filasSubtareas) {
            String nombreSubtarea = fila.campo().getText().trim();
            if (fila.original() != null) {
                ComponenteTarea original = fila.original();
                gestor.modificarTarea(original.obtenerId(), nombreSubtarea,
                        original.obtenerDescripcion(), original.obtenerPrioridad(), original.obtenerFechaLimite());
            } else {
                TareaSimple hija = gestor.crearTareaSimple(nombreSubtarea, "", 2, vencimiento);
                hija.asignarResponsables(personasElegidas);
                gestor.agregarSubtarea(tareaGuardada.obtenerId(), hija.obtenerId());
            }
        }
        configurarAviso(tareaGuardada, horasDeAviso(aviso.getValue()));
        persistir();
        comprobarRecordatorios();
        actualizarVista();
    }

    private record FilaSubtarea(ComponenteTarea original, TextField campo) { }

    private void agregarFilaSubtarea(VBox editor, List<FilaSubtarea> filas, ComponenteTarea original) {
        TextField campo = new TextField(original == null ? "" : original.obtenerTitulo());
        campo.setPromptText("Nombre de la subtarea");
        HBox.setHgrow(campo, Priority.ALWAYS);
        Button quitar = new Button("✕");
        quitar.setTooltip(new Tooltip("Quitar subtarea"));
        quitar.getStyleClass().add("small-button");
        HBox linea = new HBox(7, campo, quitar);
        linea.setAlignment(Pos.CENTER_LEFT);
        FilaSubtarea fila = new FilaSubtarea(original, campo);
        filas.add(fila);
        editor.getChildren().add(linea);
        quitar.setOnAction(e -> {
            filas.remove(fila);
            editor.getChildren().remove(linea);
        });
    }

    private void prepararDialogo(Dialog<?> dialogo) {
        DialogPane panel = dialogo.getDialogPane();
        panel.getStylesheets().add(getClass().getResource("/styles/taskbloom.css").toExternalForm());
        panel.getStyleClass().add("task-dialog");
        if (modoOscuro) panel.getStyleClass().add("dark");
    }

    private void mostrarMensaje(String titulo, String contenido) {
        Alert aviso = new Alert(Alert.AlertType.INFORMATION, contenido, ButtonType.OK);
        aviso.setTitle(titulo);
        aviso.setHeaderText(null);
        aviso.showAndWait();
    }

    /** Crea o edita el usuario local; no es un servicio de autenticación. */
    private void abrirPerfil() {
        TextInputDialog dialogo = new TextInputDialog(usuarioActual == null ? "" : usuarioActual.obtenerNombre());
        dialogo.setTitle("Mi perfil");
        dialogo.setHeaderText(usuarioActual == null ? "¡Bienvenido/a! Configura tu nombre" : "Editar mi nombre");
        dialogo.setContentText("Nombre:");
        var respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty()) return;
        String nombre = respuesta.get().trim();
        if (nombre.isBlank()) {
            mostrarMensaje("Nombre requerido", "Debes escribir un nombre para tu perfil.");
            return;
        }
        if (nombreDuplicado(nombre, usuarioActual)) {
            mostrarMensaje("Nombre duplicado", "Ya existe un miembro del equipo con ese nombre.");
            return;
        }
        if (usuarioActual == null) {
            usuarioActual = new Usuario(UUID.randomUUID().toString(), nombre);
            equipo.add(usuarioActual);
        } else {
            usuarioActual.actualizarNombre(nombre);
        }
        sincronizarObservadores();
        persistir();
        actualizarPerfil();
        actualizarFiltroResponsables();
        actualizarVista();
    }

    /** Administra el equipo desde la aplicación, sin nombres fijos en el código. */
    private void abrirEquipo() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Gestionar equipo");
        dialogo.setHeaderText("Añade, cambia el nombre o elimina miembros del equipo");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<Usuario> miembros = new ListView<>();
        miembros.setPrefHeight(215);
        miembros.setCellFactory(lista -> new ListCell<>() {
            @Override protected void updateItem(Usuario usuario, boolean vacio) {
                super.updateItem(usuario, vacio);
                setText(vacio || usuario == null ? null : usuario.obtenerNombre()
                        + (usuarioActual != null && usuario.obtenerId().equals(usuarioActual.obtenerId()) ? "  (tú)" : ""));
            }
        });
        miembros.getItems().setAll(equipo);
        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Nombre del miembro");
        Button agregar = new Button("Añadir");
        Button renombrar = new Button("Renombrar");
        Button eliminar = new Button("Eliminar");
        agregar.setOnAction(e -> {
            String nombre = campoNombre.getText().trim();
            if (nombre.isBlank() || nombreDuplicado(nombre, null)) {
                mostrarMensaje("Nombre inválido", "Escribe un nombre no vacío que no esté repetido.");
                return;
            }
            Usuario nuevo = new Usuario(UUID.randomUUID().toString(), nombre);
            equipo.add(nuevo);
            miembros.getItems().setAll(equipo);
            miembros.getSelectionModel().select(nuevo);
            campoNombre.clear();
            cambiosEnEquipo();
        });
        renombrar.setOnAction(e -> {
            Usuario elegido = miembros.getSelectionModel().getSelectedItem();
            if (elegido == null) { mostrarMensaje("Selecciona un miembro", "Primero selecciona a quién renombrar."); return; }
            String nombre = campoNombre.getText().trim();
            if (nombre.isBlank() || nombreDuplicado(nombre, elegido)) {
                mostrarMensaje("Nombre inválido", "Escribe un nombre no vacío que no esté repetido.");
                return;
            }
            elegido.actualizarNombre(nombre);
            miembros.refresh();
            campoNombre.clear();
            cambiosEnEquipo();
        });
        eliminar.setOnAction(e -> {
            Usuario elegido = miembros.getSelectionModel().getSelectedItem();
            if (elegido == null) { mostrarMensaje("Selecciona un miembro", "Primero selecciona a quién eliminar."); return; }
            if (usuarioActual != null && elegido.obtenerId().equals(usuarioActual.obtenerId())) {
                mostrarMensaje("Tu perfil", "Tu usuario principal no puede eliminarse desde Equipo; puedes renombrarlo desde Mi perfil.");
                return;
            }
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Eliminar a «" + elegido.obtenerNombre() + "» del equipo? También se quitará de las tareas asignadas.",
                    ButtonType.YES, ButtonType.NO);
            if (confirmar.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
            equipo.remove(elegido);
            gestorNotificaciones.eliminarPorUsuario(elegido.obtenerId());
            for (ComponenteTarea tarea : gestor.obtenerTareasPrincipales()) quitarResponsable(tarea, elegido.obtenerId());
            miembros.getItems().setAll(equipo);
            cambiosEnEquipo();
        });
        VBox contenido = new VBox(9,
                new Label("Miembros del equipo"), miembros,
                new Label("Nombre"), campoNombre,
                new HBox(8, agregar, renombrar, eliminar),
                new Label("Eliminar un miembro no elimina sus tareas."));
        contenido.setPadding(new Insets(12));
        contenido.setPrefWidth(390);
        dialogo.getDialogPane().setContent(contenido);
        prepararDialogo(dialogo);
        dialogo.showAndWait();
    }

    private void quitarResponsable(ComponenteTarea tarea, String idUsuario) {
        tarea.asignarResponsables(tarea.obtenerResponsables().stream()
                .filter(u -> !u.obtenerId().equals(idUsuario)).toList());
        for (ComponenteTarea hija : tarea.obtenerSubtareas()) quitarResponsable(hija, idUsuario);
    }

    private boolean nombreDuplicado(String nombre, Usuario ignorar) {
        return equipo.stream().anyMatch(u -> u != ignorar && u.obtenerNombre().equalsIgnoreCase(nombre));
    }

    private void cambiosEnEquipo() {
        sincronizarObservadores();
        persistir();
        actualizarFiltroResponsables();
        actualizarPerfil();
        actualizarVista();
    }

    private void actualizarPerfil() {
        String nombre = usuarioActual == null ? "Configurar perfil" : usuarioActual.obtenerNombre();
        botonPerfil.setText("☺  " + nombre + "  ✎");
        String saludo = LocalTime.now().getHour() < 12 ? "Buenos días" :
                LocalTime.now().getHour() < 19 ? "Buenas tardes" : "Buenas noches";
        tituloBienvenida.setText(usuarioActual == null ? "Bienvenido/a a TaskBloom" : saludo + ", " + nombre);
    }

    private void actualizarFiltroResponsables() {
        String anterior = filtroResponsable.getValue();
        filtroResponsable.getItems().clear();
        filtroResponsable.getItems().add("Todos");
        equipo.forEach(u -> filtroResponsable.getItems().add(u.obtenerNombre()));
        filtroResponsable.setValue(filtroResponsable.getItems().contains(anterior) ? anterior : "Todos");
        Usuario elegido = equipo.stream().filter(u -> u.obtenerNombre().equals(filtroResponsable.getValue()))
                .findFirst().orElse(null);
        organizador.establecerFiltro(elegido == null ? null : new FiltrarPorResponsable(elegido.obtenerId()));
    }

    private void persistir() {
        try {
            repositorio.guardarEstado(gestor, equipo, usuarioActual, modoOscuro,
                    gestorRecordatorios, gestorNotificaciones);
        } catch (RuntimeException error) {
            mostrarError("No se pudieron guardar los cambios", error);
        }
    }

    private void mostrarError(String titulo, Throwable error) {
        Alert aviso = new Alert(Alert.AlertType.ERROR,
                error.getMessage() + (error.getCause() == null ? "" : "\n" + error.getCause().getMessage()),
                ButtonType.OK);
        aviso.setTitle(titulo);
        aviso.setHeaderText(null);
        aviso.showAndWait();
    }

    /** Mantiene un ObservadorUsuario suscrito por cada usuario del equipo. */
    private void sincronizarObservadores() {
        Set<String> vigentes = equipo.stream().map(Usuario::obtenerId)
                .collect(java.util.stream.Collectors.toSet());
        for (String id : List.copyOf(observadoresEquipo.keySet())) {
            if (!vigentes.contains(id)) {
                gestorRecordatorios.desuscribir(observadoresEquipo.remove(id));
            }
        }
        for (Usuario u : equipo) {
            if (!observadoresEquipo.containsKey(u.obtenerId())) {
                ObservadorUsuario observador = new ObservadorUsuario(u, gestorNotificaciones);
                observadoresEquipo.put(u.obtenerId(), observador);
                gestorRecordatorios.suscribir(observador);
            }
        }
    }

    private static String etiquetaAnticipacion(int horas) {
        return switch (horas) {
            case 0 -> "Al vencer";
            case 1 -> "1 hora antes";
            case 6 -> "6 horas antes";
            case 24 -> "24 horas antes";
            case 48 -> "48 horas antes";
            case 168 -> "7 días antes";
            default -> "Sin recordatorio";
        };
    }

    private static Integer horasDeAviso(String etiqueta) {
        return switch (etiqueta == null ? "" : etiqueta) {
            case "Al vencer" -> 0;
            case "1 hora antes" -> 1;
            case "6 horas antes" -> 6;
            case "24 horas antes" -> 24;
            case "48 horas antes" -> 48;
            case "7 días antes" -> 168;
            default -> null;
        };
    }

    private void quitarAvisoConNotificaciones(String idTarea) {
        Recordatorio anterior = gestorRecordatorios.buscarPorTarea(idTarea);
        if (anterior != null) {
            gestorNotificaciones.eliminarPorRecordatorios(Set.of(anterior.obtenerId()));
            gestorRecordatorios.quitarRecordatorio(idTarea);
        }
    }

    private void configurarAviso(ComponenteTarea tarea, Integer horas) {
        if (horas == null) {
            quitarAvisoConNotificaciones(tarea.obtenerId());
            return;
        }
        Recordatorio anterior = gestorRecordatorios.buscarPorTarea(tarea.obtenerId());
        Recordatorio nuevo = gestorRecordatorios.programarRecordatorio(tarea, horas);
        if (anterior != null && !anterior.obtenerId().equals(nuevo.obtenerId())) {
            gestorNotificaciones.eliminarPorRecordatorios(Set.of(anterior.obtenerId()));
        }
    }

    private void comprobarRecordatorios() {
        int enviados = gestorRecordatorios.verificarVencimientos();
        if (enviados > 0) persistir();
        actualizarContadorAvisos();
        actualizarProximoAviso();
    }

    private void actualizarContadorAvisos() {
        long pendientes = gestorNotificaciones.contarNoLeidas(usuarioActual);
        botonNotificaciones.setText("🔔  Avisos" + (pendientes > 0 ? " (" + pendientes + ")" : ""));
        if (pendientes > 0 && !botonNotificaciones.getStyleClass().contains("notification-unread"))
            botonNotificaciones.getStyleClass().add("notification-unread");
        else if (pendientes == 0) botonNotificaciones.getStyleClass().remove("notification-unread");
    }

    private void actualizarProximoAviso() {
        proximoAviso.getChildren().clear();
        Label subtitulo = new Label("RECORDATORIOS");
        subtitulo.getStyleClass().add("eyebrow");
        Recordatorio siguiente = gestorRecordatorios.obtenerRecordatorios().stream()
                .filter(r -> !r.estaEnviado() && !(r.obtenerTarea().obtenerEstado() instanceof EstadoTerminada))
                .min(Comparator.comparing(Recordatorio::obtenerFechaAviso)).orElse(null);
        Label detalle = new Label(siguiente == null ? "Sin recordatorios pendientes"
                : "«" + siguiente.obtenerTarea().obtenerTitulo() + "» · avisar el "
                + siguiente.obtenerFechaAviso().format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es", "ES"))));
        detalle.getStyleClass().add("task-title");
        Button ver = new Button("Ver recordatorios");
        ver.getStyleClass().add("small-button");
        ver.setOnAction(e -> abrirRecordatorios());
        proximoAviso.getChildren().addAll(subtitulo, detalle, ver);
    }

    /** Agenda real de vencimientos a partir de las tareas guardadas (sin datos de ejemplo). */
    private void abrirCalendario() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Calendario de tareas");
        dialogo.setHeaderText("Agenda de vencimientos");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        DatePicker dia = new DatePicker(LocalDate.now());
        CheckBox soloDia = new CheckBox("Mostrar solo la fecha seleccionada");
        VBox eventos = new VBox(8);
        Runnable refrescar = () -> {
            eventos.getChildren().clear();
            List<ComponenteTarea> ordenadas = gestor.obtenerTareasPrincipales().stream()
                    .filter(t -> !soloDia.isSelected()
                            || t.obtenerFechaLimite().toLocalDate().equals(dia.getValue()))
                    .sorted(Comparator.comparing(ComponenteTarea::obtenerFechaLimite)).toList();
            if (ordenadas.isEmpty()) eventos.getChildren().add(new Label("No hay tareas para mostrar."));
            for (ComponenteTarea t : ordenadas) {
                Label titulo = new Label(t.obtenerTitulo());
                titulo.getStyleClass().add("task-title");
                Label detalle = new Label(t.obtenerFechaLimite().format(
                        DateTimeFormatter.ofPattern("EEEE d MMM, HH:mm", new Locale("es", "ES")))
                        + "  ·  " + t.obtenerEstado().obtenerNombre() + "  ·  " + nombresResponsables(t));
                detalle.getStyleClass().add("muted");
                detalle.setWrapText(true);
                VBox fila = new VBox(5, titulo, detalle);
                fila.getStyleClass().add("task-card");
                eventos.getChildren().add(fila);
            }
        };
        soloDia.selectedProperty().addListener((obs, old, val) -> refrescar.run());
        dia.valueProperty().addListener((obs, old, val) -> refrescar.run());
        refrescar.run();
        ScrollPane scroll = new ScrollPane(eventos);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(390);
        scroll.setPrefViewportWidth(520);
        VBox contenido = new VBox(12, new HBox(12, dia, soloDia), scroll);
        contenido.setPadding(new Insets(12));
        dialogo.getDialogPane().setContent(contenido);
        prepararDialogo(dialogo);
        dialogo.showAndWait();
    }

    /** Copia de exportación: no modifica SQLite ni cambia los estados de las tareas. */
    private void exportarCSV() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar tareas a CSV");
        selector.setInitialFileName("taskbloom-tareas.csv");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        java.io.File destino = selector.showSaveDialog(escena.getWindow());
        if (destino == null) return;
        StringBuilder csv = new StringBuilder("Tipo,ID,ID padre,Título,Descripción,Estado,Prioridad,Fecha límite,Responsables\r\n");
        for (ComponenteTarea t : gestor.obtenerTareasPrincipales()) escribirFilaCSV(csv, t, "");
        try {
            Files.writeString(destino.toPath(), "\uFEFF" + csv, StandardCharsets.UTF_8);
            mostrarMensaje("Exportación completada", "CSV guardado en: " + destino.getAbsolutePath());
        } catch (IOException error) {
            mostrarError("No se pudo exportar el CSV", error);
        }
    }

    private void escribirFilaCSV(StringBuilder csv, ComponenteTarea t, String padre) {
        List<String> valores = List.of(t instanceof TareaCompuesta ? "Compuesta" : "Simple",
                t.obtenerId(), padre, t.obtenerTitulo(), t.obtenerDescripcion(),
                t.obtenerEstado().obtenerNombre(), etiquetaPrioridad(t.obtenerPrioridad()),
                t.obtenerFechaLimite().toString(), nombresResponsables(t));
        csv.append(String.join(",", valores.stream().map(TaskBloomApp::escaparCSV).toList())).append("\r\n");
        for (ComponenteTarea hija : t.obtenerSubtareas()) escribirFilaCSV(csv, hija, t.obtenerId());
    }

    private static String escaparCSV(String valor) {
        // Evitar que aplicaciones de hojas de cálculo interpreten un título como fórmula.
        String seguro = !valor.isEmpty() && "=+-@".indexOf(valor.charAt(0)) >= 0
                ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    private void abrirRecordatorios() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Recordatorios de tareas");
        dialogo.setHeaderText("Avisos programados para las tareas");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox contenido = new VBox(10);
        List<Recordatorio> ordenados = gestorRecordatorios.obtenerRecordatorios().stream()
                .sorted(Comparator.comparing(Recordatorio::obtenerFechaAviso)).toList();
        if (ordenados.isEmpty()) contenido.getChildren().add(new Label("Aún no hay recordatorios. Crea o edita una tarea para programar uno."));
        for (Recordatorio r : ordenados) {
            Label titulo = new Label("✿  " + r.obtenerTarea().obtenerTitulo());
            titulo.getStyleClass().add("task-title");
            Label detalle = new Label("Vence: " + r.obtenerTarea().obtenerFechaLimite().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "   ·   Aviso: " + r.obtenerFechaAviso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "   ·   " + etiquetaAnticipacion(r.obtenerAnticipacionHoras())
                    + "   ·   " + (r.estaEnviado() ? "Enviado" : "Pendiente"));
            detalle.setWrapText(true);
            detalle.getStyleClass().add("muted");
            Label destinatarios = new Label("Responsables: " + nombresResponsables(r.obtenerTarea()));
            destinatarios.getStyleClass().add("muted");
            VBox tarjeta = new VBox(4, titulo, detalle, destinatarios);
            tarjeta.getStyleClass().add("task-card");
            contenido.getChildren().add(tarjeta);
        }
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(350);
        scroll.setPrefViewportWidth(500);
        dialogo.getDialogPane().setContent(scroll);
        prepararDialogo(dialogo);
        dialogo.showAndWait();
    }

    private void abrirNotificaciones() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Mis notificaciones");
        dialogo.setHeaderText(usuarioActual == null ? "Configura tu perfil para ver tus avisos"
                : "Notificaciones para " + usuarioActual.obtenerNombre());
        ButtonType leidas = new ButtonType("Marcar todas como leídas", ButtonBar.ButtonData.APPLY);
        dialogo.getDialogPane().getButtonTypes().addAll(leidas, ButtonType.CLOSE);
        VBox contenido = new VBox(9);
        List<Notificacion> propias = gestorNotificaciones.consultarNotificaciones(usuarioActual);
        if (propias.isEmpty()) contenido.getChildren().add(new Label("No tienes notificaciones todavía."));
        for (Notificacion n : propias) {
            Label titulo = new Label((n.estaLeida() ? "✓  " : "●  ") + n.obtenerMensaje());
            titulo.setWrapText(true);
            titulo.getStyleClass().add("task-title");
            Label fecha = new Label(n.obtenerFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            fecha.getStyleClass().add("muted");
            VBox tarjeta = new VBox(5, titulo, fecha);
            tarjeta.getStyleClass().add("task-card");
            contenido.getChildren().add(tarjeta);
        }
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(460);
        scroll.setPrefViewportHeight(330);
        dialogo.getDialogPane().setContent(scroll);
        prepararDialogo(dialogo);
        if (dialogo.showAndWait().orElse(ButtonType.CLOSE) == leidas) {
            gestorNotificaciones.marcarTodasComoLeidas(usuarioActual);
            persistir();
            actualizarContadorAvisos();
        }
    }

    @Override public void stop() {
        if (comprobadorRecordatorios != null) comprobadorRecordatorios.stop();
    }

    public static void main(String[] args) { launch(args); }
}
