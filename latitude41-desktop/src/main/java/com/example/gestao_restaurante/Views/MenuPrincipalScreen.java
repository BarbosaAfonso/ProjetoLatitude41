package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

public class MenuPrincipalScreen {

    @FXML
    private Label kpiProdutosValue;

    @FXML
    private Label kpiProdutosSub;

    @FXML
    private Label kpiReservasValue;

    @FXML
    private Label kpiReservasSub;

    @FXML
    private Label kpiPedidosValue;

    @FXML
    private Label kpiPedidosSub;

    @FXML
    private Label kpiOcupacaoValue;

    @FXML
    private Label kpiOcupacaoSub;

    @FXML
    private Label mesasResumoLabel;

    @FXML
    private Label resumoOperacionalLabel;

    @FXML
    private void initialize() {
        validarInjecoesFXML();
        carregarIndicadores();
    }

    private void validarInjecoesFXML() {
        if (kpiProdutosValue == null || kpiProdutosSub == null || kpiReservasValue == null || kpiReservasSub == null
                || kpiPedidosValue == null || kpiPedidosSub == null || kpiOcupacaoValue == null
                || kpiOcupacaoSub == null || mesasResumoLabel == null || resumoOperacionalLabel == null) {
            throw new IllegalStateException("Falha de injecao FXML em MenuPrincipalScreen: verifica fx:id e fx:controller.");
        }
    }

    private void carregarIndicadores() {
        try {
            ArrayNode produtos = DesktopAppContext.apiService().getArray("/produtos");
            ArrayNode reservas = DesktopAppContext.apiService().getArray("/reservas");
            ArrayNode pedidos = DesktopAppContext.apiService().getArray("/pedidos");
            ArrayNode mesas = DesktopAppContext.apiService().getArray("/mesas");

            preencherIndicadorProdutos(produtos);
            preencherIndicadorReservas(reservas);
            preencherIndicadorPedidos(pedidos);
            preencherIndicadorMesas(mesas);
        } catch (RuntimeException e) {
            kpiProdutosValue.setText("--");
            kpiReservasValue.setText("--");
            kpiPedidosValue.setText("--");
            kpiOcupacaoValue.setText("--");
            kpiProdutosSub.setText("Sem dados");
            kpiReservasSub.setText("Sem dados");
            kpiPedidosSub.setText("Sem dados");
            kpiOcupacaoSub.setText("Sem dados");
            mesasResumoLabel.setText("Nao foi possivel carregar os dados.");
            resumoOperacionalLabel.setText("Confirma se a API esta ativa em http://localhost:8080.");
        }
    }

    private void preencherIndicadorProdutos(ArrayNode produtos) {
        long ativos = streamCount(produtos, json -> !json.hasNonNull("disponivel") || json.path("disponivel").asBoolean(true));
        kpiProdutosValue.setText(String.valueOf(ativos));
        kpiProdutosSub.setText("de " + produtos.size() + " produtos disponiveis");
    }

    private void preencherIndicadorReservas(ArrayNode reservas) {
        int reservasHoje = 0;
        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());

        for (JsonNode reserva : reservas) {
            String dataHora = firstNonBlank(
                    ViewUtils.text(reserva, "dataHora"),
                    ViewUtils.text(reserva, "data_hora"),
                    ViewUtils.text(reserva, "data")
            );

            LocalDate dataReserva = parseDate(dataHora);
            if (dataReserva != null && hoje.equals(dataReserva)) {
                reservasHoje++;
            }
        }

        kpiReservasValue.setText(String.valueOf(reservasHoje));
        kpiReservasSub.setText("reservas para hoje");
    }

    private void preencherIndicadorPedidos(ArrayNode pedidos) {
        long emCurso = streamCount(pedidos, json -> {
            String estado = ViewUtils.text(json, "estado").toUpperCase(Locale.ROOT).trim();
            return !estado.equals("ENTREGUE")
                    && !estado.equals("CANCELADO")
                    && !estado.equals("CONCLUIDO")
                    && !estado.equals("FINALIZADO")
                    && !estado.equals("PAGO");
        });
        kpiPedidosValue.setText(String.valueOf(emCurso));
        kpiPedidosSub.setText("pedidos em execucao");

        resumoOperacionalLabel.setText("Atualmente existem " + emCurso + " pedidos em curso e " + pedidos.size() + " pedidos no total.");
    }

    private void preencherIndicadorMesas(ArrayNode mesas) {
        int total = mesas.size();
        long livres = streamCount(mesas, json -> "LIVRE".equalsIgnoreCase(ViewUtils.text(json, "estado")));
        long ocupadas = streamCount(mesas, json -> "OCUPADA".equalsIgnoreCase(ViewUtils.text(json, "estado")));
        long reservadas = streamCount(mesas, json -> "RESERVADA".equalsIgnoreCase(ViewUtils.text(json, "estado")));

        double ocupacao = total == 0 ? 0.0 : (ocupadas * 100.0 / total);
        kpiOcupacaoValue.setText(String.format(Locale.US, "%.0f%%", ocupacao));
        kpiOcupacaoSub.setText(ocupadas + " de " + total + " mesas ocupadas");

        mesasResumoLabel.setText("Livres: " + livres + " | Ocupadas: " + ocupadas + " | Reservadas: " + reservadas);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(rawDate).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
            // Try next format
        }

        try {
            return OffsetDateTime.parse(rawDate).toLocalDate();
        } catch (Exception ignored) {
            // Try next format
        }

        try {
            return LocalDateTime.parse(rawDate).toLocalDate();
        } catch (Exception ignored) {
            // Try next format
        }

        try {
            return LocalDate.parse(rawDate);
        } catch (Exception ignored) {
            return null;
        }
    }

    private long streamCount(ArrayNode array, java.util.function.Predicate<JsonNode> predicate) {
        long total = 0;
        for (JsonNode jsonNode : array) {
            if (predicate.test(jsonNode)) {
                total++;
            }
        }
        return total;
    }
}
