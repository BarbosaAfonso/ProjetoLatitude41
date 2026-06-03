package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Ingrediente;
import com.example.gestao_restaurante.Modules.Stock;
import com.example.gestao_restaurante.Repositories.IngredienteRepository;
import com.example.gestao_restaurante.Repositories.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class IngredienteService {

    private final IngredienteRepository ingredienteRepository;
    private final StockRepository stockRepository;

    public IngredienteService(IngredienteRepository ingredienteRepository, StockRepository stockRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.stockRepository = stockRepository;
    }

    // GET - listar todos os ingredientes
    public List<Ingrediente> listarTodos() {
        return (List<Ingrediente>) ingredienteRepository.findAll();
    }

    // GET - procurar ingrediente por id
    public Optional<Ingrediente> procurarPorId(Integer id) {
        return ingredienteRepository.findById(id);
    }

    // POST - criar novo ingrediente
    @Transactional
    public Ingrediente criar(Ingrediente ingrediente) {
        Ingrediente novoIngrediente = new Ingrediente();
        aplicarDados(novoIngrediente, ingrediente);

        Ingrediente guardado = ingredienteRepository.save(novoIngrediente);
        if (!stockRepository.existsById(guardado.getId())) {
            Stock stock = new Stock();
            stock.setIngrediente(guardado);
            stock.setQuant(BigDecimal.ZERO);
            stock.setEstado("disponivel");
            stockRepository.save(stock);
        }

        return guardado;
    }

    // PUT - atualizar ingrediente existente
    @Transactional
    public Optional<Ingrediente> atualizar(Integer id, Ingrediente ingredienteAtualizado) {
        return ingredienteRepository.findById(id).map(ingrediente -> {
            aplicarDados(ingrediente, ingredienteAtualizado);
            return ingredienteRepository.save(ingrediente);
        });
    }

    // DELETE - apagar ingrediente por id
    @Transactional
    public boolean apagar(Integer id) {
        if (ingredienteRepository.existsById(id)) {
            if (stockRepository.existsById(id)) {
                stockRepository.deleteById(id);
            }
            ingredienteRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void aplicarDados(Ingrediente destino, Ingrediente origem) {
        if (origem == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrediente invalido.");
        }

        destino.setNome(validarNome(origem.getNome()));
        destino.setUnidade(normalizarUnidade(origem.getUnidade()));
        destino.setPreco(origem.getPreco() == null ? BigDecimal.ZERO : origem.getPreco());
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do ingrediente e obrigatorio.");
        }
        return nome.trim();
    }

    private String normalizarUnidade(String unidade) {
        if (unidade == null || unidade.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade do ingrediente e obrigatoria.");
        }

        String valor = unidade.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case "KG/G", "KG", "G" -> "KG/G";
            case "L", "LITRO", "LITROS" -> "L";
            case "UN", "UNIDADE", "UNIDADES" -> "UNIDADE";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidade do ingrediente invalida.");
        };
    }
}
