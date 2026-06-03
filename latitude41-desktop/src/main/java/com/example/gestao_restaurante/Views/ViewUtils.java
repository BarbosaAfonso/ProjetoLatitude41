package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ViewUtils {

    private static final DateTimeFormatter SESSAO_FORMATTER =
            DateTimeFormatter.ofPattern("dd 'de' MMMM", new Locale("pt", "PT"));

    private ViewUtils() {
    }

    public static void showInfo(String title, String message) {
        showMessageDialog(title, "Informacao", message, "dialog-icon-info");
    }

    public static void showWarning(String title, String message) {
        showMessageDialog(title, "Atenção", message, "dialog-icon-warning");
    }

    public static void showError(String title, String message) {
        showMessageDialog(title, "Erro", message, "dialog-icon-error");
    }

    public static boolean confirm(String title, String message) {
        ButtonType sim = new ButtonType("Sim", ButtonBar.ButtonData.OK_DONE);
        ButtonType nao = new ButtonType("Nao", ButtonBar.ButtonData.CANCEL_CLOSE);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(sim, nao);
        dialog.getDialogPane().setContent(criarConteudoMensagem(message, "dialog-icon-question"));

        prepararDialogo(dialog, title, "Confirme a acao antes de continuar.");
        estilizarBotao(dialog, sim, "dialog-primary-button", true, false);
        estilizarBotao(dialog, nao, "dialog-secondary-button", false, true);

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == sim;
    }

    public static void preencherCabecalho(Label sessaoLabel,
                                          Label utilizadorLabel,
                                          Label cargoLabel,
                                          Label avatarLabel,
                                          String contexto) {
        String nome = DesktopAppContext.utilizadorNome();
        String nomeFormatado = nome == null || nome.isBlank() ? "Admin" : nome.trim();

        if (utilizadorLabel != null) {
            utilizadorLabel.setText(nomeFormatado);
        }

        if (avatarLabel != null) {
            avatarLabel.setText(nomeFormatado.substring(0, 1).toUpperCase(Locale.ROOT));
        }

        if (cargoLabel != null) {
            cargoLabel.setText(DesktopAppContext.utilizadorCargoLabel().toUpperCase(Locale.ROOT));
        }

        if (sessaoLabel != null) {
            String sessao = "Sessao da Noite - " + LocalDate.now().format(SESSAO_FORMATTER);
            if (contexto != null && !contexto.isBlank()) {
                sessao += " | " + contexto;
            }
            sessaoLabel.setText(sessao);
        }
    }

    public static void prepararDialogo(Dialog<?> dialog, String titulo, String subtitulo) {
        configurarBaseDialogo(dialog);

        String stylesheet = Optional.ofNullable(ViewUtils.class.getResource("/css/gestao-produtos.css"))
                .map(url -> url.toExternalForm())
                .orElse(null);

        if (stylesheet != null && !dialog.getDialogPane().getStylesheets().contains(stylesheet)) {
            dialog.getDialogPane().getStylesheets().add(stylesheet);
        }

        dialog.getDialogPane().getStyleClass().add("app-dialog-pane");

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("app-dialog-title");

        Label subtituloLabel = new Label(subtitulo);
        subtituloLabel.getStyleClass().add("app-dialog-subtitle");
        subtituloLabel.setWrapText(true);

        VBox header = new VBox(4, tituloLabel, subtituloLabel);
        header.getStyleClass().add("app-dialog-header");

        dialog.getDialogPane().setHeader(header);
    }

    public static void estilizarBotoesDialogo(Dialog<?> dialog) {
        estilizarBotoesDialogo(dialog, "Guardar", "Cancelar");
    }

    public static void estilizarBotoesDialogo(Dialog<?> dialog, String textoConfirmar, String textoCancelar) {
        Node confirmarNode = dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (confirmarNode instanceof Button confirmar) {
            confirmar.setText(textoConfirmar);
        }

        Node cancelarNode = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelarNode instanceof Button cancelar) {
            cancelar.setText(textoCancelar);
        }

        estilizarBotao(dialog, ButtonType.OK, "dialog-primary-button", true, false);
        estilizarBotao(dialog, ButtonType.CANCEL, "dialog-secondary-button", false, true);
    }

    public static VBox criarCampoFormulario(String labelText, Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("app-form-label");

        input.getStyleClass().add("app-form-input");
        if (input instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }

        VBox box = new VBox(6, label, input);
        box.getStyleClass().add("app-form-field");
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    public static String text(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return "";
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    public static String nestedText(JsonNode node, String objectField, String nestedField) {
        if (node == null || node.isNull()) {
            return "";
        }
        JsonNode nested = node.get(objectField);
        if (nested == null || nested.isNull()) {
            return "";
        }
        JsonNode value = nested.get(nestedField);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private static void showMessageDialog(String title,
                                          String subtitulo,
                                          String message,
                                          String iconClass) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setContent(criarConteudoMensagem(message, iconClass));

        prepararDialogo(dialog, title, subtitulo);
        estilizarBotoesDialogo(dialog, "OK", "Fechar");
        dialog.showAndWait();
    }

    private static Node criarConteudoMensagem(String message, String iconClass) {
        Label icon = new Label(iconText(iconClass));
        icon.getStyleClass().addAll("dialog-message-icon", iconClass);

        Label messageLabel = new Label(Objects.requireNonNullElse(message, ""));
        messageLabel.getStyleClass().add("dialog-message-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(420);

        HBox content = new HBox(14, icon, messageLabel);
        content.getStyleClass().add("dialog-message-box");
        return content;
    }

    private static void configurarBaseDialogo(Dialog<?> dialog) {
        dialog.setGraphic(null);
        dialog.setHeaderText(null);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);
        dialog.setOnShown(event -> {
            if (dialog.getDialogPane().getScene() != null) {
                dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
            }
            ativarArrasto(dialog);
        });
    }

    private static void estilizarBotao(Dialog<?> dialog,
                                       ButtonType buttonType,
                                       String variantClass,
                                       boolean defaultButton,
                                       boolean cancelButton) {
        Node node = dialog.getDialogPane().lookupButton(buttonType);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("dialog-action-button", variantClass);
            button.setDefaultButton(defaultButton);
            button.setCancelButton(cancelButton);
        }
    }

    private static void ativarArrasto(Dialog<?> dialog) {
        Node header = dialog.getDialogPane().lookup(".app-dialog-header");
        if (header == null || dialog.getDialogPane().getScene() == null) {
            return;
        }

        Window window = dialog.getDialogPane().getScene().getWindow();
        final Delta delta = new Delta();

        header.setOnMousePressed(event -> {
            delta.x = window.getX() - event.getScreenX();
            delta.y = window.getY() - event.getScreenY();
        });

        header.setOnMouseDragged(event -> {
            window.setX(event.getScreenX() + delta.x);
            window.setY(event.getScreenY() + delta.y);
        });
    }

    private static String iconText(String iconClass) {
        return switch (iconClass) {
            case "dialog-icon-error" -> "!";
            case "dialog-icon-warning" -> "!";
            case "dialog-icon-question" -> "?";
            default -> "i";
        };
    }

    private static final class Delta {
        private double x;
        private double y;
    }
}
