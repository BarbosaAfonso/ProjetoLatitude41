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
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public class GestaoStockScreen {

    private static final int NOVO_INGREDIENTE_ID = -1;
    private static final String UNIDADE_PESO = "kg/g";
    private static final String UNIDADE_LITRO = "l";
    private static final String UNIDADE_UNIDADE = "unidade";

    @FXML
    private TableView<JsonNode> tabela;

    @FXML
    private TableColumn<JsonNode, String> colId;

    @FXML
    private TableColumn<JsonNode, String> colIngrediente;

    @FXML
    private TableColumn<JsonNode, String> colQuant;

    @FXML
    private TableColumn<JsonNode, String> colEstado;

    @FXML
    private TableColumn<JsonNode, String> colAcoes;

    @FXML
    private TextField pesquisaField;

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> dadosFiltrados = new FilteredList<>(dados, stock -> true);

    @FXML
    private void initialize() {
        configurarColunas();

        SortedList<JsonNode> dadosOrdenados = new SortedList<>(dadosFiltrados);
        dadosOrdenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(dadosOrdenados);

        carregarDados();
    }

    private void configurarColunas() {
        colId.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "id")));
        colIngrediente.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.nestedText(data.getValue(), "ingrediente", "nome")));
        colQuant.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "quant")));
        colEstado.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "estado")));
        colAcoes.setCellValueFactory(data -> new ReadOnlyStringWrapper("acoes"));
        configurarColunasTextoCentradas();

        colQuant.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "" : formatarQuantidade(item));
                setGraphic(null);
                setAlignment(Pos.CENTER);
            }
        });

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

                String estado = item.trim().toLowerCase(Locale.ROOT);
                boolean disponivel = "disponivel".equals(estado);
                boolean esgotado = "esgotado".equals(estado);
                badge.setText(disponivel ? "Disponivel" : esgotado ? "Esgotado" : "Encomendado");
                badge.getStyleClass().setAll("estado-badge",
                        disponivel ? "estado-sucesso" : esgotado ? "estado-erro" : "estado-alerta");
                setGraphic(wrapper);
            }
        });

        colAcoes.setSortable(false);
        colAcoes.setReorderable(false);
        colAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnConfirmar = new Button("\u2713");
            private final Button btnApagar = new Button("\uD83D\uDDD1");
            private final HBox wrapper = new HBox(8, btnConfirmar, btnApagar);

            {
                wrapper.setAlignment(Pos.CENTER);
                btnConfirmar.getStyleClass().addAll("acao-button", "acao-confirmar");
                btnApagar.getStyleClass().addAll("acao-button", "acao-apagar");

                btnConfirmar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        confirmarEntrega(linha);
                    }
                });

                btnApagar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        apagarStock(linha);
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
                    btnConfirmar.setDisable("disponivel".equalsIgnoreCase(ViewUtils.text(linha, "estado")));
                    setGraphic(wrapper);
                }
            }
        });
    }

    private void configurarColunasTextoCentradas() {
        colId.setCellFactory(column -> criarCelulaTextoCentrada());
        colIngrediente.setCellFactory(column -> criarCelulaTextoCentrada());
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
    private void onNovoStock() {
        onAdicionar();
    }

    @FXML
    private void onPesquisar() {
        String termo = pesquisaField == null ? "" : pesquisaField.getText().trim().toLowerCase(Locale.ROOT);
        dadosFiltrados.setPredicate(stock -> {
        if (termo.isBlank()) {
            return true;
        }

            return contem(stock, "id", termo)
                    || contem(stock, "quant", termo)
                    || contem(stock, "estado", termo)
                    || ViewUtils.nestedText(stock, "ingrediente", "id").toLowerCase(Locale.ROOT).contains(termo)
                    || ViewUtils.nestedText(stock, "ingrediente", "nome").toLowerCase(Locale.ROOT).contains(termo);
        });
    }

    @FXML
    private void onAdicionar() {
        Optional<ObjectNode> payload = dialogoStock(null);
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().post("/stocks", body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Stock", e.getMessage());
            }
        });
    }

    @FXML
    private void onEditar() {
        JsonNode selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            ViewUtils.showWarning("Stock", "Selecione uma encomenda para confirmar.");
            return;
        }

        confirmarEntrega(selecionado);
    }

    @FXML
    private void onApagar() {
        JsonNode selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            ViewUtils.showWarning("Stock", "Selecione um registo de stock para apagar.");
            return;
        }

        apagarStock(selecionado);
    }

    @FXML
    private void onAtualizar() {
        carregarDados();
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void apagarStock(JsonNode selecionado) {
        String id = ViewUtils.text(selecionado, "id");
        if (!ViewUtils.confirm("Stock", "Eliminar o registo do ingrediente ID " + id + "?")) {
            return;
        }

        try {
            boolean apagado = DesktopAppContext.apiService().delete("/stocks/" + id);
            if (!apagado) {
                ViewUtils.showWarning("Stock", "Registo nao encontrado.");
            }
            carregarDados();
        } catch (RuntimeException e) {
            ViewUtils.showError("Stock", e.getMessage());
        }
    }

    private void confirmarEntrega(JsonNode selecionado) {
        if ("disponivel".equalsIgnoreCase(ViewUtils.text(selecionado, "estado"))) {
            ViewUtils.showInfo("Stock", "Este ingrediente ja esta disponivel.");
            return;
        }

        String id = ViewUtils.text(selecionado, "id");
        String ingrediente = ViewUtils.nestedText(selecionado, "ingrediente", "nome");
        if (!ViewUtils.confirm("Stock", "Confirmar rececao de " + ingrediente + " (ID " + id + ")?")) {
            return;
        }

        try {
            ObjectNode payload = criarPayloadStock(selecionado, "disponivel");
            DesktopAppContext.apiService().put("/stocks/" + id, payload);
            carregarDados();
        } catch (RuntimeException e) {
            ViewUtils.showError("Stock", e.getMessage());
        }
    }

    private void carregarDados() {
        try {
            ArrayNode resposta = DesktopAppContext.apiService().getArray("/stocks");
            dados.clear();
            resposta.forEach(dados::add);
        } catch (RuntimeException e) {
            ViewUtils.showError("Stock", e.getMessage());
        }
    }

    private Optional<ObjectNode> dialogoStock(JsonNode atual) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Nova Encomenda ao Fornecedor");

            ComboBox<JsonNode> ingredienteCombo = new ComboBox<>(carregarIngredientes());
            configurarComboIngrediente(ingredienteCombo);

            TextField quantField = new TextField(atual == null ? "" : ViewUtils.text(atual, "quant"));
            quantField.setPromptText("Ex: 18,50");
            TextField novoIngredienteField = new TextField();
            novoIngredienteField.setPromptText("Nome do novo produto");
            novoIngredienteField.setDisable(true);
            ComboBox<String> unidadeCombo = new ComboBox<>(FXCollections.observableArrayList(
                    UNIDADE_PESO,
                    UNIDADE_LITRO,
                    UNIDADE_UNIDADE
            ));
            unidadeCombo.setDisable(true);

            ingredienteCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
                boolean novoSelecionado = isNovoIngrediente(newValue);
                novoIngredienteField.setDisable(!novoSelecionado);
                unidadeCombo.setDisable(!novoSelecionado);
                if (!novoSelecionado) {
                    novoIngredienteField.clear();
                    unidadeCombo.setValue(normalizarUnidadeUi(ViewUtils.text(newValue, "unidade")));
                } else if (unidadeCombo.getValue() == null) {
                    unidadeCombo.setValue(UNIDADE_UNIDADE);
                }
            });

            HBox linhaPrincipal = new HBox(12,
                    ViewUtils.criarCampoFormulario("Ingrediente", ingredienteCombo),
                    ViewUtils.criarCampoFormulario("Quantidade", quantField)
            );
            HBox linhaComplementar = new HBox(12,
                    ViewUtils.criarCampoFormulario("Novo Produto", novoIngredienteField),
                    ViewUtils.criarCampoFormulario("Unidade", unidadeCombo)
            );
            linhaPrincipal.getStyleClass().add("app-form-row");
            linhaComplementar.getStyleClass().add("app-form-row");

            VBox form = new VBox(12, linhaPrincipal, linhaComplementar);
            form.getStyleClass().add("app-form");

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Simule o envio de uma encomenda ao fornecedor e registe o ingrediente.");
            ViewUtils.estilizarBotoesDialogo(dialog, "Enviar Pedido", "Cancelar");

            Optional<ButtonType> resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
                return Optional.empty();
            }

            JsonNode ingredienteSelecionado = ingredienteCombo.getValue();
            if (ingredienteSelecionado == null) {
                throw new IllegalArgumentException("Selecione um ingrediente valido.");
            }

            if (isNovoIngrediente(ingredienteSelecionado)) {
                ingredienteSelecionado = criarNovoIngrediente(novoIngredienteField.getText(), unidadeCombo.getValue());
            }

            int ingredienteId = Integer.parseInt(ViewUtils.text(ingredienteSelecionado, "id"));

            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("id", ingredienteId);
            payload.put("quant", new BigDecimal(normalizarNumero(quantField.getText())));
            payload.put("estado", "encomendado");

            ObjectNode ingrediente = payload.putObject("ingrediente");
            ingrediente.put("id", ingredienteId);
            return Optional.of(payload);
        } catch (RuntimeException e) {
            ViewUtils.showError("Stock", e.getMessage());
            return Optional.empty();
        }
    }

    private ObservableList<JsonNode> carregarIngredientes() {
        ArrayNode resposta = DesktopAppContext.apiService().getArray("/ingredientes");
        ObservableList<JsonNode> ingredientes = FXCollections.observableArrayList();
        resposta.forEach(ingredientes::add);
        ingredientes.add(criarOpcaoNovoIngrediente());
        return ingredientes;
    }

    private void configurarComboIngrediente(ComboBox<JsonNode> ingredienteCombo) {
        ingredienteCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(JsonNode object) {
                return object == null ? "" : descricaoIngrediente(object);
            }

            @Override
            public JsonNode fromString(String string) {
                return null;
            }
        });
        ingredienteCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JsonNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : descricaoIngrediente(item));
            }
        });
        ingredienteCombo.setCellFactory(listView -> new ListCell<>() {
            private final Button remover = new Button("X");
            private final HBox wrapper = new HBox(8);
            private final Label label = new Label();

            {
                remover.getStyleClass().addAll("acao-button", "acao-apagar");
                remover.setOnAction(event -> {
                    JsonNode item = getItem();
                    event.consume();
                    if (item != null && !isNovoIngrediente(item)) {
                        removerIngrediente(ingredienteCombo, item);
                    }
                });
                wrapper.setAlignment(Pos.CENTER_LEFT);
                wrapper.getChildren().addAll(remover, label);
            }

            @Override
            protected void updateItem(JsonNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                label.setText(descricaoIngrediente(item));
                remover.setVisible(!isNovoIngrediente(item));
                remover.setManaged(!isNovoIngrediente(item));
                setGraphic(wrapper);
                setText(null);
            }
        });
        ingredienteCombo.setVisibleRowCount(10);
    }

    private void selecionarIngredienteAtual(ComboBox<JsonNode> combo, String ingredienteId) {
        if (ingredienteId == null || ingredienteId.isBlank()) {
            return;
        }

        for (JsonNode ingrediente : combo.getItems()) {
            if (ingredienteId.equals(ViewUtils.text(ingrediente, "id"))) {
                combo.setValue(ingrediente);
                return;
            }
        }
    }

    private String descricaoIngrediente(JsonNode ingrediente) {
        if (isNovoIngrediente(ingrediente)) {
            return "(+ novo produto)";
        }
        String id = ViewUtils.text(ingrediente, "id");
        String nome = ViewUtils.text(ingrediente, "nome");
        String unidade = ViewUtils.text(ingrediente, "unidade");
        return id + " | " + nome + (unidade.isBlank() ? "" : " | " + unidade);
    }

    private void removerIngrediente(ComboBox<JsonNode> combo, JsonNode ingrediente) {
        String nome = ViewUtils.text(ingrediente, "nome");
        if (!ViewUtils.confirm("Stock", "Remover " + nome + " da lista de ingredientes?")) {
            return;
        }

        try {
            DesktopAppContext.apiService().delete("/stocks/" + ViewUtils.text(ingrediente, "id"));
            DesktopAppContext.apiService().delete("/ingredientes/" + ViewUtils.text(ingrediente, "id"));
            combo.setItems(carregarIngredientes());
            combo.getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            ViewUtils.showError("Stock", e.getMessage());
        }
    }

    private JsonNode criarNovoIngrediente(String nome, String unidadeSelecionada) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Indique o nome do novo produto.");
        }
        if (unidadeSelecionada == null || unidadeSelecionada.isBlank()) {
            throw new IllegalArgumentException("Selecione a unidade do novo ingrediente.");
        }

        ObjectNode payload = DesktopAppContext.apiService().createObject();
        payload.put("nome", nome.trim());
        payload.put("unidade", normalizarUnidadeApi(unidadeSelecionada));
        payload.put("preco", BigDecimal.ZERO);
        return DesktopAppContext.apiService().post("/ingredientes", payload);
    }

    private ObjectNode criarOpcaoNovoIngrediente() {
        ObjectNode node = DesktopAppContext.apiService().createObject();
        node.put("id", NOVO_INGREDIENTE_ID);
        node.put("nome", "(+ novo produto)");
        return node;
    }

    private boolean isNovoIngrediente(JsonNode ingrediente) {
        return ingrediente != null && ingrediente.path("id").asInt(Integer.MIN_VALUE) == NOVO_INGREDIENTE_ID;
    }

    private ObjectNode criarPayloadStock(JsonNode stock, String estado) {
        ObjectNode payload = DesktopAppContext.apiService().createObject();
        String id = ViewUtils.text(stock, "id");
        payload.put("id", Integer.parseInt(id));
        payload.put("quant", new BigDecimal(normalizarNumero(ViewUtils.text(stock, "quant"))));
        payload.put("estado", estado);

        ObjectNode ingrediente = payload.putObject("ingrediente");
        ingrediente.put("id", Integer.parseInt(id));
        return payload;
    }

    private boolean contem(JsonNode stock, String campo, String termo) {
        return ViewUtils.text(stock, campo).toLowerCase(Locale.ROOT).contains(termo);
    }

    private String formatarQuantidade(String quantidadeRaw) {
        try {
            BigDecimal valor = new BigDecimal(normalizarNumero(quantidadeRaw));
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "PT"));
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
            return format.format(valor);
        } catch (Exception e) {
            return quantidadeRaw;
        }
    }

    private String normalizarNumero(String texto) {
        return texto == null ? "0" : texto.replace(" ", "").replace(",", ".").trim();
    }

    private String normalizarUnidadeUi(String unidadeApi) {
        String valor = unidadeApi == null ? "" : unidadeApi.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case "KG/G", "KG", "G" -> UNIDADE_PESO;
            case "L", "LITRO", "LITROS" -> UNIDADE_LITRO;
            case "UN", "UNIDADE", "UNIDADES" -> UNIDADE_UNIDADE;
            default -> UNIDADE_UNIDADE;
        };
    }

    private String normalizarUnidadeApi(String unidadeUi) {
        String valor = unidadeUi == null ? "" : unidadeUi.trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case UNIDADE_PESO -> "KG/G";
            case UNIDADE_LITRO -> "L";
            case UNIDADE_UNIDADE -> "UNIDADE";
            default -> throw new IllegalArgumentException("Unidade invalida.");
        };
    }
}
