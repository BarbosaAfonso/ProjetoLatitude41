package com.example.gestao_restaurante.Views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class DesktopAppContext {

    private static final String BASE_TITLE = "Gestao Restaurante";
    private static final String FXML_BASE_PATH = "/fxml/";
    private static final String MAIN_SHELL_FXML = "MainView.fxml";

    private static Stage primaryStage;
    private static final ApiService apiService = new ApiService();
    private static UtilizadorSessao utilizadorSessao = UtilizadorSessao.empty();

    private static Parent mainShellRoot;
    private static MainShellScreen mainShellController;

    private DesktopAppContext() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static ApiService apiService() {
        return apiService;
    }

    public static String utilizadorNome() {
        return utilizadorSessao.nome();
    }

    public static void setUtilizadorNome(String nome) {
        utilizadorSessao = utilizadorSessao.withNome(nome);
    }

    public static Integer utilizadorId() {
        return utilizadorSessao.id();
    }

    public static String utilizadorEmail() {
        return utilizadorSessao.email();
    }

    public static String utilizadorTipo() {
        return utilizadorSessao.tipo();
    }

    public static String utilizadorEstadoConta() {
        return utilizadorSessao.estadoConta();
    }

    public static String utilizadorCargoLabel() {
        return isAdmin() ? "Administrador" : "Funcionario";
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(utilizadorSessao.tipo());
    }

    public static void setSessaoUtilizador(Integer id,
                                           String nome,
                                           String email,
                                           String tipo,
                                           String estadoConta) {
        utilizadorSessao = new UtilizadorSessao(
                id,
                nome == null ? "" : nome.trim(),
                email == null ? "" : email.trim(),
                tipo == null ? "" : tipo.trim(),
                estadoConta == null ? "" : estadoConta.trim()
        );
    }

    public static void limparSessao() {
        utilizadorSessao = UtilizadorSessao.empty();
    }

    public static void registerMainShell(MainShellScreen controller) {
        mainShellController = controller;
    }

    public static void showLogin() {
        mainShellRoot = null;
        mainShellController = null;
        navigateTo("LoginView.fxml", BASE_TITLE + " - Login");
    }

    public static void showMenuPrincipal() {
        navigateInsideShell("MenuPrincipalView.fxml", BASE_TITLE + " - Dashboard", "dashboard");
    }

    public static void showGestaoProdutos() {
        navigateInsideShell("GestaoProdutosView.fxml", BASE_TITLE + " - Produtos", "produtos");
    }

    public static void showGestaoMesas() {
        navigateInsideShell("GestaoMesasView.fxml", BASE_TITLE + " - Mesas", "mesas");
    }

    public static void showGestaoReservas() {
        navigateInsideShell("GestaoReservasView.fxml", BASE_TITLE + " - Reservas", "reservas");
    }

    public static void showGestaoPedidos() {
        navigateInsideShell("GestaoPedidosView.fxml", BASE_TITLE + " - Pedidos", "pedidos");
    }

    public static void showGestaoStock() {
        navigateInsideShell("GestaoStockView.fxml", BASE_TITLE + " - Stock", "stock");
    }

    public static void showGestaoUtilizadores() {
        if (!isAdmin()) {
            throw new IllegalStateException("A gestao de utilizadores esta disponivel apenas para administradores.");
        }
        navigateInsideShell("GestaoUtilizadoresView.fxml", BASE_TITLE + " - Utilizadores", "utilizadores");
    }

    public static void showScene(Parent root, String windowTitle) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage ainda nao foi inicializado.");
        }

        Scene scene = new Scene(root);
        primaryStage.setTitle(windowTitle);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void navigateTo(String fxmlFile, String windowTitle) {
        try {
            Parent root = loadFxml(fxmlFile);
            showScene(root, windowTitle);
        } catch (IOException e) {
            throw buildFxmlLoadException(fxmlFile, e);
        }
    }

    private static void navigateInsideShell(String contentFxml, String windowTitle, String activeMenuKey) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage ainda nao foi inicializado.");
        }

        try {
            ensureMainShellLoaded();
            Parent content = loadFxml(contentFxml);
            mainShellController.setContent(content);
            mainShellController.setActiveMenu(activeMenuKey);

            primaryStage.setTitle(windowTitle);
            if (primaryStage.getScene() == null || primaryStage.getScene().getRoot() != mainShellRoot) {
                showScene(mainShellRoot, windowTitle);
            }
        } catch (IOException e) {
            throw buildFxmlLoadException(contentFxml, e);
        }
    }

    private static void ensureMainShellLoaded() throws IOException {
        if (mainShellRoot != null && mainShellController != null) {
            return;
        }

        FXMLLoader loader = createLoader(MAIN_SHELL_FXML);
        mainShellRoot = loader.load();
        MainShellScreen controller = loader.getController();
        if (controller == null) {
            throw new IllegalStateException("Controller MainShellScreen nao foi inicializado.");
        }
        mainShellController = controller;
    }

    private static Parent loadFxml(String fxmlFile) throws IOException {
        FXMLLoader loader = createLoader(fxmlFile);
        return loader.load();
    }

    private static FXMLLoader createLoader(String fxmlFile) {
        String fullPath = FXML_BASE_PATH + fxmlFile;
        URL resource = DesktopAppContext.class.getResource(fullPath);
        if (resource == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                resource = contextClassLoader.getResource(fullPath.startsWith("/") ? fullPath.substring(1) : fullPath);
            }
        }
        if (resource == null) {
            throw new RuntimeException("Erro ao carregar interface grafica. Recurso FXML nao encontrado: " + fullPath);
        }
        return new FXMLLoader(resource);
    }

    private static RuntimeException buildFxmlLoadException(String fxmlFile, IOException e) {
        String details = rootCauseSummary(e);
        return new RuntimeException("Erro ao carregar interface grafica: " + fxmlFile + ". Causa raiz: " + details, e);
    }

    private static String rootCauseSummary(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        return root.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : " - " + message);
    }

    private record UtilizadorSessao(Integer id, String nome, String email, String tipo, String estadoConta) {
        private static UtilizadorSessao empty() {
            return new UtilizadorSessao(null, "", "", "", "");
        }

        private UtilizadorSessao withNome(String nome) {
            return new UtilizadorSessao(id, nome == null ? "" : nome, email, tipo, estadoConta);
        }
    }
}
