package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
            resultadoLogin = DesktopAppContext.getUtilizadorService().login(email.trim(), password);
        } catch (RuntimeException e) {
            e.printStackTrace();
            ViewUtils.showError(
                    "Login",
                    "Nao foi possivel validar login na API. Confirma se o backend esta ativo em http://localhost:8080.\n\n"
                            + "Para arrancar: abre o modulo latitude41-api e executa .\\mvnw.cmd spring-boot:run.\n\nDetalhe: "
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
}
