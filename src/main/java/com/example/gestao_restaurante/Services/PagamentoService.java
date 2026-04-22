package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Pagamento;
import com.example.gestao_restaurante.Repositories.PagamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
    }

    // GET - listar todos os pagamentos
    public List<Pagamento> listarTodos() {
        return (List<Pagamento>) pagamentoRepository.findAll();
    }

    // GET - procurar pagamento por id
    public Optional<Pagamento> procurarPorId(Integer id) {
        return pagamentoRepository.findById(id);
    }

    // POST - criar novo pagamento
    public Pagamento criar(Pagamento pagamento) {
        return pagamentoRepository.save(pagamento);
    }

    // PUT - atualizar pagamento existente
    public Optional<Pagamento> atualizar(Integer id, Pagamento pagamentoAtualizado) {
        return pagamentoRepository.findById(id).map(pagamento -> {
            pagamento.setValor(pagamentoAtualizado.getValor());
            pagamento.setIdPedido(pagamentoAtualizado.getIdPedido());
            return pagamentoRepository.save(pagamento);
        });
    }

    // DELETE - apagar pagamento por id
    public boolean apagar(Integer id) {
        if (pagamentoRepository.existsById(id)) {
            pagamentoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
