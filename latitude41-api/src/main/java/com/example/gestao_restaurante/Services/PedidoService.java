package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Dtos.PedidoCompletoRequest;
import com.example.gestao_restaurante.Dtos.PedidoCompletoResponse;
import com.example.gestao_restaurante.Dtos.PedidoLinhaRequest;
import com.example.gestao_restaurante.Dtos.PedidoLinhaResumo;
import com.example.gestao_restaurante.Modules.LinhaPedido;
import com.example.gestao_restaurante.Modules.LinhaPedidoId;
import com.example.gestao_restaurante.Modules.Mesa;
import com.example.gestao_restaurante.Modules.Pedido;
import com.example.gestao_restaurante.Modules.Produto;
import com.example.gestao_restaurante.Modules.Reserva;
import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Repositories.LinhaPedidoRepository;
import com.example.gestao_restaurante.Repositories.MesaRepository;
import com.example.gestao_restaurante.Repositories.PedidoRepository;
import com.example.gestao_restaurante.Repositories.ProdutoRepository;
import com.example.gestao_restaurante.Repositories.ReservaRepository;
import com.example.gestao_restaurante.Repositories.UtilizadorRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PedidoService {

    private static final long MARGEM_DATA_HORA_SEGUNDOS = 30;
    private static final String ESTADO_PEDIDO_REGISTADO = "REGISTADO";
    private static final Set<String> ESTADOS_PEDIDO_ATIVOS = Set.of(
            ESTADO_PEDIDO_REGISTADO,
            "ABERTO",
            "PRONTO",
            "EM PREPARACAO",
            "EM_PREPARACAO",
            "PREPARACAO"
    );
    private static final Set<String> ESTADOS_RESERVA_ATIVOS = Set.of("CONFIRMADA", "OCUPADA", "EM_CURSO", "ATIVA");
    private static final Set<String> ESTADOS_MESA_PERMITIDOS = Set.of("LIVRE", "RESERVADA", "OCUPADA");
    private static final long JANELA_RESERVA_ATUAL_MINUTOS = 120;
    private static final Pattern PATTERN_CONSTRAINT = Pattern.compile("constraint\\s+\"?([\\w\\d_]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final int LIMITE_DETALHE_ERRO = 220;

    private final PedidoRepository pedidoRepository;
    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ProdutoRepository produtoRepository;
    private final LinhaPedidoRepository linhaPedidoRepository;
    private final EntityManager entityManager;

    public PedidoService(PedidoRepository pedidoRepository,
                         ReservaRepository reservaRepository,
                         MesaRepository mesaRepository,
                         UtilizadorRepository utilizadorRepository,
                         ProdutoRepository produtoRepository,
                         LinhaPedidoRepository linhaPedidoRepository,
                         EntityManager entityManager) {
        this.pedidoRepository = pedidoRepository;
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.produtoRepository = produtoRepository;
        this.linhaPedidoRepository = linhaPedidoRepository;
        this.entityManager = entityManager;
    }

    public List<Pedido> listarTodos() {
        return (List<Pedido>) pedidoRepository.findAll();
    }

    public Optional<Pedido> procurarPorId(Integer id) {
        return pedidoRepository.findById(id);
    }

    public List<PedidoCompletoResponse> listarCompletosPorMesa(Integer mesaId) {
        Mesa mesa = procurarMesa(mesaId);
        return pedidoRepository.findByIdReservaNumMesaIdAndEstadoInOrderByDataHoraDesc(
                        mesa.getId(),
                        ESTADOS_PEDIDO_ATIVOS
                ).stream()
                .map(this::construirRespostaCompleta)
                .toList();
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
        try {
            if (request == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido invalido.");
            }
            if (request.getLinhas() == null || request.getLinhas().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adicione pelo menos um produto ao pedido.");
            }

            Integer mesaId = resolverMesaId(request);
            Mesa mesa = procurarMesa(mesaId);
            validarMesaParaPedido(mesa);
            Reserva reserva = resolverReservaParaPedido(request, mesa);

            Pedido pedidoGuardado = procurarPedidoAtivoOuCriarPorMesa(mesa.getId(), reserva, request);

            int indiceLinha = 0;
            for (PedidoLinhaRequest linhaRequest : request.getLinhas()) {
                indiceLinha++;
                if (linhaRequest == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linha " + indiceLinha + " do pedido esta vazia.");
                }

                Produto produto = procurarProduto(linhaRequest);
                int quantidade = validarQuantidade(linhaRequest);
                BigDecimal precoUnitario = validarPrecoUnitario(linhaRequest, produto);
                String observacoes = normalizarObservacoes(linhaRequest.getObservacoes());
                adicionarOuAtualizarLinha(pedidoGuardado, produto, quantidade, precoUnitario, observacoes);
            }

            // Forca validacao imediata de FK/UNIQUE no fim da transacao de negocio.
            entityManager.flush();

            return construirRespostaCompleta(pedidoGuardado);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    resolverMensagemConflitoIntegridade(e),
                    e
            );
        } catch (RuntimeException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha interna ao processar o pedido completo. " + resumirDetalheErro(e),
                    e
            );
        }
    }

    private Pedido procurarPedidoAtivoOuCriarPorMesa(Integer mesaId, Reserva reserva, PedidoCompletoRequest request) {
        Optional<Pedido> pedidoAtivo = pedidoRepository.findTopByMesaIdAndStatusInOrderByDataHoraDesc(
                mesaId,
                ESTADOS_PEDIDO_ATIVOS
        );
        if (pedidoAtivo.isPresent()) {
            return pedidoAtivo.get();
        }

        // Fallback para cenarios em que a BD imponha unicidade por reserva.
        Optional<Pedido> ultimoPedidoReserva = pedidoRepository.findFirstByIdReservaIdOrderByDataHoraDesc(reserva.getId());
        if (ultimoPedidoReserva.isPresent()) {
            Pedido existente = ultimoPedidoReserva.get();
            if (!"CANCELADO".equalsIgnoreCase(normalizarEstado(existente.getEstado(), ""))) {
                return existente;
            }
        }

        Pedido pedido = new Pedido();
        pedido.setDataHora(request.getDataHora() == null ? Instant.now() : request.getDataHora());
        pedido.setEstado(normalizarEstado(request.getEstado(), ESTADO_PEDIDO_REGISTADO));
        validarDataHora(pedido.getDataHora(), pedido.getEstado(), null);
        pedido.setIdReserva(reserva);
        return pedidoRepository.save(pedido);
    }

    private Integer resolverMesaId(PedidoCompletoRequest request) {
        if (request.getMesaId() != null) {
            return request.getMesaId();
        }
        if (request.getReservaId() != null) {
            Reserva reserva = reservaRepository.findById(request.getReservaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva nao encontrada."));
            if (reserva.getNumMesa() == null || reserva.getNumMesa().getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reserva selecionada nao esta associada a uma mesa valida.");
            }
            return reserva.getNumMesa().getId();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma mesa valida.");
    }

    private Mesa procurarMesa(Integer mesaId) {
        return mesaRepository.findById(mesaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mesa nao encontrada."));
    }

    private Reserva resolverReservaParaPedido(PedidoCompletoRequest request, Mesa mesa) {
        if (request.getReservaId() != null) {
            Reserva reserva = reservaRepository.findById(request.getReservaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva nao encontrada."));
            validarReservaParaPedido(reserva, mesa.getId());
            return reserva;
        }

        Optional<Reserva> reservaAtiva = procurarReservaVigentePorMesa(mesa.getId());
        if (reservaAtiva.isPresent()) {
            validarReservaParaPedido(reservaAtiva.get(), mesa.getId());
            return reservaAtiva.get();
        }

        return criarReservaDeServico(mesa, request.getUtilizadorId());
    }

    private Optional<Reserva> procurarReservaVigentePorMesa(Integer mesaId) {
        Instant agora = Instant.now();
        return reservaRepository.findByNumMesaIdOrderByDataHoraDesc(mesaId).stream()
                .filter(this::isReservaAtiva)
                .filter(reserva -> isReservaDaHoraAtual(reserva, agora))
                .min(Comparator.comparingLong(reserva -> Math.abs(Duration.between(agora, reserva.getDataHora()).toMinutes())));
    }

    private boolean isReservaAtiva(Reserva reserva) {
        if (reserva == null) {
            return false;
        }
        String estadoReserva = normalizarEstado(reserva.getEstado(), "");
        return ESTADOS_RESERVA_ATIVOS.contains(estadoReserva);
    }

    private boolean isReservaDaHoraAtual(Reserva reserva, Instant agora) {
        if (reserva == null || reserva.getDataHora() == null) {
            return false;
        }
        long diferencaMinutos = Math.abs(Duration.between(agora, reserva.getDataHora()).toMinutes());
        return diferencaMinutos <= JANELA_RESERVA_ATUAL_MINUTOS;
    }

    private Reserva criarReservaDeServico(Mesa mesa, Integer utilizadorId) {
        Utilizador utilizador = resolverUtilizadorPedido(utilizadorId);

        Reserva reserva = new Reserva();
        reserva.setDataHora(Instant.now());
        reserva.setEstado("CONFIRMADA");
        reserva.setNumMesa(mesa);
        reserva.setIdUtilizador(utilizador);
        return reservaRepository.save(reserva);
    }

    private Utilizador resolverUtilizadorPedido(Integer utilizadorId) {
        if (utilizadorId != null) {
            return utilizadorRepository.findById(utilizadorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizador nao encontrado para associar ao pedido."));
        }

        for (Utilizador utilizador : utilizadorRepository.findAll()) {
            if (utilizador != null && utilizador.getId() != null) {
                return utilizador;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao existe utilizador disponivel para criar pedido sem reserva.");
    }

    private void adicionarOuAtualizarLinha(Pedido pedido,
                                           Produto produto,
                                           int quantidade,
                                           BigDecimal precoUnitario,
                                           String observacoes) {
        LinhaPedidoId id = new LinhaPedidoId();
        id.setIdPedido(pedido.getId());
        id.setIdProduto(produto.getId());

        Optional<LinhaPedido> linhaExistenteOpt = linhaPedidoRepository.findById(id);
        if (linhaExistenteOpt.isPresent()) {
            LinhaPedido existente = linhaExistenteOpt.get();
            int quantidadeAtual = existente.getQuantidade() == null ? 0 : existente.getQuantidade();
            existente.setQuantidade(quantidadeAtual + quantidade);
            existente.setPrecoUnitVenda(precoUnitario);
            existente.setObservacoes(agregarObservacoes(existente.getObservacoes(), observacoes));
            linhaPedidoRepository.save(existente);
            return;
        }

        LinhaPedido linhaPedido = new LinhaPedido();
        linhaPedido.setId(id);
        linhaPedido.setIdPedido(pedido);
        linhaPedido.setIdProduto(produto);
        linhaPedido.setQuantidade(quantidade);
        linhaPedido.setPrecoUnitVenda(precoUnitario);
        linhaPedido.setObservacoes(observacoes);
        linhaPedidoRepository.save(linhaPedido);
    }

    private PedidoLinhaResumo paraResumo(LinhaPedido linhaPedido) {
        Produto produto = linhaPedido.getIdProduto();
        Integer produtoId = produto == null ? linhaPedido.getId().getIdProduto() : produto.getId();
        String nomeProduto = produto == null ? null : produto.getNome();
        String tipoProduto = produto == null ? null : produto.getTipo();
        Integer quantidade = linhaPedido.getQuantidade() == null ? 0 : linhaPedido.getQuantidade();
        BigDecimal precoUnitario = linhaPedido.getPrecoUnitVenda() == null ? BigDecimal.ZERO : linhaPedido.getPrecoUnitVenda();

        PedidoLinhaResumo resumo = new PedidoLinhaResumo();
        resumo.setProdutoId(produtoId);
        resumo.setNomeProduto(normalizarNomeProduto(nomeProduto, produtoId));
        resumo.setTipoProduto(normalizarTipoProduto(tipoProduto));
        resumo.setQuantidade(quantidade);
        resumo.setPrecoUnitVenda(precoUnitario);
        resumo.setObservacoes(linhaPedido.getObservacoes());
        resumo.setSubtotal(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
        return resumo;
    }

    private PedidoCompletoResponse construirRespostaCompleta(Pedido pedido) {
        List<LinhaPedido> linhasPersistidas = linhaPedidoRepository.findByIdIdPedidoOrderByIdIdProdutoAsc(pedido.getId());
        List<PedidoLinhaResumo> linhasResumo = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int quantidadeItens = 0;

        for (LinhaPedido linhaPersistida : linhasPersistidas) {
            PedidoLinhaResumo resumo = paraResumo(linhaPersistida);
            linhasResumo.add(resumo);
            subtotal = subtotal.add(resumo.getSubtotal());
            quantidadeItens += resumo.getQuantidade() == null ? 0 : resumo.getQuantidade();
        }

        PedidoCompletoResponse response = new PedidoCompletoResponse();
        response.setId(pedido.getId());
        response.setDataHora(pedido.getDataHora());
        response.setEstado(pedido.getEstado());
        response.setReservaId(pedido.getIdReserva() == null ? null : pedido.getIdReserva().getId());
        response.setMesaId(pedido.getIdReserva() == null || pedido.getIdReserva().getNumMesa() == null
                ? null
                : pedido.getIdReserva().getNumMesa().getId());
        response.setQuantidadeItens(quantidadeItens);
        response.setSubtotal(subtotal);
        response.setLinhas(linhasResumo);
        return response;
    }

    private String agregarObservacoes(String existentes, String novas) {
        String base = normalizarObservacoes(existentes);
        String extra = normalizarObservacoes(novas);

        if (base == null) {
            return extra;
        }
        if (extra == null) {
            return base;
        }
        if (base.equals(extra)) {
            return base;
        }
        return base + " | " + extra;
    }

    private String resolverMensagemConflitoIntegridade(DataIntegrityViolationException e) {
        String detalhe = extrairDetalheIntegridade(e);
        String detalheNormalizado = detalhe.toLowerCase(Locale.ROOT);
        String constraint = extrairNomeConstraint(detalhe);

        if (detalheNormalizado.contains("linha_pedido")
                && (detalheNormalizado.contains("id_pedido") || detalheNormalizado.contains("id_produto"))) {
            return montarMensagemComConstraint(
                    "Conflito ao anexar produtos ao pedido existente. Tente novamente para consolidar as linhas do pedido.",
                    constraint
            );
        }

        if (detalheNormalizado.contains("pedido")
                && (detalheNormalizado.contains("id_reserva") || detalheNormalizado.contains("reserva"))) {
            return montarMensagemComConstraint(
                    "Ja existe um pedido associado a esta mesa/reserva. O sistema deve reutilizar o pedido ativo para anexar novas linhas.",
                    constraint
            );
        }

        return montarMensagemComConstraint(
                "Conflito de dados ao gravar o pedido. A reserva/mesa ou algum produto pode ter sido alterado por outro utilizador.",
                constraint
        );
    }

    private String extrairDetalheIntegridade(DataIntegrityViolationException e) {
        Throwable causa = e.getMostSpecificCause();
        if (causa != null && causa.getMessage() != null && !causa.getMessage().isBlank()) {
            return causa.getMessage();
        }
        return e.getMessage() == null ? "" : e.getMessage();
    }

    private String extrairNomeConstraint(String detalhe) {
        if (detalhe == null || detalhe.isBlank()) {
            return "";
        }
        Matcher matcher = PATTERN_CONSTRAINT.matcher(detalhe);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String montarMensagemComConstraint(String mensagem, String constraint) {
        if (constraint == null || constraint.isBlank()) {
            return mensagem;
        }
        return mensagem + " (constraint: " + constraint + ")";
    }

    private String resumirDetalheErro(Throwable throwable) {
        Throwable causa = throwable;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }

        String mensagem = causa.getMessage();
        if (mensagem == null || mensagem.isBlank()) {
            mensagem = throwable.getMessage();
        }
        if (mensagem == null || mensagem.isBlank()) {
            return "Verifique a reserva selecionada e as linhas do pedido.";
        }

        String detalhe = mensagem.replaceAll("\\s+", " ").trim();
        if (detalhe.length() > LIMITE_DETALHE_ERRO) {
            detalhe = detalhe.substring(0, LIMITE_DETALHE_ERRO) + "...";
        }
        return detalhe;
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

    private void validarReservaParaPedido(Reserva reserva, Integer mesaId) {
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva invalida.");
        }
        if (reserva.getNumMesa() == null || reserva.getNumMesa().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reserva selecionada nao esta associada a uma mesa valida.");
        }
        if (!Objects.equals(reserva.getNumMesa().getId(), mesaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reserva selecionada nao pertence a mesa indicada.");
        }
        if ("CANCELADA".equalsIgnoreCase(reserva.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nao e possivel criar pedidos para reservas canceladas.");
        }
    }

    private void validarMesaParaPedido(Mesa mesa) {
        String estadoMesa = normalizarEstado(mesa == null ? null : mesa.getEstado(), "");
        if (!ESTADOS_MESA_PERMITIDOS.contains(estadoMesa)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Apenas e permitido lancar pedidos para mesas LIVRE, RESERVADA ou OCUPADA."
            );
        }
    }

    private String normalizarTipoProduto(String tipoProduto) {
        if (tipoProduto == null || tipoProduto.isBlank()) {
            return "Sem categoria";
        }
        return tipoProduto.trim();
    }

    private String normalizarNomeProduto(String nomeProduto, Integer produtoId) {
        if (nomeProduto == null || nomeProduto.isBlank()) {
            return "Produto #" + produtoId;
        }
        return nomeProduto.trim();
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
        Instant limitePassado = Instant.now().minusSeconds(MARGEM_DATA_HORA_SEGUNDOS);
        if (dataHora.isBefore(limitePassado) && !"CANCELADO".equalsIgnoreCase(estado) && !dataMantida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao pode criar ou alterar pedidos para datas passadas.");
        }
    }
}
