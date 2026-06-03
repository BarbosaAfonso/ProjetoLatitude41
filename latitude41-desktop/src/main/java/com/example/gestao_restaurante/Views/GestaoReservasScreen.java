package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class GestaoReservasScreen {

    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private TableView<JsonNode> tabela;

    @FXML
    private TableColumn<JsonNode, String> colId;

    @FXML
    private TableColumn<JsonNode, String> colDataHora;

    @FXML
    private TableColumn<JsonNode, String> colEstado;

    @FXML
    private TableColumn<JsonNode, String> colMesa;

    @FXML
    private TableColumn<JsonNode, String> colUtilizador;

    @FXML
    private TableColumn<JsonNode, String> colAcoes;

    @FXML
    private TextField pesquisaField;

    @FXML
    private FlowPane filtrosEstadoBox;

    @FXML
    private ToggleButton mostrarCanceladasToggle;

    @FXML
    private ComboBox<String> ordenarPorCombo;

    @FXML
    private ComboBox<String> direcaoOrdenacaoCombo;

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> dadosFiltrados = new FilteredList<>(dados, reserva -> true);
    private String filtroEstadoAtivo = "todas";

    @FXML
    private void initialize() {
        configurarColunas();
        configurarFiltrosEstado();
        configurarOrdenacao();

        SortedList<JsonNode> dadosOrdenados = new SortedList<>(dadosFiltrados);
        dadosOrdenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(dadosOrdenados);

        tabela.setRowFactory(table -> new TableRowCancelado<>("CANCELADA"));

        if (pesquisaField != null) {
            pesquisaField.textProperty().addListener((obs, oldValue, newValue) -> atualizarFiltros());
        }

        carregarDados();
        aplicarOrdenacao();
    }

    private void configurarColunas() {
        colId.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "id")));
        colDataHora.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "dataHora")));
        colEstado.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "estado")));
        colMesa.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.nestedText(data.getValue(), "numMesa", "id")));
        colUtilizador.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.nestedText(data.getValue(), "idUtilizador", "id")));
        colAcoes.setCellValueFactory(data -> new ReadOnlyStringWrapper("acoes"));
        configurarColunasTextoCentradas();

        colDataHora.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "" : formatarDataHora(item));
                setGraphic(null);
                setAlignment(Pos.CENTER);
            }
        });
        colId.setComparator(this::compararInteiros);
        colDataHora.setComparator(this::compararDatas);
        colMesa.setComparator(this::compararInteiros);
        colUtilizador.setComparator(this::compararInteiros);

        colEstado.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            private final HBox wrapper = new HBox(badge);

            {
                wrapper.setAlignment(Pos.CENTER);
                badge.getStyleClass().add("estado-badge");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    return;
                }

                badge.setText(formatarEstado(item));
                badge.getStyleClass().setAll("estado-badge", classeEstadoReserva(item));
                setGraphic(wrapper);
            }
        });

        colAcoes.setSortable(false);
        colAcoes.setReorderable(false);
        colAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnEditar = new Button("\u270E");
            private final Button btnCancelar = new Button("Cancelar");
            private final HBox wrapper = new HBox(8, btnEditar, btnCancelar);

            {
                wrapper.setAlignment(Pos.CENTER);
                btnEditar.getStyleClass().addAll("acao-button", "acao-editar");
                btnCancelar.getStyleClass().addAll("acao-button", "acao-cancelar");

                btnEditar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        editarReserva(linha);
                    }
                });

                btnCancelar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        cancelarReserva(linha);
                    }
                });

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    JsonNode linha = (JsonNode) getTableRow().getItem();
                    boolean cancelada = isReservaCancelada(linha);
                    btnCancelar.setDisable(false);
                    btnCancelar.setText(cancelada ? "Repor" : "Cancelar");
                    setGraphic(wrapper);
                }
            }
        });
    }

    private void configurarColunasTextoCentradas() {
        colId.setCellFactory(column -> criarCelulaTextoCentrada());
        colMesa.setCellFactory(column -> criarCelulaTextoCentrada());
        colUtilizador.setCellFactory(column -> criarCelulaTextoCentrada());
    }

    private TableCell<JsonNode, String> criarCelulaTextoCentrada() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
                setAlignment(Pos.CENTER);
            }
        };
    }

    @FXML
    private void onNovaReserva() {
        onAdicionar();
    }

    @FXML
    private void onPesquisar() {
        atualizarFiltros();
    }

    @FXML
    private void onOrdenacaoAlterada() {
        aplicarOrdenacao();
    }

    @FXML
    private void onToggleCanceladas() {
        if (mostrarCanceladasToggle != null) {
            mostrarCanceladasToggle.setText(mostrarCanceladasToggle.isSelected()
                    ? "Ocultar canceladas"
                    : "Mostrar canceladas");
        }
        atualizarFiltros();
    }

    @FXML
    private void onAdicionar() {
        Optional<ObjectNode> payload = dialogoReserva(null);
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().post("/reservas", body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Reservas", e.getMessage());
            }
        });
    }

    @FXML
    private void onEditar() {
        JsonNode selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            ViewUtils.showWarning("Reservas", "Selecione uma reserva para editar.");
            return;
        }

        editarReserva(selecionada);
    }

    @FXML
    private void onApagar() {
        JsonNode selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            ViewUtils.showWarning("Reservas", "Selecione uma reserva para cancelar.");
            return;
        }

        cancelarReserva(selecionada);
    }

    @FXML
    private void onAtualizar() {
        carregarDados();
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void editarReserva(JsonNode selecionada) {
        Optional<ObjectNode> payload = dialogoReserva(selecionada);
        payload.ifPresent(body -> {
            try {
                String id = ViewUtils.text(selecionada, "id");
                DesktopAppContext.apiService().put("/reservas/" + id, body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Reservas", e.getMessage());
            }
        });
    }

    private void cancelarReserva(JsonNode selecionada) {
        String id = ViewUtils.text(selecionada, "id");
        if (isReservaCancelada(selecionada)) {
            reporReserva(selecionada);
            return;
        }

        if (!ViewUtils.confirm("Reservas", "Cancelar reserva ID " + id + "?")) {
            return;
        }

        try {
            JsonNode reservaAtual = DesktopAppContext.apiService().getObject("/reservas/" + id);
            DesktopAppContext.apiService().put("/reservas/" + id, criarPayloadReserva(reservaAtual, "CANCELADA"));
            carregarDados();
        } catch (RuntimeException e) {
            ViewUtils.showError("Reservas", e.getMessage());
        }
    }

    private void reporReserva(JsonNode selecionada) {
        String id = ViewUtils.text(selecionada, "id");
        if (!ViewUtils.confirm("Reservas", "Repor reserva ID " + id + " para CONFIRMADA?")) {
            return;
        }

        try {
            JsonNode reservaAtual = DesktopAppContext.apiService().getObject("/reservas/" + id);
            DesktopAppContext.apiService().put("/reservas/" + id, criarPayloadReserva(reservaAtual, "CONFIRMADA"));
            carregarDados();
        } catch (RuntimeException e) {
            ViewUtils.showError("Reservas", e.getMessage());
        }
    }

    private void carregarDados() {
        try {
            ArrayNode resposta = DesktopAppContext.apiService().getArray("/reservas");
            dados.clear();
            resposta.forEach(dados::add);
            atualizarFiltros();
            aplicarOrdenacao();
        } catch (RuntimeException e) {
            ViewUtils.showError("Reservas", e.getMessage());
        }
    }

    private void configurarFiltrosEstado() {
        if (filtrosEstadoBox == null) {
            return;
        }

        filtrosEstadoBox.getChildren().clear();
        for (String filtro : List.of("Todas", "Confirmadas", "Pendentes", "Canceladas")) {
            Button button = new Button(filtro);
            button.getStyleClass().add("filtro-chip");
            button.setOnAction(event -> selecionarFiltroEstado(filtro));
            filtrosEstadoBox.getChildren().add(button);
        }
        atualizarEstiloFiltrosEstado();
    }

    private void selecionarFiltroEstado(String filtro) {
        filtroEstadoAtivo = normalizarTexto(filtro);
        atualizarEstiloFiltrosEstado();
        atualizarFiltros();
    }

    private void atualizarEstiloFiltrosEstado() {
        if (filtrosEstadoBox == null) {
            return;
        }

        filtrosEstadoBox.getChildren().forEach(node -> {
            if (!(node instanceof Button button)) {
                return;
            }

            String valor = normalizarTexto(button.getText());
            if (valor.equals(filtroEstadoAtivo)) {
                if (!button.getStyleClass().contains("filtro-chip-activo")) {
                    button.getStyleClass().add("filtro-chip-activo");
                }
            } else {
                button.getStyleClass().remove("filtro-chip-activo");
            }
        });
    }

    private void configurarOrdenacao() {
        if (ordenarPorCombo == null || direcaoOrdenacaoCombo == null) {
            return;
        }

        ordenarPorCombo.setItems(FXCollections.observableArrayList("Data/Hora", "ID", "Estado", "Mesa", "Cliente"));
        direcaoOrdenacaoCombo.setItems(FXCollections.observableArrayList("Ascendente", "Descendente"));
        ordenarPorCombo.setValue("Data/Hora");
        direcaoOrdenacaoCombo.setValue("Ascendente");
    }

    private void atualizarFiltros() {
        String termo = pesquisaField == null ? "" : pesquisaField.getText().trim().toLowerCase(Locale.ROOT);
        boolean mostrarCanceladas = mostrarCanceladasToggle != null && mostrarCanceladasToggle.isSelected();
        dadosFiltrados.setPredicate(reserva -> correspondeEstado(reserva)
                && correspondeCanceladas(reserva, mostrarCanceladas)
                && correspondePesquisa(reserva, termo));
    }

    private boolean correspondeEstado(JsonNode reserva) {
        String estado = normalizarTexto(ViewUtils.text(reserva, "estado"));
        return switch (filtroEstadoAtivo) {
            case "confirmadas" -> "confirmada".equals(estado);
            case "pendentes" -> "pendente".equals(estado);
            case "canceladas" -> "cancelada".equals(estado);
            default -> true;
        };
    }

    private boolean correspondeCanceladas(JsonNode reserva, boolean mostrarCanceladas) {
        return mostrarCanceladas || !isReservaCancelada(reserva);
    }

    private boolean correspondePesquisa(JsonNode reserva, String termo) {
        if (termo.isBlank()) {
            return true;
        }

        return contem(reserva, "id", termo)
                || contem(reserva, "dataHora", termo)
                || contem(reserva, "estado", termo)
                || ViewUtils.nestedText(reserva, "numMesa", "id").toLowerCase(Locale.ROOT).contains(termo)
                || ViewUtils.nestedText(reserva, "idUtilizador", "id").toLowerCase(Locale.ROOT).contains(termo);
    }

    private void aplicarOrdenacao() {
        if (tabela == null || ordenarPorCombo == null || direcaoOrdenacaoCombo == null) {
            return;
        }

        TableColumn<JsonNode, ?> coluna = switch (ordenarPorCombo.getValue()) {
            case "ID" -> colId;
            case "Estado" -> colEstado;
            case "Mesa" -> colMesa;
            case "Cliente" -> colUtilizador;
            case "Data/Hora" -> colDataHora;
            default -> colDataHora;
        };

        coluna.setSortType("Descendente".equals(direcaoOrdenacaoCombo.getValue())
                ? TableColumn.SortType.DESCENDING
                : TableColumn.SortType.ASCENDING);
        tabela.getSortOrder().setAll(coluna);
    }

    private Optional<ObjectNode> dialogoReserva(JsonNode atual) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(atual == null ? "Adicionar Reserva" : "Editar Reserva");

            LocalDateTime dataHoraAtual = parseDataHora(ViewUtils.text(atual, "dataHora"));

            DatePicker dataPicker = new DatePicker(dataHoraAtual == null ? LocalDate.now() : dataHoraAtual.toLocalDate());
            ComboBox<String> horaCombo = criarComboTempo(23);
            ComboBox<String> minutoCombo = criarComboTempo(59);
            horaCombo.setValue(formatarTempo(dataHoraAtual == null ? 20 : dataHoraAtual.getHour()));
            minutoCombo.setValue(formatarTempo(dataHoraAtual == null ? 0 : dataHoraAtual.getMinute()));

            ComboBox<String> estadoCombo = new ComboBox<>(FXCollections.observableArrayList("CONFIRMADA", "PENDENTE", "CANCELADA"));
            estadoCombo.setValue(normalizarEstado(ViewUtils.text(atual, "estado"), "CONFIRMADA"));

            ComboBox<JsonNode> mesaCombo = new ComboBox<>(carregarOpcoes("/mesas"));
            configurarCombo(mesaCombo, this::descricaoMesa);
            selecionarPorId(mesaCombo, ViewUtils.nestedText(atual, "numMesa", "id"), node -> ViewUtils.text(node, "id"));

            ComboBox<JsonNode> utilizadorCombo = new ComboBox<>(carregarOpcoes("/utilizadores"));
            configurarCombo(utilizadorCombo, this::descricaoUtilizador);
            selecionarPorId(utilizadorCombo, ViewUtils.nestedText(atual, "idUtilizador", "id"), node -> ViewUtils.text(node, "id"));

            HBox dataHoraRow = new HBox(12,
                    ViewUtils.criarCampoFormulario("Data", dataPicker),
                    ViewUtils.criarCampoFormulario("Hora", horaCombo),
                    ViewUtils.criarCampoFormulario("Minutos", minutoCombo)
            );
            HBox relacoesRow = new HBox(12,
                    ViewUtils.criarCampoFormulario("Mesa", mesaCombo),
                    ViewUtils.criarCampoFormulario("Cliente", utilizadorCombo)
            );
            HBox estadoRow = new HBox(12, ViewUtils.criarCampoFormulario("Estado", estadoCombo));

            dataHoraRow.getStyleClass().add("app-form-row");
            relacoesRow.getStyleClass().add("app-form-row");
            estadoRow.getStyleClass().add("app-form-row");

            VBox form = new VBox(12, dataHoraRow, relacoesRow, estadoRow);
            form.getStyleClass().add("app-form");

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Escolha a data, hora e entidades ligadas a esta reserva.");
            ViewUtils.estilizarBotoesDialogo(dialog);

            Optional<ButtonType> resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
                return Optional.empty();
            }

            validarDataHoraSelecionada(dataPicker.getValue(), horaCombo.getValue(), minutoCombo.getValue(), estadoCombo.getValue());

            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("dataHora", construirDataHoraIso(dataPicker.getValue(), horaCombo.getValue(), minutoCombo.getValue()));
            payload.put("estado", normalizarEstado(estadoCombo.getValue(), "CONFIRMADA"));

            JsonNode mesaSelecionada = mesaCombo.getValue();
            JsonNode utilizadorSelecionado = utilizadorCombo.getValue();
            if (mesaSelecionada == null || utilizadorSelecionado == null) {
                throw new IllegalArgumentException("Mesa e utilizador sao obrigatorios.");
            }

            ObjectNode mesa = payload.putObject("numMesa");
            mesa.put("id", Integer.parseInt(ViewUtils.text(mesaSelecionada, "id")));

            ObjectNode utilizador = payload.putObject("idUtilizador");
            utilizador.put("id", Integer.parseInt(ViewUtils.text(utilizadorSelecionado, "id")));

            return Optional.of(payload);
        } catch (RuntimeException e) {
            ViewUtils.showError("Reservas", e.getMessage());
            return Optional.empty();
        }
    }

    private ObservableList<JsonNode> carregarOpcoes(String path) {
        ArrayNode resposta = DesktopAppContext.apiService().getArray(path);
        ObservableList<JsonNode> opcoes = FXCollections.observableArrayList();
        resposta.forEach(opcoes::add);
        return opcoes;
    }

    private void configurarCombo(ComboBox<JsonNode> combo, Function<JsonNode, String> labelProvider) {
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(JsonNode object) {
                return object == null ? "" : labelProvider.apply(object);
            }

            @Override
            public JsonNode fromString(String string) {
                return null;
            }
        });
        combo.setVisibleRowCount(8);
    }

    private void selecionarPorId(ComboBox<JsonNode> combo,
                                 String idAtual,
                                 Function<JsonNode, String> idExtractor) {
        if (idAtual == null || idAtual.isBlank()) {
            return;
        }

        for (JsonNode node : combo.getItems()) {
            if (idAtual.equals(idExtractor.apply(node))) {
                combo.setValue(node);
                return;
            }
        }
    }

    private ComboBox<String> criarComboTempo(int max) {
        ObservableList<String> valores = FXCollections.observableArrayList();
        for (int i = 0; i <= max; i++) {
            valores.add(formatarTempo(i));
        }
        return new ComboBox<>(valores);
    }

    private String descricaoMesa(JsonNode mesa) {
        String id = ViewUtils.text(mesa, "id");
        String estado = ViewUtils.text(mesa, "estado");
        return "Mesa " + id + " | " + (estado.isBlank() ? "sem estado" : estado);
    }

    private String descricaoUtilizador(JsonNode utilizador) {
        String id = ViewUtils.text(utilizador, "id");
        String nome = ViewUtils.text(utilizador, "nome");
        String email = ViewUtils.text(utilizador, "email");
        return id + " | " + nome + (email.isBlank() ? "" : " | " + email);
    }

    private boolean contem(JsonNode reserva, String campo, String termo) {
        return ViewUtils.text(reserva, campo).toLowerCase(Locale.ROOT).contains(termo);
    }

    private String formatarDataHora(String dataHoraRaw) {
        LocalDateTime dataHora = parseDataHora(dataHoraRaw);
        if (dataHora != null) {
            return DATA_HORA_FORMATTER.format(dataHora);
        }

        String valor = dataHoraRaw == null ? "" : dataHoraRaw.trim();
        return valor.replace("T", " ").replace("Z", "");
    }

    private LocalDateTime parseDataHora(String dataHoraRaw) {
        String valor = dataHoraRaw == null ? "" : dataHoraRaw.trim();
        if (valor.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(Instant.parse(valor), ZoneId.systemDefault());
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(valor);
        } catch (Exception ignored) {
        }

        return null;
    }

    private String construirDataHoraIso(LocalDate data, String hora, String minuto) {
        if (data == null || hora == null || minuto == null) {
            throw new IllegalArgumentException("Data e hora da reserva sao obrigatorias.");
        }

        LocalTime tempo = LocalTime.of(Integer.parseInt(hora), Integer.parseInt(minuto));
        return LocalDateTime.of(data, tempo).atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private void validarDataHoraSelecionada(LocalDate data, String hora, String minuto, String estado) {
        LocalDateTime dataHora = construirDataHora(data, hora, minuto);
        String estadoNormalizado = normalizarEstado(estado, "CONFIRMADA");
        if (dataHora.isBefore(LocalDateTime.now()) && !"CANCELADA".equals(estadoNormalizado)) {
            throw new IllegalArgumentException("Nao pode criar ou alterar reservas para datas passadas.");
        }
    }

    private LocalDateTime construirDataHora(LocalDate data, String hora, String minuto) {
        if (data == null || hora == null || minuto == null) {
            throw new IllegalArgumentException("Data e hora da reserva sao obrigatorias.");
        }
        return LocalDateTime.of(data, LocalTime.of(Integer.parseInt(hora), Integer.parseInt(minuto)));
    }

    private String formatarEstado(String estadoRaw) {
        String estado = normalizarEstado(estadoRaw, "");
        return switch (estado) {
            case "CONFIRMADA" -> "Confirmada";
            case "PENDENTE" -> "Pendente";
            case "CANCELADA" -> "Cancelada";
            case "CONCLUIDA" -> "Concluida";
            default -> estadoRaw == null ? "" : estadoRaw.trim();
        };
    }

    private String classeEstadoReserva(String estadoRaw) {
        String estado = normalizarEstado(estadoRaw, "");
        return switch (estado) {
            case "CONFIRMADA", "CONCLUIDA" -> "estado-sucesso";
            case "PENDENTE" -> "estado-alerta";
            case "CANCELADA" -> "estado-erro";
            default -> "estado-neutro";
        };
    }

    private String normalizarEstado(String estadoRaw, String valorPadrao) {
        String valor = estadoRaw == null || estadoRaw.isBlank() ? valorPadrao : estadoRaw.trim();
        return valor.toUpperCase(Locale.ROOT);
    }

    private String formatarTempo(int valor) {
        return String.format(Locale.ROOT, "%02d", valor);
    }

    private int compararInteiros(String a, String b) {
        return Integer.compare(parseInteiro(a), parseInteiro(b));
    }

    private int compararDatas(String a, String b) {
        LocalDateTime dataA = parseDataHora(a);
        LocalDateTime dataB = parseDataHora(b);
        if (dataA == null && dataB == null) {
            return 0;
        }
        if (dataA == null) {
            return -1;
        }
        if (dataB == null) {
            return 1;
        }
        return dataA.compareTo(dataB);
    }

    private int parseInteiro(String valor) {
        try {
            return Integer.parseInt(valor == null ? "0" : valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ObjectNode criarPayloadReserva(JsonNode reserva, String estado) {
        ObjectNode payload = DesktopAppContext.apiService().createObject();
        payload.put("dataHora", ViewUtils.text(reserva, "dataHora"));
        payload.put("estado", estado);

        String mesaId = ViewUtils.nestedText(reserva, "numMesa", "id");
        if (!mesaId.isBlank()) {
            ObjectNode mesa = payload.putObject("numMesa");
            mesa.put("id", Integer.parseInt(mesaId));
        }

        String utilizadorId = ViewUtils.nestedText(reserva, "idUtilizador", "id");
        if (!utilizadorId.isBlank()) {
            ObjectNode utilizador = payload.putObject("idUtilizador");
            utilizador.put("id", Integer.parseInt(utilizadorId));
        }
        return payload;
    }

    private boolean isReservaCancelada(JsonNode reserva) {
        return "cancelada".equals(normalizarTexto(ViewUtils.text(reserva, "estado")));
    }

    private String normalizarTexto(String texto) {
        String valor = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private final class TableRowCancelado<T extends JsonNode> extends javafx.scene.control.TableRow<T> {
        private final String estadoCancelado;

        private TableRowCancelado(String estadoCancelado) {
            this.estadoCancelado = estadoCancelado;
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("linha-cancelada");

            if (!empty && item != null && estadoCancelado.equalsIgnoreCase(ViewUtils.text(item, "estado"))) {
                getStyleClass().add("linha-cancelada");
            }
        }
    }
}
