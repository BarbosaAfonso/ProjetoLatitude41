package com.example.gestao_restaurante.Views;

import com.example.gestao_restaurante.Modules.Mesa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public class GestaoMesasScreen {

    @FXML
    private FlowPane mesasFlowPane;

    @FXML
    private Label ocupacaoLabel;

    @FXML
    private Button fabAdicionar;

    private final ObservableList<Mesa> mesas = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        carregarMesas();
    }

    @FXML
    private void onFloorPlan() {
        carregarMesas();
    }

    @FXML
    private void onGestaoProdutos() {
        DesktopAppContext.showGestaoProdutos();
    }

    @FXML
    private void onGestaoReservas() {
        DesktopAppContext.showGestaoReservas();
    }

    @FXML
    private void onGestaoPedidos() {
        DesktopAppContext.showGestaoPedidos();
    }

    @FXML
    private void onGestaoStock() {
        DesktopAppContext.showGestaoStock();
    }

    @FXML
    private void onTerminarSessao() {
        DesktopAppContext.limparSessao();
        DesktopAppContext.showLogin();
    }

    @FXML
    private void onAdicionar() {
        Optional<ObjectNode> payload = dialogoMesa(null);
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().post("/mesas", body);
                carregarMesas();
            } catch (RuntimeException e) {
                ViewUtils.showError("Mesas", e.getMessage());
            }
        });
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void carregarMesas() {
        try {
            ArrayNode resposta = DesktopAppContext.apiService().getArray("/mesas");
            mesas.clear();
            resposta.forEach(json -> mesas.add(mapearMesa(json)));
            mesas.sort(Comparator.comparing(Mesa::getId, Comparator.nullsLast(Integer::compareTo)));
            renderizarPlanta();
            atualizarOcupacao();
        } catch (RuntimeException e) {
            ViewUtils.showError("Mesas", e.getMessage());
        }
    }

    private void renderizarPlanta() {
        mesasFlowPane.getChildren().clear();
        for (Mesa mesa : mesas) {
            mesasFlowPane.getChildren().add(new MesaCard(mesa));
        }
    }

    private void atualizarOcupacao() {
        int total = mesas.size();
        long ocupadas = mesas.stream()
                .map(m -> EstadoMesa.from(m.getEstado()))
                .filter(estado -> estado == EstadoMesa.OCUPADA)
                .count();

        double taxa = total == 0 ? 0.0 : (ocupadas * 100.0 / total);
        ocupacaoLabel.setText(String.format(Locale.US, "OCUPACAO: %.0f%%", taxa));
    }

    private Mesa mapearMesa(JsonNode json) {
        Mesa mesa = new Mesa();
        mesa.setId(parseInt(ViewUtils.text(json, "id")));
        mesa.setNumLugares(parseInt(ViewUtils.text(json, "numLugares")));
        mesa.setEstado(ViewUtils.text(json, "estado"));
        return mesa;
    }

    private Integer parseInt(String valor) {
        try {
            return valor == null || valor.isBlank() ? null : Integer.parseInt(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void editarMesa(Mesa mesa) {
        Optional<ObjectNode> payload = dialogoMesa(mesa);
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().put("/mesas/" + mesa.getId(), body);
                carregarMesas();
            } catch (RuntimeException e) {
                ViewUtils.showError("Mesas", e.getMessage());
            }
        });
    }

    private void apagarMesa(Mesa mesa) {
        if (!ViewUtils.confirm("Mesas", "Apagar mesa ID " + mesa.getId() + "?")) {
            return;
        }

        try {
            boolean apagada = DesktopAppContext.apiService().delete("/mesas/" + mesa.getId());
            if (!apagada) {
                ViewUtils.showWarning("Mesas", "Mesa nao encontrada.");
            }
            carregarMesas();
        } catch (RuntimeException e) {
            ViewUtils.showError("Mesas", e.getMessage());
        }
    }

    private Optional<ObjectNode> dialogoMesa(Mesa atual) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(atual == null ? "Adicionar Mesa" : "Editar Mesa");

        javafx.scene.control.TextField idField = new javafx.scene.control.TextField(atual == null ? "" : String.valueOf(atual.getId()));
        javafx.scene.control.TextField lugaresField = new javafx.scene.control.TextField(atual == null ? "" : String.valueOf(atual.getNumLugares()));
        ComboBox<String> estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("LIVRE", "OCUPADA", "RESERVADA");
        estadoCombo.setValue(atual == null ? "LIVRE" : EstadoMesa.from(atual.getEstado()).label);

        idField.setPromptText("Ex: 12");
        lugaresField.setPromptText("Ex: 4");

        if (atual != null) {
            idField.setDisable(true);
        }

        HBox primeiraLinha = new HBox(12,
                ViewUtils.criarCampoFormulario("Numero da mesa", idField),
                ViewUtils.criarCampoFormulario("Numero de lugares", lugaresField)
        );
        HBox segundaLinha = new HBox(12, ViewUtils.criarCampoFormulario("Estado", estadoCombo));

        primeiraLinha.getStyleClass().add("app-form-row");
        segundaLinha.getStyleClass().add("app-form-row");

        VBox form = new VBox(12, primeiraLinha, segundaLinha);
        form.getStyleClass().add("app-form");

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Defina a capacidade e o estado atual da mesa.");
        ViewUtils.estilizarBotoesDialogo(dialog);

        Optional<ButtonType> resultado = dialog.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return Optional.empty();
        }

        try {
            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("id", Integer.parseInt(idField.getText().trim()));
            payload.put("numLugares", Integer.parseInt(lugaresField.getText().trim()));
            payload.put("estado", estadoCombo.getValue().trim().toUpperCase(Locale.ROOT));
            return Optional.of(payload);
        } catch (Exception e) {
            ViewUtils.showError("Mesas", "Dados invalidos. Verifique os campos.");
            return Optional.empty();
        }
    }

    private boolean isMesaEspecial(Mesa mesa) {
        return (mesa.getId() != null && mesa.getId() == 6)
                || (mesa.getNumLugares() != null && mesa.getNumLugares() >= 8);
    }

    private String numeroMesaFormatado(Mesa mesa) {
        if (mesa.getId() == null) {
            return "--";
        }
        String numeroBase = String.format("%02d", mesa.getId());
        return isMesaEspecial(mesa) ? numeroBase + " (VIP Booth)" : numeroBase;
    }

    private enum EstadoMesa {
        LIVRE("LIVRE", "mesa-livre", "badge-livre"),
        OCUPADA("OCUPADA", "mesa-ocupada", "badge-ocupada"),
        RESERVADA("RESERVADA", "mesa-reservada", "badge-reservada"),
        DESCONHECIDO("DESCONHECIDO", "mesa-desconhecida", "badge-desconhecida");

        private final String label;
        private final String cardClass;
        private final String badgeClass;

        EstadoMesa(String label, String cardClass, String badgeClass) {
            this.label = label;
            this.cardClass = cardClass;
            this.badgeClass = badgeClass;
        }

        private static EstadoMesa from(String valor) {
            if (valor == null) {
                return DESCONHECIDO;
            }
            String texto = valor.trim().toUpperCase(Locale.ROOT);
            return switch (texto) {
                case "LIVRE" -> LIVRE;
                case "OCUPADA" -> OCUPADA;
                case "RESERVADA" -> RESERVADA;
                default -> DESCONHECIDO;
            };
        }
    }

    private class MesaCard extends VBox {
        private final Mesa mesa;
        private final StackPane cardPane;
        private final Label badgeLabel;

        private MesaCard(Mesa mesa) {
            this.mesa = mesa;

            setAlignment(Pos.TOP_CENTER);
            setSpacing(8);
            getStyleClass().add("mesa-card-wrapper");

            cardPane = new StackPane();
            cardPane.getStyleClass().add("mesa-card");
            cardPane.setPrefHeight(isMesaEspecial(mesa) ? 170 : 152);
            cardPane.setPrefWidth(isMesaEspecial(mesa) ? 410 : 190);

            badgeLabel = new Label();
            badgeLabel.getStyleClass().add("mesa-badge");
            StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
            StackPane.setMargin(badgeLabel, new Insets(-10, -10, 0, 0));

            Label numeroMesaLabel = new Label(numeroMesaFormatado(mesa));
            numeroMesaLabel.getStyleClass().add("mesa-numero");
            if (isMesaEspecial(mesa)) {
                numeroMesaLabel.getStyleClass().add("mesa-numero-especial");
            }

            String lugares = mesa.getNumLugares() == null ? "0" : String.valueOf(mesa.getNumLugares());
            Label lugaresLabel = new Label(lugares + " LUGARES");
            lugaresLabel.getStyleClass().add("mesa-lugares");

            VBox centerBox = new VBox(3, numeroMesaLabel, lugaresLabel);
            centerBox.setAlignment(Pos.CENTER);

            cardPane.getChildren().addAll(centerBox, badgeLabel);

            Label captionLabel = new Label("MESA " + (mesa.getId() == null ? "--" : mesa.getId()));
            captionLabel.getStyleClass().add("mesa-caption");

            getChildren().addAll(cardPane, captionLabel);

            aplicarEstadoVisual();
            configurarInteracao();
        }

        private void aplicarEstadoVisual() {
            EstadoMesa estado = EstadoMesa.from(mesa.getEstado());
            cardPane.getStyleClass().removeAll("mesa-livre", "mesa-ocupada", "mesa-reservada", "mesa-desconhecida");
            cardPane.getStyleClass().add(estado.cardClass);

            badgeLabel.setText(estado.label);
            badgeLabel.getStyleClass().setAll("mesa-badge", estado.badgeClass);
        }

        private void configurarInteracao() {
            MenuItem editarItem = new MenuItem("Editar");
            MenuItem apagarItem = new MenuItem("Apagar");

            editarItem.setOnAction(event -> editarMesa(mesa));
            apagarItem.setOnAction(event -> apagarMesa(mesa));

            ContextMenu contextMenu = new ContextMenu(editarItem, apagarItem);
            setOnContextMenuRequested(event -> contextMenu.show(this, event.getScreenX(), event.getScreenY()));

            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    editarMesa(mesa);
                }
            });
        }
    }
}
