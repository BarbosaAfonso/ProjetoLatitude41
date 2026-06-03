package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Dtos.CategoriaValorDTO;
import com.example.gestao_restaurante.Dtos.RelatorioAnaliseDTO;
import com.example.gestao_restaurante.Dtos.TopProdutoDTO;
import com.example.gestao_restaurante.Dtos.VendaDiariaDTO;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RelatorioService {

    private static final int TOP_PRODUTOS_LIMITE = 10;
    private static final String CATEGORIA_SEM_VALOR = "Sem categoria";

    private final EntityManager entityManager;
    private final PdfService pdfService;

    public RelatorioService(EntityManager entityManager, PdfService pdfService) {
        this.entityManager = entityManager;
        this.pdfService = pdfService;
    }

    public RelatorioAnaliseDTO gerarAnalise(LocalDate dataInicio, LocalDate dataFim) {
        validarIntervalo(dataInicio, dataFim);

        ZoneId zoneId = ZoneId.systemDefault();
        Instant inicioPeriodo = dataInicio.atStartOfDay(zoneId).toInstant();
        Instant fimPeriodoExclusivo = dataFim.plusDays(1).atStartOfDay(zoneId).toInstant();

        BigDecimal totalFaturado = consultarTotalFaturado(inicioPeriodo, fimPeriodoExclusivo);
        long numeroPedidos = consultarNumeroPedidos(inicioPeriodo, fimPeriodoExclusivo);
        BigDecimal ticketMedio = numeroPedidos <= 0
                ? BigDecimal.ZERO
                : totalFaturado.divide(BigDecimal.valueOf(numeroPedidos), 2, RoundingMode.HALF_UP);

        RelatorioAnaliseDTO dto = new RelatorioAnaliseDTO();
        dto.setDataInicio(dataInicio);
        dto.setDataFim(dataFim);
        dto.setTotalFaturado(totalFaturado);
        dto.setNumeroPedidos(numeroPedidos);
        dto.setTicketMedio(ticketMedio);
        dto.setVendasUltimos7Dias(consultarVendasUltimos7Dias(dataInicio, dataFim, zoneId));
        dto.setVendasPorCategoria(consultarVendasPorCategoria(inicioPeriodo, fimPeriodoExclusivo));
        dto.setTopProdutos(consultarTopProdutos(inicioPeriodo, fimPeriodoExclusivo));
        dto.setGastosStockPorCategoria(consultarGastoStockPorCategoria());
        return dto;
    }

    public Path exportarAnaliseEmPdf(LocalDate dataInicio, LocalDate dataFim) {
        RelatorioAnaliseDTO analise = gerarAnalise(dataInicio, dataFim);
        return pdfService.gerarPdfRelatorioAnalise(dataInicio, dataFim, analise);
    }

    private BigDecimal consultarTotalFaturado(Instant inicioPeriodo, Instant fimPeriodoExclusivo) {
        BigDecimal valor = entityManager.createQuery("""
                        select coalesce(sum(f.valor), 0)
                        from Fatura f
                        where f.dataHora >= :inicio and f.dataHora < :fim
                        """, BigDecimal.class)
                .setParameter("inicio", inicioPeriodo)
                .setParameter("fim", fimPeriodoExclusivo)
                .getSingleResult();

        return valor == null ? BigDecimal.ZERO : valor;
    }

    private long consultarNumeroPedidos(Instant inicioPeriodo, Instant fimPeriodoExclusivo) {
        Long total = entityManager.createQuery("""
                        select count(distinct f.idPedido.id)
                        from Fatura f
                        where f.dataHora >= :inicio and f.dataHora < :fim
                        """, Long.class)
                .setParameter("inicio", inicioPeriodo)
                .setParameter("fim", fimPeriodoExclusivo)
                .getSingleResult();

        return total == null ? 0L : total;
    }

    private List<VendaDiariaDTO> consultarVendasUltimos7Dias(LocalDate dataInicio, LocalDate dataFim, ZoneId zoneId) {
        LocalDate inicioSerie = dataFim.minusDays(6);
        if (inicioSerie.isBefore(dataInicio)) {
            inicioSerie = dataInicio;
        }

        Instant inicio = inicioSerie.atStartOfDay(zoneId).toInstant();
        Instant fimExclusivo = dataFim.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT DATE(f.data_hora) AS dia, COALESCE(SUM(f.valor), 0) AS total
                        FROM fatura f
                        WHERE f.data_hora >= :inicio AND f.data_hora < :fim
                        GROUP BY DATE(f.data_hora)
                        ORDER BY dia
                        """)
                .setParameter("inicio", Timestamp.from(inicio))
                .setParameter("fim", Timestamp.from(fimExclusivo))
                .getResultList();

        Map<LocalDate, BigDecimal> valoresPorDia = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate dia = toLocalDate(row[0]);
            if (dia != null) {
                valoresPorDia.put(dia, toBigDecimal(row[1]));
            }
        }

        List<VendaDiariaDTO> serie = new ArrayList<>();
        LocalDate cursor = inicioSerie;
        while (!cursor.isAfter(dataFim)) {
            BigDecimal totalDia = valoresPorDia.getOrDefault(cursor, BigDecimal.ZERO);
            serie.add(new VendaDiariaDTO(cursor, totalDia));
            cursor = cursor.plusDays(1);
        }
        return serie;
    }

    private List<TopProdutoDTO> consultarTopProdutos(Instant inicioPeriodo, Instant fimPeriodoExclusivo) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT
                            p.id_produto,
                            p.nome,
                            COALESCE(NULLIF(TRIM(p.tipo), ''), 'Sem categoria') AS categoria,
                            COALESCE(SUM(lp.quantidade), 0) AS quantidade_vendida,
                            COALESCE(SUM(lp.quantidade * lp.preco_unit_venda), 0) AS valor_total
                        FROM fatura f
                        JOIN pedido ped ON ped.id_pedido = f.id_pedido
                        JOIN linha_pedido lp ON lp.id_pedido = ped.id_pedido
                        JOIN produto p ON p.id_produto = lp.id_produto
                        WHERE f.data_hora >= :inicio AND f.data_hora < :fim
                        GROUP BY p.id_produto, p.nome, p.tipo
                        ORDER BY quantidade_vendida DESC, valor_total DESC
                        """)
                .setParameter("inicio", Timestamp.from(inicioPeriodo))
                .setParameter("fim", Timestamp.from(fimPeriodoExclusivo))
                .setMaxResults(TOP_PRODUTOS_LIMITE)
                .getResultList();

        List<TopProdutoDTO> topProdutos = new ArrayList<>();
        for (Object[] row : rows) {
            topProdutos.add(new TopProdutoDTO(
                    toInteger(row[0]),
                    toStringSafe(row[1]),
                    normalizarCategoria(row[2]),
                    toLong(row[3]),
                    toBigDecimal(row[4])
            ));
        }
        return topProdutos;
    }

    private List<CategoriaValorDTO> consultarVendasPorCategoria(Instant inicioPeriodo, Instant fimPeriodoExclusivo) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT
                            COALESCE(NULLIF(TRIM(p.tipo), ''), 'Sem categoria') AS categoria,
                            COALESCE(SUM(lp.quantidade * lp.preco_unit_venda), 0) AS valor_total
                        FROM fatura f
                        JOIN pedido ped ON ped.id_pedido = f.id_pedido
                        JOIN linha_pedido lp ON lp.id_pedido = ped.id_pedido
                        JOIN produto p ON p.id_produto = lp.id_produto
                        WHERE f.data_hora >= :inicio AND f.data_hora < :fim
                        GROUP BY COALESCE(NULLIF(TRIM(p.tipo), ''), 'Sem categoria')
                        ORDER BY valor_total DESC
                        """)
                .setParameter("inicio", Timestamp.from(inicioPeriodo))
                .setParameter("fim", Timestamp.from(fimPeriodoExclusivo))
                .getResultList();

        List<CategoriaValorDTO> categorias = new ArrayList<>();
        for (Object[] row : rows) {
            categorias.add(new CategoriaValorDTO(
                    normalizarCategoria(row[0]),
                    toBigDecimal(row[1])
            ));
        }
        return categorias;
    }

    private List<CategoriaValorDTO> consultarGastoStockPorCategoria() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT
                            COALESCE(NULLIF(TRIM(i.unidade), ''), 'Sem categoria') AS categoria,
                            COALESCE(SUM(COALESCE(s.quant, 0) * COALESCE(i.preco, 0)), 0) AS valor_total
                        FROM stock s
                        JOIN ingrediente i ON i.id_ingred = s.id_ingred
                        GROUP BY COALESCE(NULLIF(TRIM(i.unidade), ''), 'Sem categoria')
                        ORDER BY valor_total DESC
                        """)
                .getResultList();

        List<CategoriaValorDTO> gastos = new ArrayList<>();
        for (Object[] row : rows) {
            gastos.add(new CategoriaValorDTO(
                    normalizarCategoria(row[0]),
                    toBigDecimal(row[1])
            ));
        }
        return gastos;
    }

    private void validarIntervalo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de inicio e data de fim sao obrigatorias.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de fim nao pode ser anterior a data de inicio.");
        }
    }

    private String normalizarCategoria(Object value) {
        String texto = toStringSafe(value);
        if (texto.isBlank()) {
            return CATEGORIA_SEM_VALOR;
        }
        String trimmed = texto.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
