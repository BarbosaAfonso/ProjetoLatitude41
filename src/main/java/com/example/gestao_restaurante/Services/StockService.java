package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Ingrediente;
import com.example.gestao_restaurante.Modules.Stock;
import com.example.gestao_restaurante.Repositories.IngredienteRepository;
import com.example.gestao_restaurante.Repositories.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final IngredienteRepository ingredienteRepository;

    public StockService(StockRepository stockRepository, IngredienteRepository ingredienteRepository) {
        this.stockRepository = stockRepository;
        this.ingredienteRepository = ingredienteRepository;
    }

    public List<Stock> listarTodos() {
        return (List<Stock>) stockRepository.findAll();
    }

    public Optional<Stock> procurarPorId(Integer id) {
        return stockRepository.findById(id);
    }

    public Stock criar(Stock stock) {
        Ingrediente ingrediente = procurarIngrediente(stock);
        return stockRepository.findById(ingrediente.getId())
                .map(existente -> {
                    existente.setQuant(validarQuantidade(stock.getQuant()));
                    existente.setEstado(normalizarEstado(stock.getEstado(), "encomendado"));
                    return stockRepository.save(existente);
                })
                .orElseGet(() -> {
                    Stock novoStock = new Stock();
                    novoStock.setIngrediente(ingrediente);
                    novoStock.setQuant(validarQuantidade(stock.getQuant()));
                    novoStock.setEstado(normalizarEstado(stock.getEstado(), "encomendado"));
                    return stockRepository.save(novoStock);
                });
    }

    public Optional<Stock> atualizar(Integer id, Stock stockAtualizado) {
        return stockRepository.findById(id).map(stock -> {
            stock.setQuant(validarQuantidade(stockAtualizado.getQuant()));
            stock.setEstado(normalizarEstado(stockAtualizado.getEstado(), stock.getEstado()));

            Integer ingredienteId = stockAtualizado.getIngrediente() == null ? null : stockAtualizado.getIngrediente().getId();
            if (ingredienteId != null && !ingredienteId.equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ingrediente do stock nao pode ser alterado.");
            }

            return stockRepository.save(stock);
        });
    }

    public boolean apagar(Integer id) {
        if (stockRepository.existsById(id)) {
            stockRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private Ingrediente procurarIngrediente(Stock stock) {
        Integer ingredienteId = stock == null || stock.getIngrediente() == null ? null : stock.getIngrediente().getId();
        if (ingredienteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um ingrediente valido.");
        }

        return ingredienteRepository.findById(ingredienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrediente nao encontrado."));
    }

    private BigDecimal validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade do stock e obrigatoria.");
        }
        if (quantidade.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade do stock nao pode ser negativa.");
        }
        return quantidade;
    }

    private String normalizarEstado(String estado, String valorPadrao) {
        String valor = estado == null || estado.isBlank() ? valorPadrao : estado.trim();
        valor = valor.toLowerCase(Locale.ROOT);
        if (!valor.equals("encomendado") && !valor.equals("disponivel") && !valor.equals("esgotado")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de stock invalido.");
        }
        return valor;
    }
}
