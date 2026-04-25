package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestaoRelatoriosScreen {

    private static final DateTimeFormatter DATA_BARRA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final Locale LOCALE_PT = new Locale("pt", "PT");
    private static final NumberFormat MOEDA_FORMATTER = NumberFormat.getCurrencyInstance(LOCALE_PT);

    @FXML
    private DatePicker dataInicioPicker;

    @FXML
    private DatePicker dataFimPicker;

    @FXML
    private Label totalFaturadoValue;

    @FXML
    private Label ticketMedioValue;

    @FXML
    private Label numeroPedidosValue;

    @FXML
    private Label intervaloResumoLabel;

    @FXML
    private Label stockResumoLabel;

    @FXML
    private BarChart<String, Number> vendasBarChart;

    @FXML
    private PieChart vendasCategoriaPie;

    @FXML
    private TableView<TopProdutoRow> vendasTable;

    @FXML
    private TableColumn<TopProdutoRow, String> colProduto;

    @FXML
    private TableColumn<TopProdutoRow, String> colCategoria;

    @FXML
    private TableColumn<TopProdutoRow, String> colQuantidade;

    @FXML
    private TableColumn<TopProdutoRow, String> colValorTotal;

    @FXML
    private Button exportarPdfButton;

    private final ObservableList<TopProdutoRow> topProdutosItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        configurarTabela();
        configurarIntervaloInicial();
        carregarRelatorio();
    }

    @FXML
    private void onAplicarFiltros() {
        carregarRelatorio();
    }

    @FXML
    private void onUltimos7Dias() {
        LocalDate hoje = LocalDate.now();
        if (dataInicioPicker != null) {
            dataInicioPicker.setValue(hoje.minusDays(6));
        }
        if (dataFimPicker != null) {
            dataFimPicker.setValue(hoje);
        }
        carregarRelatorio();
    }

    @FXML
    private void onExportarPdf() {
        try {
            LocalDate inicio = dataInicioPicker == null ? null : dataInicioPicker.getValue();
            LocalDate fim = dataFimPicker == null ? null : dataFimPicker.getValue();
            validarIntervalo(inicio, fim);
            JsonNode resposta = DesktopAppContext.apiService().getObject("/relatorios/exportar-pdf?dataInicio=" + inicio + "&dataFim=" + fim);
            String avisoAberturaPdf = abrirPdfGerado(ViewUtils.text(resposta, "caminhoPdf"));
            String mensagem = "Exportacao PDF concluida com sucesso.";
            if (!avisoAberturaPdf.isBlank()) {
                mensagem += "\n\n" + avisoAberturaPdf;
            }
            ViewUtils.showInfo("Relatorios", mensagem);
        } catch (RuntimeException e) {
            ViewUtils.showError("Relatorios", e.getMessage());
        }
    }

    @FXML
    private void onVoltar() {
        DesktopAppContext.showMenuPrincipal();
    }

    private void configurarTabela() {
        if (vendasTable == null) {
            return;
        }

        colProduto.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().nomeProduto()));
        colCategoria.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().categoria()));
        colQuantidade.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().quantidadeVendida())));
        colValorTotal.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatarMoeda(data.getValue().valorTotal())));
        vendasTable.setItems(topProdutosItems);
    }

    private void configurarIntervaloInicial() {
        LocalDate hoje = LocalDate.now();
        if (dataInicioPicker != null && dataInicioPicker.getValue() == null) {
            dataInicioPicker.setValue(hoje.minusDays(6));
        }
        if (dataFimPicker != null && dataFimPicker.getValue() == null) {
            dataFimPicker.setValue(hoje);
        }
    }

    private void carregarRelatorio() {
        try {
            LocalDate inicio = dataInicioPicker == null ? null : dataInicioPicker.getValue();
            LocalDate fim = dataFimPicker == null ? null : dataFimPicker.getValue();
            validarIntervalo(inicio, fim);

            String path = "/relatorios/analise?dataInicio=" + inicio + "&dataFim=" + fim;
            JsonNode relatorio = DesktopAppContext.apiService().getObject(path);

            atualizarKpis(relatorio, inicio, fim);
            atualizarBarChart(relatorio.path("vendasUltimos7Dias"));
            atualizarPieChart(relatorio.path("vendasPorCategoria"));
            atualizarTabela(relatorio.path("topProdutos"));
            atualizarResumoStock(relatorio.path("gastosStockPorCategoria"));
        } catch (RuntimeException e) {
            limparVisaoComErro(e.getMessage());
        }
    }

    private void atualizarKpis(JsonNode relatorio, LocalDate inicio, LocalDate fim) {
        BigDecimal totalFaturado = decimal(relatorio.path("totalFaturado"));
        BigDecimal ticketMedio = decimal(relatorio.path("ticketMedio"));
        long numeroPedidos = inteiroLong(relatorio.path("numeroPedidos"));

        totalFaturadoValue.setText(formatarMoeda(totalFaturado));
        ticketMedioValue.setText(formatarMoeda(ticketMedio));
        numeroPedidosValue.setText(String.valueOf(numeroPedidos));
        intervaloResumoLabel.setText("Intervalo: " + inicio + " ate " + fim);
    }

    private void atualizarBarChart(JsonNode vendasUltimos7Dias) {
        if (vendasBarChart == null) {
            return;
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Vendas");

        for (JsonNode item : vendasUltimos7Dias) {
            LocalDate dia = parseLocalDate(item.path("data").asText(""));
            String categoria = dia == null ? "-" : DATA_BARRA_FORMATTER.format(dia);
            BigDecimal total = decimal(item.path("total"));
            serie.getData().add(new XYChart.Data<>(categoria, total.doubleValue()));
        }

        vendasBarChart.getData().setAll(serie);
    }

    private void atualizarPieChart(JsonNode vendasPorCategoria) {
        if (vendasCategoriaPie == null) {
            return;
        }

        ObservableList<PieChart.Data> fatias = FXCollections.observableArrayList();
        for (JsonNode item : vendasPorCategoria) {
            String categoria = item.path("categoria").asText("Sem categoria");
            double total = decimal(item.path("valor")).doubleValue();
            if (total > 0.0) {
                fatias.add(new PieChart.Data(categoria, total));
            }
        }

        if (fatias.isEmpty()) {
            fatias.add(new PieChart.Data("Sem dados", 1));
        }

        vendasCategoriaPie.setData(fatias);
    }

    private void atualizarTabela(JsonNode topProdutos) {
        topProdutosItems.clear();
        for (JsonNode item : topProdutos) {
            topProdutosItems.add(new TopProdutoRow(
                    item.path("nomeProduto").asText("-"),
                    item.path("categoria").asText("Sem categoria"),
                    inteiroLong(item.path("quantidadeVendida")),
                    decimal(item.path("valorTotal"))
            ));
        }
    }

    private void atualizarResumoStock(JsonNode gastosStockPorCategoria) {
        List<String> resumo = new ArrayList<>();
        int limite = 3;
        int contador = 0;

        for (JsonNode item : gastosStockPorCategoria) {
            if (contador >= limite) {
                break;
            }
            String categoria = item.path("categoria").asText("Sem categoria");
            String valor = formatarMoeda(decimal(item.path("valor")));
            resumo.add(categoria + ": " + valor);
            contador++;
        }

        if (resumo.isEmpty()) {
            stockResumoLabel.setText("Gasto em stock por categoria: sem dados.");
            return;
        }

        stockResumoLabel.setText("Gasto em stock por categoria: " + String.join(" | ", resumo));
    }

    private void limparVisaoComErro(String mensagem) {
        totalFaturadoValue.setText("--");
        ticketMedioValue.setText("--");
        numeroPedidosValue.setText("--");
        intervaloResumoLabel.setText("Nao foi possivel carregar o relatorio.");
        stockResumoLabel.setText(mensagem == null || mensagem.isBlank() ? "Erro inesperado." : mensagem);
        if (vendasBarChart != null) {
            vendasBarChart.getData().clear();
        }
        if (vendasCategoriaPie != null) {
            vendasCategoriaPie.getData().clear();
        }
        topProdutosItems.clear();
    }

    private void validarIntervalo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Selecione data de inicio e data de fim.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }
    }

    private String abrirPdfGerado(String caminhoPdf) {
        if (caminhoPdf == null || caminhoPdf.isBlank()) {
            throw new IllegalArgumentException("O relatorio foi gerado, mas nao foi devolvido caminho do PDF.");
        }

        File ficheiro = new File(caminhoPdf);
        if (!ficheiro.exists()) {
            throw new IllegalArgumentException("O PDF do relatorio nao foi encontrado no caminho indicado.");
        }

        if (tentarAbrirComDesktop(ficheiro) || tentarAbrirComFallbackSistema(ficheiro)) {
            return "";
        }

        return "O relatorio foi gerado, mas nao foi possivel abrir automaticamente o PDF.\n"
                + "Abra manualmente em: " + ficheiro.getAbsolutePath();
    }

    private boolean tentarAbrirComDesktop(File ficheiro) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                return false;
            }
            desktop.open(ficheiro);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tentarAbrirComFallbackSistema(File ficheiro) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", "\"" + ficheiro.getAbsolutePath() + "\"").start();
                return true;
            }
            if (os.contains("mac")) {
                new ProcessBuilder("open", ficheiro.getAbsolutePath()).start();
                return true;
            }
            if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", ficheiro.getAbsolutePath()).start();
                return true;
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        return MOEDA_FORMATTER.format(seguro);
    }

    private long inteiroLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0L;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        try {
            return Long.parseLong(node.asText("0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText("0").replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseLocalDate(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (Exception e) {
            return null;
        }
    }

    private record TopProdutoRow(String nomeProduto, String categoria, long quantidadeVendida, BigDecimal valorTotal) {
    }
}
