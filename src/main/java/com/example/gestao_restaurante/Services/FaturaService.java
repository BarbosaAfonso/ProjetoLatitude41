package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Fatura;
import com.example.gestao_restaurante.Repositories.FaturaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FaturaService {

    private final FaturaRepository faturaRepository;

    public FaturaService(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
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
}
