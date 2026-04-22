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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Optional;

public class GestaoUtilizadoresScreen {

    @FXML
    private TableView<JsonNode> tabela;

    @FXML
    private TableColumn<JsonNode, String> colId;

    @FXML
    private TableColumn<JsonNode, String> colNome;

    @FXML
    private TableColumn<JsonNode, String> colEmail;

    @FXML
    private TableColumn<JsonNode, String> colTipo;

    @FXML
    private TableColumn<JsonNode, String> colEstadoConta;

    @FXML
    private TableColumn<JsonNode, String> colAcoes;

    @FXML
    private TextField pesquisaField;

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> dadosFiltrados = new FilteredList<>(dados, utilizador -> true);

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
        colNome.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "nome")));
        colEmail.setCellValueFactory(data -> new ReadOnlyStringWrapper(ViewUtils.text(data.getValue(), "email")));
        colTipo.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatarTipo(ViewUtils.text(data.getValue(), "tipo"))));
        colEstadoConta.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatarEstadoConta(ViewUtils.text(data.getValue(), "estadoConta"))));
        colAcoes.setCellValueFactory(data -> new ReadOnlyStringWrapper("acoes"));

        colAcoes.setSortable(false);
        colAcoes.setCellFactory(column -> new TableCell<>() {
            private final Button btnEditar = new Button("\u270E");
            private final Button btnEstado = new Button();
            private final HBox wrapper = new HBox(8, btnEditar, btnEstado);

            {
                wrapper.setAlignment(Pos.CENTER);
                btnEditar.getStyleClass().addAll("acao-button", "acao-editar");

                btnEditar.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        editarUtilizador(linha);
                    }
                });

                btnEstado.setOnAction(event -> {
                    JsonNode linha = getTableRow() == null ? null : (JsonNode) getTableRow().getItem();
                    if (linha != null) {
                        alternarEstadoConta(linha);
                    }
                });

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                JsonNode linha = (JsonNode) getTableRow().getItem();
                boolean ativo = isContaAtiva(linha);
                btnEstado.getStyleClass().setAll("acao-button", ativo ? "acao-cancelar" : "acao-confirmar");
                btnEstado.setText(ativo ? "Desativar" : "Reativar");
                setGraphic(wrapper);
            }
        });
    }

    @FXML
    private void onPesquisar() {
        String termo = pesquisaField == null ? "" : pesquisaField.getText().trim().toLowerCase(Locale.ROOT);
        dadosFiltrados.setPredicate(utilizador -> termo.isBlank()
                || contem(utilizador, "id", termo)
                || contem(utilizador, "nome", termo)
                || contem(utilizador, "email", termo)
                || contem(utilizador, "tipo", termo)
                || contem(utilizador, "estadoConta", termo));
    }

    @FXML
    private void onAdicionar() {
        Optional<ObjectNode> payload = dialogoNovoUtilizador();
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().post("/utilizadores", body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Utilizadores", e.getMessage());
            }
        });
    }

    @FXML
    private void onAtualizar() {
        carregarDados();
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void editarUtilizador(JsonNode utilizador) {
        Optional<ObjectNode> payload = dialogoUtilizador(utilizador);
        payload.ifPresent(body -> {
            try {
                DesktopAppContext.apiService().put("/utilizadores/" + ViewUtils.text(utilizador, "id"), body);
                carregarDados();
            } catch (RuntimeException e) {
                ViewUtils.showError("Utilizadores", e.getMessage());
            }
        });
    }

    private void alternarEstadoConta(JsonNode utilizador) {
        boolean ativo = isContaAtiva(utilizador);
        String nome = ViewUtils.text(utilizador, "nome");
        String novoEstado = ativo ? "INATIVO" : "ATIVO";
        String acao = ativo ? "desativar" : "reativar";

        if (!ViewUtils.confirm("Utilizadores", "Pretende " + acao + " a conta de " + nome + "?")) {
            return;
        }

        try {
            ObjectNode payload = criarPayloadUtilizador(utilizador);
            payload.put("estadoConta", novoEstado);
            DesktopAppContext.apiService().put("/utilizadores/" + ViewUtils.text(utilizador, "id"), payload);
            carregarDados();
        } catch (RuntimeException e) {
            ViewUtils.showError("Utilizadores", e.getMessage());
        }
    }

    private void carregarDados() {
        try {
            ArrayNode resposta = DesktopAppContext.apiService().getArray("/utilizadores");
            dados.clear();
            resposta.forEach(dados::add);
            onPesquisar();
        } catch (RuntimeException e) {
            ViewUtils.showError("Utilizadores", e.getMessage());
        }
    }

    private Optional<ObjectNode> dialogoUtilizador(JsonNode atual) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Editar Utilizador");

            TextField nomeField = new TextField(ViewUtils.text(atual, "nome"));
            TextField emailField = new TextField(ViewUtils.text(atual, "email"));
            TextField contactoField = new TextField(ViewUtils.text(atual, "contacto"));
            TextField passwordField = new TextField();
            passwordField.setPromptText("Deixe vazio para manter a password");

            ComboBox<String> tipoCombo = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "FUNCIONARIO"));
            tipoCombo.setValue(normalizar(ViewUtils.text(atual, "tipo"), "FUNCIONARIO"));

            ComboBox<String> estadoCombo = new ComboBox<>(FXCollections.observableArrayList("ATIVO", "INATIVO"));
            estadoCombo.setValue(normalizar(ViewUtils.text(atual, "estadoConta"), "ATIVO"));

            HBox linhaPrincipal = new HBox(12,
                    ViewUtils.criarCampoFormulario("Nome", nomeField),
                    ViewUtils.criarCampoFormulario("Email", emailField)
            );
            HBox linhaDetalhe = new HBox(12,
                    ViewUtils.criarCampoFormulario("Contacto", contactoField),
                    ViewUtils.criarCampoFormulario("Password", passwordField)
            );
            HBox linhaPermissoes = new HBox(12,
                    ViewUtils.criarCampoFormulario("Tipo", tipoCombo),
                    ViewUtils.criarCampoFormulario("Estado da Conta", estadoCombo)
            );

            linhaPrincipal.getStyleClass().add("app-form-row");
            linhaDetalhe.getStyleClass().add("app-form-row");
            linhaPermissoes.getStyleClass().add("app-form-row");

            VBox form = new VBox(12, linhaPrincipal, linhaDetalhe, linhaPermissoes);
            form.getStyleClass().add("app-form");

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Atualize o cargo e o estado da conta do colaborador.");
            ViewUtils.estilizarBotoesDialogo(dialog, "Guardar", "Cancelar");

            Optional<ButtonType> resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
                return Optional.empty();
            }

            ObjectNode payload = criarPayloadUtilizador(atual);
            payload.put("nome", nomeField.getText().trim());
            payload.put("email", emailField.getText().trim());
            payload.put("contacto", contactoField.getText().trim());
            if (!passwordField.getText().isBlank()) {
                payload.put("password", passwordField.getText());
            }
            payload.put("tipo", tipoCombo.getValue());
            payload.put("estadoConta", estadoCombo.getValue());
            return Optional.of(payload);
        } catch (RuntimeException e) {
            ViewUtils.showError("Utilizadores", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ObjectNode> dialogoNovoUtilizador() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Nova Conta");

            TextField nomeField = new TextField();
            TextField emailField = new TextField();
            TextField contactoField = new TextField();
            TextField passwordField = new TextField();
            passwordField.setPromptText("Password inicial");

            ComboBox<String> tipoCombo = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "FUNCIONARIO"));
            tipoCombo.setValue("FUNCIONARIO");

            ComboBox<String> estadoCombo = new ComboBox<>(FXCollections.observableArrayList("ATIVO", "INATIVO"));
            estadoCombo.setValue("ATIVO");

            HBox linhaPrincipal = new HBox(12,
                    ViewUtils.criarCampoFormulario("Nome", nomeField),
                    ViewUtils.criarCampoFormulario("Email", emailField)
            );
            HBox linhaDetalhe = new HBox(12,
                    ViewUtils.criarCampoFormulario("Contacto", contactoField),
                    ViewUtils.criarCampoFormulario("Password", passwordField)
            );
            HBox linhaPermissoes = new HBox(12,
                    ViewUtils.criarCampoFormulario("Tipo", tipoCombo),
                    ViewUtils.criarCampoFormulario("Estado da Conta", estadoCombo)
            );

            linhaPrincipal.getStyleClass().add("app-form-row");
            linhaDetalhe.getStyleClass().add("app-form-row");
            linhaPermissoes.getStyleClass().add("app-form-row");

            VBox form = new VBox(12, linhaPrincipal, linhaDetalhe, linhaPermissoes);
            form.getStyleClass().add("app-form");

            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ViewUtils.prepararDialogo(dialog, dialog.getTitle(), "Crie uma conta nova e defina o tipo e o estado inicial.");
            ViewUtils.estilizarBotoesDialogo(dialog, "Criar Conta", "Cancelar");

            Optional<ButtonType> resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
                return Optional.empty();
            }

            ObjectNode payload = DesktopAppContext.apiService().createObject();
            payload.put("nome", nomeField.getText().trim());
            payload.put("email", emailField.getText().trim());
            payload.put("contacto", contactoField.getText().trim());
            payload.put("password", passwordField.getText());
            payload.put("tipo", tipoCombo.getValue());
            payload.put("estadoConta", estadoCombo.getValue());
            return Optional.of(payload);
        } catch (RuntimeException e) {
            ViewUtils.showError("Utilizadores", e.getMessage());
            return Optional.empty();
        }
    }

    private ObjectNode criarPayloadUtilizador(JsonNode utilizador) {
        ObjectNode payload = DesktopAppContext.apiService().createObject();
        payload.put("nome", ViewUtils.text(utilizador, "nome"));
        payload.put("contacto", ViewUtils.text(utilizador, "contacto"));
        payload.put("email", ViewUtils.text(utilizador, "email"));
        payload.put("tipo", normalizar(ViewUtils.text(utilizador, "tipo"), "FUNCIONARIO"));
        payload.put("estadoConta", normalizar(ViewUtils.text(utilizador, "estadoConta"), "ATIVO"));
        return payload;
    }

    private boolean contem(JsonNode utilizador, String campo, String termo) {
        return ViewUtils.text(utilizador, campo).toLowerCase(Locale.ROOT).contains(termo);
    }

    private boolean isContaAtiva(JsonNode utilizador) {
        return "ATIVO".equals(normalizar(ViewUtils.text(utilizador, "estadoConta"), "ATIVO"));
    }

    private String formatarTipo(String tipo) {
        return "ADMIN".equalsIgnoreCase(tipo) ? "Administrador" : "Funcionario";
    }

    private String formatarEstadoConta(String estado) {
        return isValor(estado, "ATIVO") ? "Ativa" : "Inativa";
    }

    private boolean isValor(String texto, String valor) {
        return valor.equals(normalizar(texto, ""));
    }

    private String normalizar(String texto, String valorPadrao) {
        String valor = texto == null || texto.isBlank() ? valorPadrao : texto.trim();
        return valor.toUpperCase(Locale.ROOT);
    }
}
