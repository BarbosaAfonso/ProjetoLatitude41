package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GestaoPedidosScreen {

    private static final BigDecimal TAXA_IVA = new BigDecimal("0.23");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MOEDA_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    @FXML
    private Label sessaoLabel;

    @FXML
    private Label utilizadorLabel;

    @FXML
    private Label cargoLabel;

    @FXML
    private Label avatarLabel;

    @FXML
    private TextField pesquisaField;

    @FXML
    private ComboBox<JsonNode> reservaCombo;

    @FXML
    private FlowPane filtrosTipoBox;

    @FXML
    private TilePane produtosGrid;

    @FXML
    private Label categoriaAtivaLabel;

    @FXML
    private Label resultadosLabel;

    @FXML
    private Label mesaLabel;

    @FXML
    private Label reservaLabel;

    @FXML
    private Label clienteLabel;

    @FXML
    private Label estadoPedidoLabel;

    @FXML
    private Label itensResumoLabel;

    @FXML
    private VBox itensPedidoBox;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label ivaLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Button limparPedidoButton;

    @FXML
    private Button confirmarPedidoButton;

    private final ObservableList<JsonNode> mesasLancamento = FXCollections.observableArrayList();
    private final ObservableList<JsonNode> produtos = FXCollections.observableArrayList();
    private final Map<Integer, LinhaRascunho> linhasPedido = new LinkedHashMap<>();

    private String filtroTipoAtivo = "TODOS";

    @FXML
    private void initialize() {
        ViewUtils.preencherCabecalho(sessaoLabel, utilizadorLabel, cargoLabel, avatarLabel,
                "Lancamento de pedidos com produtos ligados por linha de pedido");
        configurarComboReserva();
        configurarPesquisa();
        carregarMesasDisponiveis();
        carregarProdutos();
        atualizarResumoReserva();
        atualizarResumoPedido();
    }

    @FXML
    private void onPesquisar() {
        renderizarProdutos();
    }

    @FXML
    private void onReservaAlterada() {
        atualizarResumoReserva();
        atualizarEstadoAcoes();
    }

    @FXML
    private void onLimparPedido() {
        if (linhasPedido.isEmpty()) {
            return;
        }
        if (!ViewUtils.confirm("Pedidos", "Pretende limpar todos os produtos do pedido atual?")) {
            return;
        }
        linhasPedido.clear();
        atualizarResumoPedido();
    }

    @FXML
    private void onConfirmarPedido() {
        try {
            JsonNode mesaSelecionada = reservaCombo == null ? null : reservaCombo.getValue();
            if (mesaSelecionada == null) {
                throw new IllegalArgumentException("Selecione uma mesa antes de confirmar o pedido.");
            }
            if (linhasPedido.isEmpty()) {
                throw new IllegalArgumentException("Adicione pelo menos um produto ao pedido.");
            }

            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("dataHora", Instant.now().toString());
            payload.put("estado", "PREPARACAO");
            payload.put("mesaId", inteiro(ViewUtils.text(mesaSelecionada, "mesaId")));
            if (!ViewUtils.text(mesaSelecionada, "reservaId").isBlank()) {
                payload.put("reservaId", inteiro(ViewUtils.text(mesaSelecionada, "reservaId")));
            }
            if (DesktopAppContext.utilizadorId() != null) {
                payload.put("utilizadorId", DesktopAppContext.utilizadorId());
            }

            ArrayNode linhas = payload.putArray("linhas");
            for (LinhaRascunho linha : linhasPedido.values()) {
                ObjectNode linhaPayload = linhas.addObject();
                linhaPayload.put("produtoId", linha.produtoId());
                linhaPayload.put("quantidade", linha.quantidade());
                linhaPayload.put("precoUnitVenda", linha.preco().doubleValue());
                linhaPayload.put("observacoes", linha.observacoes());
            }

            JsonNode resposta = DesktopAppContext.apiService().post("/pedidos/completo", payload);
            mostrarDialogoSucesso(resposta, mesaSelecionada);
            linhasPedido.clear();
            atualizarResumoPedido();
        } catch (RuntimeException e) {
            ViewUtils.showError("Pedidos", e.getMessage());
        }
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void configurarComboReserva() {
        if (reservaCombo == null) {
            return;
        }

        reservaCombo.setItems(mesasLancamento);
        reservaCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(JsonNode object) {
                return object == null ? "" : descricaoMesaLancamento(object);
            }

            @Override
            public JsonNode fromString(String string) {
                return null;
            }
        });
        reservaCombo.setVisibleRowCount(8);
    }

    private void configurarPesquisa() {
        if (pesquisaField != null) {
            pesquisaField.textProperty().addListener((obs, oldValue, newValue) -> renderizarProdutos());
        }
    }

    private void carregarMesasDisponiveis() {
        try {
            mesasLancamento.clear();

            ArrayNode reservasApi = DesktopAppContext.apiService().getArray("/reservas");
            Map<Integer, JsonNode> reservaAtivaPorMesa = selecionarReservaAtivaPorMesa(reservasApi);

            List<JsonNode> mesasFiltradas = new ArrayList<>();
            for (JsonNode mesa : DesktopAppContext.apiService().getArray("/mesas")) {
                String estado = normalizarEstadoMesa(ViewUtils.text(mesa, "estado"));
                if (!("RESERVADA".equals(estado) || "OCUPADA".equals(estado))) {
                    continue;
                }

                int mesaId = inteiro(ViewUtils.text(mesa, "id"));
                ObjectNode item = DesktopAppContext.apiService().createObject();
                item.put("mesaId", mesaId);
                item.put("mesaEstado", estado);

                JsonNode reservaAssociada = reservaAtivaPorMesa.get(mesaId);
                if (reservaAssociada != null) {
                    item.put("reservaId", inteiro(ViewUtils.text(reservaAssociada, "id")));
                    item.put("reservaDataHora", ViewUtils.text(reservaAssociada, "dataHora"));
                    String clienteNome = ViewUtils.nestedText(reservaAssociada, "idUtilizador", "nome");
                    if (!clienteNome.isBlank()) {
                        item.put("clienteNome", clienteNome);
                    }
                }
                mesasFiltradas.add(item);
            }

            mesasFiltradas.sort(Comparator.comparing(mesa -> inteiro(ViewUtils.text(mesa, "mesaId"))));
            mesasFiltradas.forEach(mesasLancamento::add);

            if (reservaCombo != null && !mesasLancamento.isEmpty()) {
                reservaCombo.setValue(mesasLancamento.get(0));
            }
        } catch (RuntimeException e) {
            ViewUtils.showError("Pedidos", e.getMessage());
        }
    }

    private void carregarProdutos() {
        try {
            produtos.clear();
            DesktopAppContext.apiService().getArray("/produtos").forEach(produtos::add);
            produtos.sort(Comparator.comparing(produto -> ViewUtils.text(produto, "nome"), String.CASE_INSENSITIVE_ORDER));
            renderizarFiltrosTipo();
            renderizarProdutos();
        } catch (RuntimeException e) {
            ViewUtils.showError("Pedidos", e.getMessage());
        }
    }

    private void renderizarFiltrosTipo() {
        if (filtrosTipoBox == null) {
            return;
        }

        filtrosTipoBox.getChildren().clear();
        List<String> tipos = produtos.stream()
                .map(produto -> categoriaProduto(ViewUtils.text(produto, "tipo")))
                .distinct()
                .toList();

        criarBotaoFiltro("Todos", "TODOS");
        for (String tipo : tipos) {
            criarBotaoFiltro(tipo, tipo.toUpperCase(Locale.ROOT));
        }
        atualizarEstiloFiltros();
    }

    private void criarBotaoFiltro(String texto, String valor) {
        Button button = new Button(texto);
        button.getStyleClass().add("pedido-filtro-chip");
        button.setOnAction(event -> {
            filtroTipoAtivo = valor;
            atualizarEstiloFiltros();
            renderizarProdutos();
        });
        filtrosTipoBox.getChildren().add(button);
    }

    private void atualizarEstiloFiltros() {
        if (filtrosTipoBox == null) {
            return;
        }

        for (Node node : filtrosTipoBox.getChildren()) {
            if (!(node instanceof Button button)) {
                continue;
            }
            String valor = button.getText().equalsIgnoreCase("Todos")
                    ? "TODOS"
                    : button.getText().toUpperCase(Locale.ROOT);
            button.getStyleClass().remove("pedido-filtro-chip-activo");
            if (valor.equalsIgnoreCase(filtroTipoAtivo)) {
                button.getStyleClass().add("pedido-filtro-chip-activo");
            }
        }
    }

    private void renderizarProdutos() {
        if (produtosGrid == null) {
            return;
        }

        List<JsonNode> filtrados = produtos.stream()
                .filter(this::produtoDisponivel)
                .filter(this::correspondeTipo)
                .filter(this::correspondePesquisa)
                .collect(Collectors.toList());

        produtosGrid.getChildren().clear();
        filtrados.forEach(produto -> produtosGrid.getChildren().add(criarCardProduto(produto)));

        if (categoriaAtivaLabel != null) {
            categoriaAtivaLabel.setText("Categoria: " + ("TODOS".equals(filtroTipoAtivo) ? "Todos os produtos" : categoriaProduto(filtroTipoAtivo)));
        }
        if (resultadosLabel != null) {
            resultadosLabel.setText(filtrados.size() + " produtos disponiveis");
        }
    }

    private VBox criarCardProduto(JsonNode produto) {
        VBox card = new VBox(12);
        card.getStyleClass().add("pedido-produto-card");

        StackPane imagem = new StackPane();
        imagem.getStyleClass().add("pedido-produto-imagem");
        imagem.setMinHeight(170);

        VBox placeholder = new VBox(6);
        placeholder.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDDBC");
        icon.getStyleClass().add("pedido-produto-imagem-icone");
        Label text = new Label("Imagem");
        text.getStyleClass().add("pedido-produto-imagem-texto");
        placeholder.getChildren().addAll(icon, text);
        imagem.getChildren().add(placeholder);

        Label badge = new Label(categoriaProduto(ViewUtils.text(produto, "tipo")));
        badge.getStyleClass().add("pedido-produto-badge");

        Label preco = new Label(formatarMoeda(decimal(ViewUtils.text(produto, "preco"))));
        preco.getStyleClass().add("pedido-produto-preco");

        HBox topoInfo = new HBox(badge, criarSpacer(), preco);
        topoInfo.setAlignment(Pos.CENTER_LEFT);

        Label nome = new Label(ViewUtils.text(produto, "nome"));
        nome.getStyleClass().add("pedido-produto-nome");
        nome.setWrapText(true);

        Label descricao = new Label("Categoria " + categoriaProduto(ViewUtils.text(produto, "tipo")) + " pronta a ser associada ao pedido.");
        descricao.getStyleClass().add("pedido-produto-descricao");
        descricao.setWrapText(true);

        Button adicionar = new Button("Adicionar ao Pedido");
        adicionar.getStyleClass().add("pedido-produto-acao");
        adicionar.setMaxWidth(Double.MAX_VALUE);
        adicionar.setOnAction(event -> adicionarProduto(produto));

        card.getChildren().addAll(imagem, topoInfo, nome, descricao, adicionar);
        VBox.setVgrow(descricao, Priority.ALWAYS);
        return card;
    }

    private void adicionarProduto(JsonNode produto) {
        int produtoId = inteiro(ViewUtils.text(produto, "id"));
        LinhaRascunho linha = linhasPedido.get(produtoId);
        if (linha == null) {
            linhasPedido.put(produtoId, new LinhaRascunho(
                    produtoId,
                    ViewUtils.text(produto, "nome"),
                    categoriaProduto(ViewUtils.text(produto, "tipo")),
                    decimal(ViewUtils.text(produto, "preco")),
                    1,
                    ""
            ));
        } else {
            linhasPedido.put(produtoId, linha.comQuantidade(linha.quantidade() + 1));
        }
        atualizarResumoPedido();
    }

    private void atualizarResumoReserva() {
        JsonNode mesaSelecionada = reservaCombo == null ? null : reservaCombo.getValue();
        String mesaId = mesaSelecionada == null ? "" : ViewUtils.text(mesaSelecionada, "mesaId");
        String reservaId = mesaSelecionada == null ? "" : ViewUtils.text(mesaSelecionada, "reservaId");
        String clienteNome = mesaSelecionada == null ? "" : ViewUtils.text(mesaSelecionada, "clienteNome");
        String estadoMesa = mesaSelecionada == null ? "" : ViewUtils.text(mesaSelecionada, "mesaEstado");

        if (mesaLabel != null) {
            mesaLabel.setText(mesaId.isBlank() ? "Mesa -" : "Mesa " + mesaId);
        }
        if (reservaLabel != null) {
            reservaLabel.setText(reservaId.isBlank() ? "Sem reserva associada" : "Reserva #" + reservaId);
        }
        if (clienteLabel != null) {
            clienteLabel.setText(clienteNome.isBlank() ? "Sem cliente associado" : clienteNome);
        }
        if (estadoPedidoLabel != null) {
            estadoPedidoLabel.setText(estadoMesa.isBlank()
                    ? "Estado inicial: Em Preparacao"
                    : "Mesa: " + formatarEstadoMesa(estadoMesa) + " | Estado inicial: Em Preparacao");
        }
    }

    private void atualizarResumoPedido() {
        if (itensPedidoBox != null) {
            itensPedidoBox.getChildren().clear();
            if (linhasPedido.isEmpty()) {
                Label vazio = new Label("Ainda nao existem produtos neste pedido. Selecione itens no catalogo para montar o envio para a cozinha.");
                vazio.getStyleClass().add("pedido-vazio-label");
                vazio.setWrapText(true);
                itensPedidoBox.getChildren().add(vazio);
            } else {
                linhasPedido.values().forEach(linha -> itensPedidoBox.getChildren().add(criarLinhaResumo(linha)));
            }
        }

        BigDecimal subtotal = calcularSubtotal();
        BigDecimal iva = subtotal.multiply(TAXA_IVA);
        BigDecimal total = subtotal.add(iva);
        int quantidadeItens = linhasPedido.values().stream().mapToInt(LinhaRascunho::quantidade).sum();

        if (itensResumoLabel != null) {
            itensResumoLabel.setText(quantidadeItens + " itens no pedido");
        }
        if (subtotalLabel != null) {
            subtotalLabel.setText(formatarMoeda(subtotal));
        }
        if (ivaLabel != null) {
            ivaLabel.setText(formatarMoeda(iva));
        }
        if (totalLabel != null) {
            totalLabel.setText(formatarMoeda(total));
        }
        atualizarEstadoAcoes();
    }

    private VBox criarLinhaResumo(LinhaRascunho linha) {
        VBox card = new VBox(10);
        card.getStyleClass().add("pedido-resumo-item");

        Label nome = new Label(linha.nome());
        nome.getStyleClass().add("pedido-resumo-item-nome");

        Label subtotal = new Label(formatarMoeda(linha.subtotal()));
        subtotal.getStyleClass().add("pedido-resumo-item-preco");

        HBox topo = new HBox(nome, criarSpacer(), subtotal);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label meta = new Label(linha.tipo() + " | " + formatarMoeda(linha.preco()) + " un.");
        meta.getStyleClass().add("pedido-resumo-item-meta");

        HBox acoes = new HBox(8);
        acoes.setAlignment(Pos.CENTER_LEFT);

        Button menos = new Button("-");
        menos.getStyleClass().addAll("pedido-quantidade-botao", "pedido-quantidade-menos");
        menos.setOnAction(event -> ajustarQuantidade(linha.produtoId(), -1));

        Label quantidade = new Label(String.valueOf(linha.quantidade()));
        quantidade.getStyleClass().add("pedido-quantidade-label");

        Button mais = new Button("+");
        mais.getStyleClass().addAll("pedido-quantidade-botao", "pedido-quantidade-mais");
        mais.setOnAction(event -> ajustarQuantidade(linha.produtoId(), 1));

        Button notas = new Button("Notas");
        notas.getStyleClass().add("pedido-item-link");
        notas.setOnAction(event -> editarObservacoes(linha));

        Button remover = new Button("Remover");
        remover.getStyleClass().add("pedido-item-link");
        remover.setOnAction(event -> removerLinha(linha.produtoId()));

        acoes.getChildren().addAll(menos, quantidade, mais, notas, remover);

        card.getChildren().addAll(topo, meta, acoes);
        if (!linha.observacoes().isBlank()) {
            Label observacoes = new Label('"' + linha.observacoes() + '"');
            observacoes.getStyleClass().add("pedido-resumo-item-nota");
            observacoes.setWrapText(true);
            card.getChildren().add(observacoes);
        }
        return card;
    }

    private void ajustarQuantidade(int produtoId, int delta) {
        LinhaRascunho linha = linhasPedido.get(produtoId);
        if (linha == null) {
            return;
        }

        int novaQuantidade = linha.quantidade() + delta;
        if (novaQuantidade <= 0) {
            linhasPedido.remove(produtoId);
        } else {
            linhasPedido.put(produtoId, linha.comQuantidade(novaQuantidade));
        }
        atualizarResumoPedido();
    }

    private void removerLinha(int produtoId) {
        linhasPedido.remove(produtoId);
        atualizarResumoPedido();
    }

    private void editarObservacoes(LinhaRascunho linha) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Observacoes do Produto");

            TextArea observacoesArea = new TextArea(linha.observacoes());
            observacoesArea.setPromptText("Ex.: sem cebola, molho a parte...");
            observacoesArea.setPrefRowCount(4);

            VBox form = new VBox(12, ViewUtils.criarCampoFormulario("Observacoes", observacoesArea));
            form.getStyleClass().add("app-form");
            form.setPadding(new Insets(6, 0, 0, 0));

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Registe notas para esta linha do pedido.");
            ViewUtils.estilizarBotoesDialogo(dialog, "Guardar", "Cancelar");

            Optional<ButtonType> resultado = dialog.showAndWait();
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                linhasPedido.put(linha.produtoId(), linha.comObservacoes(observacoesArea.getText().trim()));
                atualizarResumoPedido();
            }
        } catch (RuntimeException e) {
            ViewUtils.showError("Pedidos", e.getMessage());
        }
    }

    private void mostrarDialogoSucesso(JsonNode resposta, JsonNode mesaSelecionada) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pedido Enviado");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Label icon = new Label("\u2713");
        icon.getStyleClass().add("pedido-sucesso-icone");

        Label titulo = new Label("Pedido enviado para a cozinha com sucesso!");
        titulo.getStyleClass().add("pedido-sucesso-titulo");
        titulo.setWrapText(true);

        Label subtitulo = new Label("O pedido foi criado com os produtos selecionados e gravado na tabela de linhas do pedido.");
        subtitulo.getStyleClass().add("pedido-sucesso-texto");
        subtitulo.setWrapText(true);

        VBox detalhe = new VBox(8,
                criarLinhaSucesso("Mesa", "Mesa " + ViewUtils.text(mesaSelecionada, "mesaId")),
                criarLinhaSucesso("Pedido", "#" + ViewUtils.text(resposta, "id")),
                criarLinhaSucesso("Estado", formatarEstado(ViewUtils.text(resposta, "estado")))
        );
        detalhe.getStyleClass().add("pedido-sucesso-card");

        VBox content = new VBox(18, icon, titulo, subtitulo, detalhe);
        content.getStyleClass().add("pedido-sucesso-box");
        content.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setContent(content);
        ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "A cozinha pode iniciar a preparacao imediatamente.");
        ViewUtils.estilizarBotoesDialogo(dialog, "Continuar", "Fechar");
        dialog.showAndWait();
    }

    private VBox criarLinhaSucesso(String label, String value) {
        Label labelNode = new Label(label.toUpperCase(Locale.ROOT));
        labelNode.getStyleClass().add("pedido-sucesso-meta-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("pedido-sucesso-meta-value");

        VBox box = new VBox(2, labelNode, valueNode);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void atualizarEstadoAcoes() {
        boolean valido = reservaCombo != null && reservaCombo.getValue() != null && !linhasPedido.isEmpty();
        if (limparPedidoButton != null) {
            limparPedidoButton.setDisable(linhasPedido.isEmpty());
        }
        if (confirmarPedidoButton != null) {
            confirmarPedidoButton.setDisable(!valido);
        }
    }

    private boolean produtoDisponivel(JsonNode produto) {
        String valor = ViewUtils.text(produto, "disponivel");
        return valor.isBlank() || Boolean.parseBoolean(valor);
    }

    private boolean correspondeTipo(JsonNode produto) {
        return "TODOS".equalsIgnoreCase(filtroTipoAtivo)
                || categoriaProduto(ViewUtils.text(produto, "tipo")).equalsIgnoreCase(filtroTipoAtivo);
    }

    private boolean correspondePesquisa(JsonNode produto) {
        String termo = pesquisaField == null ? "" : pesquisaField.getText().trim().toLowerCase(Locale.ROOT);
        if (termo.isBlank()) {
            return true;
        }
        return ViewUtils.text(produto, "nome").toLowerCase(Locale.ROOT).contains(termo)
                || ViewUtils.text(produto, "tipo").toLowerCase(Locale.ROOT).contains(termo);
    }

    private Map<Integer, JsonNode> selecionarReservaAtivaPorMesa(ArrayNode reservasApi) {
        Map<Integer, JsonNode> reservaAtivaPorMesa = new HashMap<>();
        for (JsonNode reserva : reservasApi) {
            int mesaId = inteiro(ViewUtils.nestedText(reserva, "numMesa", "id"));
            if (mesaId <= 0) {
                continue;
            }

            String estadoReserva = normalizarEstadoReserva(ViewUtils.text(reserva, "estado"));
            if ("CANCELADA".equals(estadoReserva)) {
                continue;
            }

            JsonNode atual = reservaAtivaPorMesa.get(mesaId);
            if (atual == null || compararDataHoraReserva(reserva, atual) > 0) {
                reservaAtivaPorMesa.put(mesaId, reserva);
            }
        }
        return reservaAtivaPorMesa;
    }

    private int compararDataHoraReserva(JsonNode atual, JsonNode outra) {
        LocalDateTime dataAtual = parseDataHora(ViewUtils.text(atual, "dataHora"));
        LocalDateTime dataOutra = parseDataHora(ViewUtils.text(outra, "dataHora"));
        if (dataAtual == null && dataOutra == null) {
            return 0;
        }
        if (dataAtual == null) {
            return -1;
        }
        if (dataOutra == null) {
            return 1;
        }
        return dataAtual.compareTo(dataOutra);
    }

    private String descricaoMesaLancamento(JsonNode mesa) {
        String descricaoBase = "Mesa " + ViewUtils.text(mesa, "mesaId") + " - " + formatarEstadoMesa(ViewUtils.text(mesa, "mesaEstado"));
        String reservaId = ViewUtils.text(mesa, "reservaId");
        if (reservaId.isBlank()) {
            return descricaoBase;
        }
        String dataHoraReserva = ViewUtils.text(mesa, "reservaDataHora");
        if (dataHoraReserva.isBlank()) {
            return descricaoBase + " | Reserva #" + reservaId;
        }
        return descricaoBase + " | Reserva #" + reservaId + " | " + formatarDataHora(dataHoraReserva);
    }

    private String normalizarEstadoReserva(String estado) {
        return estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarEstadoMesa(String estado) {
        return estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime parseDataHora(String valor) {
        String dataHora = valor == null ? "" : valor.trim();
        if (dataHora.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(dataHora), ZoneId.systemDefault());
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(dataHora);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String formatarDataHora(String valor) {
        LocalDateTime dataHora = parseDataHora(valor);
        return dataHora == null ? valor : DATA_HORA_FORMATTER.format(dataHora);
    }

    private String formatarEstado(String estado) {
        return switch ((estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT))) {
            case "REGISTADO" -> "Registado";
            case "PREPARACAO" -> "Em Preparacao";
            case "PRONTO" -> "Pronto";
            case "ENTREGUE" -> "Entregue";
            case "CANCELADO" -> "Cancelado";
            default -> estado == null ? "" : estado;
        };
    }

    private String formatarEstadoMesa(String estadoMesa) {
        return switch (normalizarEstadoMesa(estadoMesa)) {
            case "RESERVADA" -> "Reservada";
            case "OCUPADA" -> "Ocupada";
            case "LIVRE" -> "Livre";
            default -> estadoMesa == null || estadoMesa.isBlank() ? "-" : estadoMesa;
        };
    }

    private String categoriaProduto(String tipo) {
        String valor = tipo == null || tipo.isBlank() ? "Sem Categoria" : tipo.trim();
        if ("TODOS".equalsIgnoreCase(valor)) {
            return "Todos";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT) + valor.substring(1).toLowerCase(Locale.ROOT);
    }

    private BigDecimal calcularSubtotal() {
        return linhasPedido.values().stream()
                .map(LinhaRascunho::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int inteiro(String valor) {
        return Integer.parseInt(valor == null || valor.isBlank() ? "0" : valor.trim());
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor == null || valor.isBlank() ? "0" : valor.trim());
    }

    private String formatarMoeda(BigDecimal valor) {
        return MOEDA_FORMATTER.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private Region criarSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private record LinhaRascunho(int produtoId,
                                 String nome,
                                 String tipo,
                                 BigDecimal preco,
                                 int quantidade,
                                 String observacoes) {

        private LinhaRascunho comQuantidade(int novaQuantidade) {
            return new LinhaRascunho(produtoId, nome, tipo, preco, novaQuantidade, observacoes);
        }

        private LinhaRascunho comObservacoes(String novasObservacoes) {
            return new LinhaRascunho(produtoId, nome, tipo, preco, quantidade, novasObservacoes == null ? "" : novasObservacoes);
        }

        private BigDecimal subtotal() {
            return preco.multiply(BigDecimal.valueOf(quantidade));
        }
    }
}
