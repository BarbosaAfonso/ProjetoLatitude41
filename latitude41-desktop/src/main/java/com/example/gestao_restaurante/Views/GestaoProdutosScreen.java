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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class GestaoProdutosScreen {

    @FXML
    private TableView<JsonNode> tabela;

    @FXML
    private TableColumn<JsonNode, String> colId;

    @FXML
    private TableColumn<JsonNode, String> colNome;

    @FXML
    private TableColumn<JsonNode, String> colTipo;

    @FXML
    private TableColumn<JsonNode, String> colPreco;

    @FXML
    private TableColumn<JsonNode, String> colDisponivel;

    @FXML
    private TableColumn<JsonNode, String> colAcoes;

    @FXML
    private TextField pesquisaField;

    @FXML
    private FlowPane filtrosCategoriaBox;

    @FXML
    private ComboBox<String> ordenarPorCombo;

    @FXML
    private ComboBox<String> direcaoOrdenacaoCombo;

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> dadosFiltrados = new FilteredList<>(dados, produto -> true);
    private String filtroCategoriaAtivo = "todos";

    @FXML
    private void initialize() {
        configurarColunas();
        configurarFiltrosCategoria();
        configurarOrdenacao();

        SortedList<JsonNode> dadosOrdenados = new SortedList<>(dadosFiltrados);
        dadosOrdenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(dadosOrdenados);

        if (pesquisaField != null) {
            pesquisaField.textProperty().addListener((obs, oldValue, newValue) -> atualizarFiltros());
        }

        carregarDados();
        aplicarOrdenacao();
    }

    private void configurarColunas() {
        colId.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "id")));
        colNome.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "nome")));
        colTipo.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "tipo")));
        colPreco.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "preco")));
        colDisponivel.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "disponivel")));
        colAcoes.setCellValueFactory(data -> new ReadOnlyStringWrapper("acoes"));
        configurarColunasTextoCentradas();

        colPreco.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "" : formatarPreco(item));
                setGraphic(null);
                setAlignment(Pos.CENTER);
            }
        });
        colId.setComparator(this::compararInteiros);
        colPreco.setComparator(this::compararPrecos);

        colDisponivel.setCellFactory(column -> new TableCell<>() {
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

                boolean disponivel = isDisponivel(item);
                badge.setText(disponivel ? "Disponivel" : "Indisponivel");
                badge.getStyleClass().setAll(
                        "estado-badge",
                        disponivel ? "estado-disponivel" : "estado-indisponivel"
                );
                setGraphic(wrapper);
            }
        });

        colAcoes.setSortable(false);
        colAcoes.setReorderable(false);
        colAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnEditar = new Button("\u270E");
            private final Button btnApagar = new Button("\uD83D\uDDD1");
            private final HBox wrapper = new HBox(8, btnEditar, btnApagar);

            {
                wrapper.setAlignment(Pos.CENTER);
                btnEditar.getStyleClass().addAll("acao-button", "acao-editar");
                btnApagar.getStyleClass().addAll("acao-button", "acao-apagar");

                btnEditar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        editarProduto(linha);
                    }
                });

                btnApagar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        apagarProduto(linha);
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
                    setGraphic(wrapper);
                }
            }
        });
    }

    private void configurarColunasTextoCentradas() {
        colId.setCellFactory(column -> criarCelulaTextoCentrada());
        colNome.setCellFactory(column -> criarCelulaTextoCentrada());
        colTipo.setCellFactory(column -> criarCelulaTextoCentrada());
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
    private void onNovoProduto() {
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
    private void onAdicionar() {
        Optional<ObjectNode> payload = dialogoProduto(null);
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().post("/produtos", body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Produtos", e.getMessage());
            }
        });
    }

    @FXML
    private void onEditar() {
        JsonNode selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            ViewUtils.showWarning("Produtos", "Selecione um produto para editar.");
            return;
        }

        editarProduto(selecionado);
    }

    @FXML
    private void onApagar() {
        JsonNode selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            ViewUtils.showWarning("Produtos", "Selecione um produto para apagar.");
            return;
        }

        apagarProduto(selecionado);
    }

    @FXML
    private void onAtualizar() {
        carregarDados();
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void editarProduto(JsonNode selecionado) {
        Optional<ObjectNode> payload = dialogoProduto(selecionado);
        payload.ifPresent(body -> {
            try {
                String id = ViewUtils.text(selecionado, "id");
                DesktopAppContext.apiService().put("/produtos/" + id, body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Produtos", e.getMessage());
            }
        });
    }

    private void apagarProduto(JsonNode selecionado) {
        String id = ViewUtils.text(selecionado, "id");
        if (!ViewUtils.confirm("Produtos", "Apagar produto ID " + id + "?")) {
            return;
        }

        try {
            boolean apagado = DesktopAppContext.apiService().delete("/produtos/" + id);
            if (!apagado) {
                ViewUtils.showWarning("Produtos", "Produto nao encontrado.");
            }
            carregarDados();
        } catch (RuntimeException e) {
            ViewUtils.showError("Produtos", e.getMessage());
        }
    }

    private void carregarDados() {
        try {
            ArrayNode resposta = DesktopAppContext.apiService().getArray("/produtos");
            dados.clear();
            resposta.forEach(dados::add);
            configurarFiltrosCategoria();
            atualizarFiltros();
            aplicarOrdenacao();
        } catch (RuntimeException e) {
            ViewUtils.showError("Produtos", e.getMessage());
        }
    }

    private void configurarFiltrosCategoria() {
        if (filtrosCategoriaBox == null) {
            return;
        }

        filtrosCategoriaBox.getChildren().clear();
        List<String> filtros = StreamSupport.stream(dados.spliterator(), false)
                .map(produto -> ViewUtils.text(produto, "tipo").trim())
                .filter(tipo -> !tipo.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        adicionarBotaoFiltro("Todos");
        for (String filtro : filtros) {
            adicionarBotaoFiltro(filtro);
        }

        atualizarEstiloFiltrosCategoria();
    }

    private void adicionarBotaoFiltro(String filtro) {
        Button button = new Button(filtro);
        button.getStyleClass().add("filtro-chip");
        button.setOnAction(event -> selecionarFiltroCategoria(filtro));
        filtrosCategoriaBox.getChildren().add(button);
    }

    private void selecionarFiltroCategoria(String filtro) {
        filtroCategoriaAtivo = normalizarTexto(filtro);
        atualizarEstiloFiltrosCategoria();
        atualizarFiltros();
    }

    private void atualizarEstiloFiltrosCategoria() {
        if (filtrosCategoriaBox == null) {
            return;
        }

        filtrosCategoriaBox.getChildren().forEach(node -> {
            if (!(node instanceof Button button)) {
                return;
            }

            String valor = normalizarTexto(button.getText());
            if (valor.equals(filtroCategoriaAtivo)) {
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

        ordenarPorCombo.setItems(FXCollections.observableArrayList("ID", "Nome", "Categoria", "Preco", "Disponibilidade"));
        direcaoOrdenacaoCombo.setItems(FXCollections.observableArrayList("Ascendente", "Descendente"));
        ordenarPorCombo.setValue("Nome");
        direcaoOrdenacaoCombo.setValue("Ascendente");
    }

    private void atualizarFiltros() {
        String termo = pesquisaField == null ? "" : pesquisaField.getText().trim().toLowerCase(Locale.ROOT);
        dadosFiltrados.setPredicate(produto -> correspondeCategoria(produto) && correspondePesquisa(produto, termo));
    }

    private boolean correspondePesquisa(JsonNode produto, String termo) {
        if (termo.isBlank()) {
            return true;
        }

        return contem(produto, "id", termo)
                || contem(produto, "nome", termo)
                || contem(produto, "tipo", termo)
                || contem(produto, "preco", termo)
                || contem(produto, "disponivel", termo);
    }

    private boolean correspondeCategoria(JsonNode produto) {
        if ("todos".equals(filtroCategoriaAtivo)) {
            return true;
        }

        String tipo = normalizarTexto(ViewUtils.text(produto, "tipo"));
        return tipo.equals(filtroCategoriaAtivo);
    }

    private void aplicarOrdenacao() {
        if (tabela == null || ordenarPorCombo == null || direcaoOrdenacaoCombo == null) {
            return;
        }

        TableColumn<JsonNode, ?> coluna = switch (ordenarPorCombo.getValue()) {
            case "ID" -> colId;
            case "Categoria" -> colTipo;
            case "Preco" -> colPreco;
            case "Disponibilidade" -> colDisponivel;
            case "Nome" -> colNome;
            default -> colNome;
        };

        coluna.setSortType("Descendente".equals(direcaoOrdenacaoCombo.getValue())
                ? TableColumn.SortType.DESCENDING
                : TableColumn.SortType.ASCENDING);
        tabela.getSortOrder().setAll(coluna);
    }

    private Optional<ObjectNode> dialogoProduto(JsonNode atual) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(atual == null ? "Adicionar Produto" : "Editar Produto");

        TextField nomeField = new TextField(atual == null ? "" : ViewUtils.text(atual, "nome"));
        TextField tipoField = new TextField(atual == null ? "" : ViewUtils.text(atual, "tipo"));
        TextField precoField = new TextField(atual == null ? "" : ViewUtils.text(atual, "preco"));

        ComboBox<String> disponivelCombo = new ComboBox<>(FXCollections.observableArrayList("Disponivel", "Indisponivel"));
        disponivelCombo.setValue(isDisponivel(atual == null ? "true" : ViewUtils.text(atual, "disponivel"))
                ? "Disponivel"
                : "Indisponivel");

        nomeField.setPromptText("Ex: Bacalhau com natas");
        tipoField.setPromptText("Ex: Prato principal");
        precoField.setPromptText("Ex: 12,50");

        HBox primeiraLinha = new HBox(12,
                ViewUtils.criarCampoFormulario("Nome do produto", nomeField),
                ViewUtils.criarCampoFormulario("Tipo", tipoField)
        );
        HBox segundaLinha = new HBox(12,
                ViewUtils.criarCampoFormulario("Preco", precoField),
                ViewUtils.criarCampoFormulario("Disponibilidade", disponivelCombo)
        );

        primeiraLinha.getStyleClass().add("app-form-row");
        segundaLinha.getStyleClass().add("app-form-row");

        VBox form = new VBox(12, primeiraLinha, segundaLinha);
        form.getStyleClass().add("app-form");

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Preencha os dados principais do produto.");
        ViewUtils.estilizarBotoesDialogo(dialog);

        Optional<ButtonType> resultado = dialog.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return Optional.empty();
        }

        try {
            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("nome", nomeField.getText().trim());
            payload.put("tipo", tipoField.getText().trim());
            payload.put("preco", new BigDecimal(normalizarNumero(precoField.getText())));
            payload.put("disponivel", "Disponivel".equals(disponivelCombo.getValue()));
            return Optional.of(payload);
        } catch (Exception e) {
            ViewUtils.showError("Produtos", "Dados invalidos. Verifique os campos.");
            return Optional.empty();
        }
    }

    private boolean contem(JsonNode produto, String campo, String termo) {
        return ViewUtils.text(produto, campo).toLowerCase(Locale.ROOT).contains(termo);
    }

    private boolean isDisponivel(String valor) {
        String texto = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        return "true".equals(texto)
                || "1".equals(texto)
                || "sim".equals(texto)
                || "disponivel".equals(texto);
    }

    private String formatarPreco(String precoRaw) {
        try {
            BigDecimal valor = new BigDecimal(normalizarNumero(precoRaw));
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "PT"));
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
            return format.format(valor) + " EUR";
        } catch (Exception e) {
            return precoRaw;
        }
    }

    private String normalizarNumero(String texto) {
        return texto == null
                ? "0"
                : texto.replace("EUR", "").replace(" ", "").replace(",", ".").trim();
    }

    private int compararInteiros(String a, String b) {
        return Integer.compare(parseInteiro(a), parseInteiro(b));
    }

    private int compararPrecos(String a, String b) {
        return new BigDecimal(normalizarNumero(a)).compareTo(new BigDecimal(normalizarNumero(b)));
    }

    private int parseInteiro(String valor) {
        try {
            return Integer.parseInt(valor == null ? "0" : valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String normalizarTexto(String texto) {
        String valor = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
