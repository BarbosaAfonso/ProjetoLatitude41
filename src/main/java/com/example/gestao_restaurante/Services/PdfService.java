package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Dtos.RelatorioAnaliseDTO;
import com.example.gestao_restaurante.Dtos.TopProdutoDTO;
import com.example.gestao_restaurante.Modules.LinhaPedido;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class PdfService {

    private static final Locale LOCALE_PT = new Locale("pt", "PT");
    private static final Path PASTA_FATURAS = Path.of("storage", "faturas");
    private static final Path PASTA_RELATORIOS = Path.of("storage", "relatorios");
    private static final DateTimeFormatter FATURA_DATAHORA_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public Path gerarPdfFatura(Integer faturaId,
                               Integer pedidoId,
                               List<LinhaPedido> linhas,
                               BigDecimal subtotal,
                               BigDecimal iva,
                               BigDecimal total) {
        try {
            Files.createDirectories(PASTA_FATURAS);
            String idSegmento = faturaId == null ? "SEM_ID" : String.valueOf(faturaId);
            String dataHoraSegmento = LocalDateTime.now().format(FATURA_DATAHORA_FORMATTER);
            String nomeFicheiro = "FAT_" + idSegmento + "_" + dataHoraSegmento + ".pdf";
            Path pdfPath = PASTA_FATURAS.resolve(nomeFicheiro);
            try (OutputStream outputStream = Files.newOutputStream(pdfPath)) {
                Document document = new Document(new Rectangle(595, 842), 40, 40, 50, 40);
                PdfWriter.getInstance(document, outputStream);
                document.open();

                escreverCabecalho(document);
                escreverReferenciaPedido(document, pedidoId, faturaId);
                escreverTabelaLinhas(document, linhas);
                escreverTotais(document, subtotal, iva, total);
                escreverRodape(document);

                document.close();
            }
            return pdfPath;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException("Nao foi possivel gerar o PDF da fatura.", e);
        }
    }

    public Path gerarPdfRelatorioAnalise(LocalDate dataInicio,
                                         LocalDate dataFim,
                                         RelatorioAnaliseDTO relatorio) {
        try {
            Files.createDirectories(PASTA_RELATORIOS);
            LocalDate referencia = dataFim == null ? (dataInicio == null ? LocalDate.now() : dataInicio) : dataFim;
            String nomeFicheiro = nomeFicheiroPdfMensal(referencia);
            Path pdfPath = PASTA_RELATORIOS.resolve(nomeFicheiro);

            try (OutputStream outputStream = Files.newOutputStream(pdfPath)) {
                Document document = new Document(new Rectangle(595, 842), 40, 40, 50, 40);
                PdfWriter.getInstance(document, outputStream);
                document.open();

                escreverCabecalhoRelatorio(document, dataInicio, dataFim);
                escreverKpisRelatorio(document, relatorio);
                escreverTabelaTopProdutosRelatorio(document, relatorio == null ? List.of() : relatorio.getTopProdutos());
                escreverRodapeRelatorio(document);

                document.close();
            }
            return pdfPath;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException("Nao foi possivel gerar o PDF do relatorio.", e);
        }
    }

    private void escreverCabecalho(Document document) throws DocumentException {
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph nomeRestaurante = new Paragraph("Latitude 41", titulo);
        nomeRestaurante.setSpacingAfter(4);
        document.add(nomeRestaurante);

        Paragraph detalhe = new Paragraph("Comprovativo de Pagamento", subtitulo);
        detalhe.setSpacingAfter(18);
        document.add(detalhe);
    }

    private void escreverReferenciaPedido(Document document, Integer pedidoId, Integer faturaId) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10);
        String referencia = "Pedido #" + (pedidoId == null ? "-" : pedidoId) + " | Fatura #" + (faturaId == null ? "-" : faturaId);
        Paragraph paragraph = new Paragraph(referencia, font);
        paragraph.setSpacingAfter(12);
        document.add(paragraph);
    }

    private void escreverTabelaLinhas(Document document, List<LinhaPedido> linhas) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 7f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(16);

        adicionarCabecalho(table, "Quantidade");
        adicionarCabecalho(table, "Produto");
        adicionarCabecalho(table, "Preco");

        if (linhas == null || linhas.isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Sem linhas de pedido."));
            vazio.setColspan(3);
            vazio.setPadding(8);
            table.addCell(vazio);
        } else {
            for (LinhaPedido linha : linhas) {
                int quantidade = linha.getQuantidade() == null ? 0 : linha.getQuantidade();
                String nomeProduto = linha.getIdProduto() == null ? "Produto" : linha.getIdProduto().getNome();
                BigDecimal precoUnitario = linha.getPrecoUnitVenda() == null ? BigDecimal.ZERO : linha.getPrecoUnitVenda();
                BigDecimal subtotalLinha = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

                adicionarCelula(table, String.valueOf(quantidade));
                adicionarCelula(table, nomeProduto == null || nomeProduto.isBlank() ? "Produto" : nomeProduto);
                adicionarCelula(table, formatarMoeda(subtotalLinha));
            }
        }

        document.add(table);
    }

    private void escreverTotais(Document document, BigDecimal subtotal, BigDecimal iva, BigDecimal total) throws DocumentException {
        Font linhaFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        Paragraph subtotalP = new Paragraph("Subtotal: " + formatarMoeda(subtotal), linhaFont);
        subtotalP.setAlignment(Paragraph.ALIGN_RIGHT);
        subtotalP.setSpacingAfter(2);

        Paragraph ivaP = new Paragraph("IVA (23%): " + formatarMoeda(iva), linhaFont);
        ivaP.setAlignment(Paragraph.ALIGN_RIGHT);
        ivaP.setSpacingAfter(4);

        Paragraph totalP = new Paragraph("Total: " + formatarMoeda(total), totalFont);
        totalP.setAlignment(Paragraph.ALIGN_RIGHT);
        totalP.setSpacingAfter(18);

        document.add(subtotalP);
        document.add(ivaP);
        document.add(totalP);
    }

    private void escreverRodape(Document document) throws DocumentException {
        Font mensagem = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11);
        Paragraph obrigado = new Paragraph("Obrigado pela sua visita!", mensagem);
        obrigado.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(obrigado);
    }

    private void escreverCabecalhoRelatorio(Document document, LocalDate dataInicio, LocalDate dataFim) throws DocumentException {
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph nomeRestaurante = new Paragraph("Latitude 41", titulo);
        nomeRestaurante.setSpacingAfter(4);
        document.add(nomeRestaurante);

        String periodo = "Relatorio de Analise de Negocio - " + (dataInicio == null ? "-" : dataInicio)
                + " ate " + (dataFim == null ? "-" : dataFim);
        Paragraph detalhe = new Paragraph(periodo, subtitulo);
        detalhe.setSpacingAfter(18);
        document.add(detalhe);
    }

    private void escreverKpisRelatorio(Document document, RelatorioAnaliseDTO relatorio) throws DocumentException {
        Font texto = FontFactory.getFont(FontFactory.HELVETICA, 11);
        BigDecimal total = relatorio == null ? BigDecimal.ZERO : relatorio.getTotalFaturado();
        BigDecimal ticket = relatorio == null ? BigDecimal.ZERO : relatorio.getTicketMedio();
        long pedidos = relatorio == null ? 0L : relatorio.getNumeroPedidos();

        Paragraph totalP = new Paragraph("Total faturado: " + formatarMoeda(total), texto);
        totalP.setSpacingAfter(4);
        document.add(totalP);

        Paragraph ticketP = new Paragraph("Ticket medio: " + formatarMoeda(ticket), texto);
        ticketP.setSpacingAfter(4);
        document.add(ticketP);

        Paragraph pedidosP = new Paragraph("Numero de pedidos: " + pedidos, texto);
        pedidosP.setSpacingAfter(12);
        document.add(pedidosP);
    }

    private void escreverTabelaTopProdutosRelatorio(Document document, List<TopProdutoDTO> topProdutos) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{5f, 3f, 2f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(16);

        adicionarCabecalho(table, "Produto");
        adicionarCabecalho(table, "Categoria");
        adicionarCabecalho(table, "Quantidade");
        adicionarCabecalho(table, "Valor Total");

        if (topProdutos == null || topProdutos.isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Sem dados de vendas no periodo selecionado."));
            vazio.setColspan(4);
            vazio.setPadding(8);
            table.addCell(vazio);
        } else {
            for (TopProdutoDTO produto : topProdutos) {
                adicionarCelula(table, produto == null ? "" : safe(produto.getNomeProduto()));
                adicionarCelula(table, produto == null ? "" : safe(produto.getCategoria()));
                adicionarCelula(table, String.valueOf(produto == null || produto.getQuantidadeVendida() == null ? 0 : produto.getQuantidadeVendida()));
                adicionarCelula(table, formatarMoeda(produto == null ? BigDecimal.ZERO : produto.getValorTotal()));
            }
        }

        document.add(table);
    }

    private void escreverRodapeRelatorio(Document document) throws DocumentException {
        Font mensagem = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10);
        Paragraph rodape = new Paragraph("Documento gerado automaticamente pelo Latitude 41.", mensagem);
        rodape.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(rodape);
    }

    private void adicionarCabecalho(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setPadding(8);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void adicionarCelula(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto == null ? "" : texto, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setPadding(8);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        table.addCell(cell);
    }

    private String formatarMoeda(BigDecimal valor) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(LOCALE_PT);
        return moeda.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String nomeFicheiroPdfMensal(LocalDate dataReferencia) {
        String mesPt = dataReferencia.getMonth().getDisplayName(TextStyle.FULL, LOCALE_PT).toUpperCase(LOCALE_PT);
        String mesNormalizado = Normalizer.normalize(mesPt, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace(' ', '_');
        return "vendas_" + mesNormalizado + ".pdf";
    }

    private String safe(String valor) {
        return valor == null ? "" : valor;
    }
}
