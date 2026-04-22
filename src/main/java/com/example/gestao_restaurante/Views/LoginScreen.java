package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class LoginScreen {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void onEntrar() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            ViewUtils.showWarning("Login", "Preencha email e password.");
            return;
        }

        Optional<JsonNode> resultadoLogin;
        try {
            resultadoLogin = DesktopAppContext.apiService().login(email.trim(), password);
        } catch (RuntimeException e) {
            e.printStackTrace();
            ViewUtils.showError(
                    "Login",
                    "Nao foi possivel validar login na API. Confirma se o backend esta ativo em http://localhost:8080.\n\nDetalhe: "
                            + e.getMessage()
            );
            return;
        }

        if (resultadoLogin.isEmpty()) {
            ViewUtils.showError("Login", "Credenciais invalidas.");
            return;
        }

        JsonNode utilizador = resultadoLogin.get();
        String nome = ViewUtils.text(utilizador, "nome");
        DesktopAppContext.setSessaoUtilizador(
                utilizador.path("id").isNumber() ? utilizador.path("id").asInt() : null,
                nome.isBlank() ? email.trim() : nome,
                ViewUtils.text(utilizador, "email"),
                ViewUtils.text(utilizador, "tipo"),
                ViewUtils.text(utilizador, "estadoConta")
        );

        try {
            DesktopAppContext.showMenuPrincipal();
        } catch (RuntimeException e) {
            e.printStackTrace();
            ViewUtils.showError(
                    "Login",
                    "Erro ao carregar interface grafica (MainShell/MenuPrincipal).\n\nDetalhe: " + e.getMessage()
            );
        }
    }

    @FXML
    private void onRegistar() {
        Optional<ObjectNode> payload = dialogoRegisto();
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().post("/utilizadores", body);
                ViewUtils.showInfo("Registo", "Conta criada com sucesso. Pode iniciar sessao.");
            } catch (RuntimeException e) {
                ViewUtils.showError("Registo", e.getMessage());
            }
        });
    }

    private Optional<ObjectNode> dialogoRegisto() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Criar Conta");

            TextField nomeField = new TextField();
            TextField emailRegistoField = new TextField();
            TextField contactoField = new TextField();
            PasswordField passwordRegistoField = new PasswordField();
            PasswordField confirmarPasswordField = new PasswordField();

            HBox linhaPrincipal = new HBox(12,
                    ViewUtils.criarCampoFormulario("Nome", nomeField),
                    ViewUtils.criarCampoFormulario("Email", emailRegistoField)
            );
            HBox linhaDetalhe = new HBox(12, ViewUtils.criarCampoFormulario("Contacto", contactoField));
            HBox linhaPassword = new HBox(12,
                    ViewUtils.criarCampoFormulario("Password", passwordRegistoField),
                    ViewUtils.criarCampoFormulario("Confirmar Password", confirmarPasswordField)
            );

            linhaPrincipal.getStyleClass().add("app-form-row");
            linhaDetalhe.getStyleClass().add("app-form-row");
            linhaPassword.getStyleClass().add("app-form-row");

            VBox form = new VBox(12, linhaPrincipal, linhaDetalhe, linhaPassword);
            form.getStyleClass().add("app-form");

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Registe uma conta nova. As contas criadas aqui iniciam como funcionario.");
            ViewUtils.estilizarBotoesDialogo(dialog, "Criar Conta", "Cancelar");

            Optional<ButtonType> resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
                return Optional.empty();
            }

            String password = passwordRegistoField.getText();
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("A password e obrigatoria.");
            }
            if (!password.equals(confirmarPasswordField.getText())) {
                throw new IllegalArgumentException("A confirmacao da password nao coincide.");
            }

            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("nome", nomeField.getText().trim());
            payload.put("email", emailRegistoField.getText().trim());
            payload.put("contacto", contactoField.getText().trim());
            payload.put("password", password);
            payload.put("tipo", "FUNCIONARIO");
            payload.put("estadoConta", "ATIVO");
            return Optional.of(payload);
        } catch (RuntimeException e) {
            ViewUtils.showError("Registo", e.getMessage());
            return Optional.empty();
        }
    }
}
