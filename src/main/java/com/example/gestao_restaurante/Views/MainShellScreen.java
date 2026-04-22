package com.example.gestao_restaurante.Views;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainShellScreen {

    @FXML
    private StackPane contentArea;

    @FXML
    private Label headerTitleLabel;

    @FXML
    private Label headerSubtitleLabel;

    @FXML
    private Label utilizadorLabel;

    @FXML
    private Label cargoLabel;

    @FXML
    private Label avatarLabel;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnGestaoMesas;

    @FXML
    private Button btnGestaoProdutos;

    @FXML
    private Button btnGestaoReservas;

    @FXML
    private Button btnGestaoPedidos;

    @FXML
    private Button btnGestaoStock;

    @FXML
    private Button btnGestaoUtilizadores;

    @FXML
    private Button btnTerminarSessao;

    private final Map<String, Button> botoesNavegacao = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        validateInjectedNodes();
        ViewUtils.preencherCabecalho(headerSubtitleLabel, utilizadorLabel, cargoLabel, avatarLabel, null);

        botoesNavegacao.put("dashboard", btnDashboard);
        botoesNavegacao.put("mesas", btnGestaoMesas);
        botoesNavegacao.put("produtos", btnGestaoProdutos);
        botoesNavegacao.put("reservas", btnGestaoReservas);
        botoesNavegacao.put("pedidos", btnGestaoPedidos);
        botoesNavegacao.put("stock", btnGestaoStock);
        botoesNavegacao.put("utilizadores", btnGestaoUtilizadores);

        boolean admin = DesktopAppContext.isAdmin();
        if (btnGestaoUtilizadores != null) {
            btnGestaoUtilizadores.setManaged(admin);
            btnGestaoUtilizadores.setVisible(admin);
        }

        DesktopAppContext.registerMainShell(this);
    }

    @FXML
    private void onDashboard() {
        DesktopAppContext.showMenuPrincipal();
    }

    @FXML
    private void onGestaoMesas() {
        DesktopAppContext.showGestaoMesas();
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
    private void onGestaoUtilizadores() {
        DesktopAppContext.showGestaoUtilizadores();
    }

    @FXML
    private void onTerminarSessao() {
        DesktopAppContext.limparSessao();
        DesktopAppContext.showLogin();
    }

    public void setContent(Node node) {
        if (node instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        contentArea.getChildren().setAll(node);
    }

    public void setActiveMenu(String key) {
        for (Map.Entry<String, Button> entry : botoesNavegacao.entrySet()) {
            Button botao = entry.getValue();
            botao.getStyleClass().remove("nav-button-active");
            if (entry.getKey().equals(key)) {
                botao.getStyleClass().add("nav-button-active");
            }
        }
    }

    public void setHeaderTitle(String title) {
        if (headerTitleLabel != null) {
            headerTitleLabel.setText(title == null || title.isBlank() ? "Dashboard" : title);
        }
    }

    private void validateInjectedNodes() {
        if (contentArea == null
                || headerTitleLabel == null
                || headerSubtitleLabel == null
                || utilizadorLabel == null
                || cargoLabel == null
                || avatarLabel == null
                || btnDashboard == null
                || btnGestaoMesas == null
                || btnGestaoProdutos == null
                || btnGestaoReservas == null
                || btnGestaoPedidos == null
                || btnGestaoStock == null
                || btnGestaoUtilizadores == null
                || btnTerminarSessao == null) {
            throw new IllegalStateException("MainView.fxml nao esta sincronizado com MainShellScreen (fx:id/@FXML).");
        }
    }
}
