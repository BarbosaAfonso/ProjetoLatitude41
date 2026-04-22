package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Dtos.PedidoCompletoRequest;
import com.example.gestao_restaurante.Dtos.PedidoCompletoResponse;
import com.example.gestao_restaurante.Dtos.PedidoLinhaRequest;
import com.example.gestao_restaurante.Dtos.PedidoLinhaResumo;
import com.example.gestao_restaurante.Modules.LinhaPedido;
import com.example.gestao_restaurante.Modules.LinhaPedidoId;
import com.example.gestao_restaurante.Modules.Pedido;
import com.example.gestao_restaurante.Modules.Produto;
import com.example.gestao_restaurante.Modules.Reserva;
import com.example.gestao_restaurante.Repositories.LinhaPedidoRepository;
import com.example.gestao_restaurante.Repositories.PedidoRepository;
import com.example.gestao_restaurante.Repositories.ProdutoRepository;
import com.example.gestao_restaurante.Repositories.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ReservaRepository reservaRepository;
    private final ProdutoRepository produtoRepository;
    private final LinhaPedidoRepository linhaPedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ReservaRepository reservaRepository,
                         ProdutoRepository produtoRepository,
                         LinhaPedidoRepository linhaPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.reservaRepository = reservaRepository;
        this.produtoRepository = produtoRepository;
        this.linhaPedidoRepository = linhaPedidoRepository;
    }

    public List<Pedido> listarTodos() {
        return (List<Pedido>) pedidoRepository.findAll();
    }

    public Optional<Pedido> procurarPorId(Integer id) {
        return pedidoRepository.findById(id);
    }

    public Pedido criar(Pedido pedido) {
        Pedido novoPedido = new Pedido();
        aplicarDados(novoPedido, pedido, false, null);
        return pedidoRepository.save(novoPedido);
    }

    public Optional<Pedido> atualizar(Integer id, Pedido pedidoAtualizado) {
        return pedidoRepository.findById(id).map(pedido -> {
            aplicarDados(pedido, pedidoAtualizado, true, pedido.getDataHora());
            return pedidoRepository.save(pedido);
        });
    }

    public boolean apagar(Integer id) {
        if (pedidoRepository.existsById(id)) {
            pedidoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public PedidoCompletoResponse criarCompleto(PedidoCompletoRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido invalido.");
        }
        if (request.getReservaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma reserva valida.");
        }
        if (request.getLinhas() == null || request.getLinhas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adicione pelo menos um produto ao pedido.");
        }

        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva nao encontrada."));

        Pedido pedido = new Pedido();
        pedido.setDataHora(request.getDataHora() == null ? Instant.now() : request.getDataHora());
        pedido.setEstado(normalizarEstado(request.getEstado(), "PREPARACAO"));
        validarDataHora(pedido.getDataHora(), pedido.getEstado(), null);
        pedido.setIdReserva(reserva);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        List<PedidoLinhaResumo> linhasResumo = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int quantidadeItens = 0;

        for (PedidoLinhaRequest linhaRequest : request.getLinhas()) {
            Produto produto = procurarProduto(linhaRequest);
            int quantidade = validarQuantidade(linhaRequest);
            BigDecimal precoUnitario = validarPrecoUnitario(linhaRequest, produto);
            BigDecimal subtotalLinha = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

            LinhaPedido linhaPedido = new LinhaPedido();
            LinhaPedidoId id = new LinhaPedidoId();
            id.setIdPedido(pedidoGuardado.getId());
            id.setIdProduto(produto.getId());

            linhaPedido.setId(id);
            linhaPedido.setIdPedido(pedidoGuardado);
            linhaPedido.setIdProduto(produto);
            linhaPedido.setQuantidade(quantidade);
            linhaPedido.setPrecoUnitVenda(precoUnitario);
            linhaPedido.setObservacoes(normalizarObservacoes(linhaRequest.getObservacoes()));
            linhaPedidoRepository.save(linhaPedido);

            PedidoLinhaResumo resumo = new PedidoLinhaResumo();
            resumo.setProdutoId(produto.getId());
            resumo.setNomeProduto(produto.getNome());
            resumo.setTipoProduto(produto.getTipo());
            resumo.setQuantidade(quantidade);
            resumo.setPrecoUnitVenda(precoUnitario);
            resumo.setObservacoes(linhaPedido.getObservacoes());
            resumo.setSubtotal(subtotalLinha);
            linhasResumo.add(resumo);

            subtotal = subtotal.add(subtotalLinha);
            quantidadeItens += quantidade;
        }

        PedidoCompletoResponse response = new PedidoCompletoResponse();
        response.setId(pedidoGuardado.getId());
        response.setDataHora(pedidoGuardado.getDataHora());
        response.setEstado(pedidoGuardado.getEstado());
        response.setReservaId(reserva.getId());
        response.setMesaId(reserva.getNumMesa() == null ? null : reserva.getNumMesa().getId());
        response.setQuantidadeItens(quantidadeItens);
        response.setSubtotal(subtotal);
        response.setLinhas(linhasResumo);
        return response;
    }

    private void aplicarDados(Pedido destino, Pedido origem, boolean preservarReservaAtual, Instant dataHoraAnterior) {
        if (origem == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido invalido.");
        }

        destino.setDataHora(origem.getDataHora() == null ? Instant.now() : origem.getDataHora());
        String estado = normalizarEstado(origem.getEstado(), "REGISTADO");
        validarDataHora(destino.getDataHora(), estado, dataHoraAnterior);
        destino.setEstado(estado);
        destino.setIdReserva(procurarReserva(origem, destino, preservarReservaAtual));
    }

    private Reserva procurarReserva(Pedido pedido, Pedido pedidoAtual, boolean preservarReservaAtual) {
        Integer reservaId = pedido.getIdReserva() == null ? null : pedido.getIdReserva().getId();
        if (reservaId == null && preservarReservaAtual && pedidoAtual.getIdReserva() != null) {
            return pedidoAtual.getIdReserva();
        }

        if (reservaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma reserva valida.");
        }

        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva nao encontrada."));
    }

    private String normalizarEstado(String estado, String valorPadrao) {
        String valor = estado == null || estado.isBlank() ? valorPadrao : estado.trim();
        return valor.toUpperCase(Locale.ROOT);
    }

    private Produto procurarProduto(PedidoLinhaRequest linhaRequest) {
        Integer produtoId = linhaRequest == null ? null : linhaRequest.getProdutoId();
        if (produtoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto invalido no pedido.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto nao encontrado."));

        if (Boolean.FALSE.equals(produto.getDisponivel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O produto " + produto.getNome() + " nao esta disponivel.");
        }
        return produto;
    }

    private int validarQuantidade(PedidoLinhaRequest linhaRequest) {
        Integer quantidade = linhaRequest == null ? null : linhaRequest.getQuantidade();
        if (quantidade == null || quantidade <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade de cada produto tem de ser superior a zero.");
        }
        return quantidade;
    }

    private BigDecimal validarPrecoUnitario(PedidoLinhaRequest linhaRequest, Produto produto) {
        BigDecimal preco = linhaRequest == null ? null : linhaRequest.getPrecoUnitVenda();
        BigDecimal valor = preco == null ? produto.getPreco() : preco;
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preco unitario invalido.");
        }
        return valor;
    }

    private String normalizarObservacoes(String observacoes) {
        if (observacoes == null) {
            return null;
        }
        String valor = observacoes.trim();
        return valor.isBlank() ? null : valor;
    }

    private void validarDataHora(Instant dataHora, String estado, Instant dataHoraAnterior) {
        if (dataHora == null) {
            return;
        }
        boolean dataMantida = dataHoraAnterior != null && dataHoraAnterior.equals(dataHora);
        if (dataHora.isBefore(Instant.now()) && !"CANCELADO".equalsIgnoreCase(estado) && !dataMantida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao pode criar ou alterar pedidos para datas passadas.");
        }
    }
}
