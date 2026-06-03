package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Dtos.FaturaGeracaoResponse;
import com.example.gestao_restaurante.Modules.Fatura;
import com.example.gestao_restaurante.Modules.LinhaPedido;
import com.example.gestao_restaurante.Modules.Mesa;
import com.example.gestao_restaurante.Modules.Pedido;
import com.example.gestao_restaurante.Repositories.FaturaRepository;
import com.example.gestao_restaurante.Repositories.LinhaPedidoRepository;
import com.example.gestao_restaurante.Repositories.MesaRepository;
import com.example.gestao_restaurante.Repositories.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FaturaService {

    private static final BigDecimal TAXA_IVA = new BigDecimal("0.23");

    private final FaturaRepository faturaRepository;
    private final PedidoRepository pedidoRepository;
    private final LinhaPedidoRepository linhaPedidoRepository;
    private final MesaRepository mesaRepository;
    private final PdfService pdfService;

    public FaturaService(FaturaRepository faturaRepository,
                         PedidoRepository pedidoRepository,
                         LinhaPedidoRepository linhaPedidoRepository,
                         MesaRepository mesaRepository,
                         PdfService pdfService) {
        this.faturaRepository = faturaRepository;
        this.pedidoRepository = pedidoRepository;
        this.linhaPedidoRepository = linhaPedidoRepository;
        this.mesaRepository = mesaRepository;
        this.pdfService = pdfService;
    }

    // GET - listar todas as faturas
    public List<Fatura> listarTodos() {
        return (List<Fatura>) faturaRepository.findAll();
    }

    // GET - procurar fatura por id
    public Optional<Fatura> procurarPorId(Integer id) {
        return faturaRepository.findById(id);
    }

    // POST - criar nova fatura
    public Fatura criar(Fatura fatura) {
        return faturaRepository.save(fatura);
    }

    // PUT - atualizar fatura existente
    public Optional<Fatura> atualizar(Integer id, Fatura faturaAtualizada) {
        return faturaRepository.findById(id).map(fatura -> {
            fatura.setValor(faturaAtualizada.getValor());
            fatura.setMetodo(faturaAtualizada.getMetodo());
            fatura.setDataHora(faturaAtualizada.getDataHora());
            fatura.setIdPedido(faturaAtualizada.getIdPedido());
            return faturaRepository.save(fatura);
        });
    }

    // DELETE - apagar fatura por id
    public boolean apagar(Integer id) {
        if (faturaRepository.existsById(id)) {
            faturaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public FaturaGeracaoResponse gerarFatura(Long pedidoId, String metodoPagamento) {
        Integer pedidoIdInt = validarPedidoId(pedidoId);
        String metodo = validarMetodoPagamento(metodoPagamento);

        Pedido pedido = pedidoRepository.findById(pedidoIdInt)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado."));

        if ("PAGO".equalsIgnoreCase(normalizarTexto(pedido.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido ja se encontra pago.");
        }

        if (faturaRepository.findByIdPedidoId(pedidoIdInt).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe uma fatura associada a este pedido.");
        }

        List<LinhaPedido> linhas = linhaPedidoRepository.findByIdIdPedidoOrderByIdIdProdutoAsc(pedidoIdInt);
        if (linhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao e possivel fechar conta sem linhas de pedido.");
        }

        BigDecimal subtotal = calcularSubtotal(linhas);
        BigDecimal iva = subtotal.multiply(TAXA_IVA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

        Fatura fatura = new Fatura();
        fatura.setIdPedido(pedido);
        fatura.setMetodo(metodo);
        fatura.setValor(total);
        fatura.setDataHora(Instant.now());
        Fatura faturaGuardada = faturaRepository.save(fatura);

        pedido.setEstado("PAGO");
        pedidoRepository.save(pedido);

        Mesa mesaAssociada = pedido.getIdReserva() == null ? null : pedido.getIdReserva().getNumMesa();
        if (mesaAssociada != null) {
            mesaAssociada.setEstado("LIVRE");
            mesaRepository.save(mesaAssociada);
        }

        Path caminhoPdf = pdfService.gerarPdfFatura(
                faturaGuardada.getId(),
                pedido.getId(),
                linhas,
                subtotal,
                iva,
                total
        );

        FaturaGeracaoResponse response = new FaturaGeracaoResponse();
        response.setFaturaId(faturaGuardada.getId());
        response.setPedidoId(pedido.getId());
        response.setMetodoPagamento(metodo);
        response.setSubtotal(subtotal);
        response.setIva(iva);
        response.setTotal(total);
        response.setDataHora(faturaGuardada.getDataHora());
        response.setCaminhoPdf(caminhoPdf.toAbsolutePath().toString());
        return response;
    }

    private Integer validarPedidoId(Long pedidoId) {
        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido invalido para fecho de conta.");
        }
        try {
            return Math.toIntExact(pedidoId);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de pedido invalido.");
        }
    }

    private String validarMetodoPagamento(String metodoPagamento) {
        String metodo = normalizarTexto(metodoPagamento);
        return switch (metodo) {
            case "DINHEIRO" -> "Dinheiro";
            case "MULTIBANCO" -> "Multibanco";
            case "MBWAY" -> "MBWay";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metodo de pagamento invalido.");
        };
    }

    private BigDecimal calcularSubtotal(List<LinhaPedido> linhas) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LinhaPedido linha : linhas) {
            int quantidade = linha.getQuantidade() == null ? 0 : linha.getQuantidade();
            BigDecimal precoUnitario = linha.getPrecoUnitVenda() == null ? BigDecimal.ZERO : linha.getPrecoUnitVenda();
            subtotal = subtotal.add(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
