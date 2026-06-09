package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Dtos.ProdutoRequestDTO;
import com.example.gestao_restaurante.Dtos.ProdutoResponseDTO;
import com.example.gestao_restaurante.Modules.Produto;
import com.example.gestao_restaurante.Repositories.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<ProdutoResponseDTO> listarTodos() {
        List<ProdutoResponseDTO> produtos = new ArrayList<>();
        produtoRepository.findAll().forEach(produto -> produtos.add(toResponse(produto)));
        return produtos;
    }

    public Optional<ProdutoResponseDTO> procurarPorId(Integer id) {
        return produtoRepository.findById(id).map(this::toResponse);
    }

    public ProdutoResponseDTO criar(ProdutoRequestDTO request) {
        Produto produto = new Produto();
        aplicarRequest(produto, request);
        return toResponse(produtoRepository.save(produto));
    }

    public Optional<ProdutoResponseDTO> atualizar(Integer id, ProdutoRequestDTO request) {
        return produtoRepository.findById(id).map(produto -> {
            aplicarRequest(produto, request);
            return toResponse(produtoRepository.save(produto));
        });
    }

    public boolean apagar(Integer id) {
        if (produtoRepository.existsById(id)) {
            produtoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void aplicarRequest(Produto produto, ProdutoRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Produto invalido.");
        }

        produto.setNome(normalizarTextoObrigatorio(request.getNome(), "nome"));
        produto.setTipo(normalizarTextoObrigatorio(request.getTipo(), "tipo"));
        produto.setPreco(validarPreco(request.getPreco()));
        produto.setDisponivel(request.getDisponivel() == null ? Boolean.TRUE : request.getDisponivel());
        produto.setUrlImagem(normalizarTextoOpcional(request.getUrlImagem()));
        produto.setVegetariano(booleanOrFalse(request.getVegetariano()));
        produto.setSemGluten(booleanOrFalse(request.getSemGluten()));
        produto.setSemLactose(booleanOrFalse(request.getSemLactose()));
    }

    private ProdutoResponseDTO toResponse(Produto produto) {
        ProdutoResponseDTO response = new ProdutoResponseDTO();
        response.setId(produto.getId());
        response.setNome(produto.getNome());
        response.setTipo(produto.getTipo());
        response.setPreco(produto.getPreco());
        response.setDisponivel(produto.getDisponivel() == null ? Boolean.TRUE : produto.getDisponivel());
        response.setUrlImagem(produto.getUrlImagem());
        response.setVegetariano(booleanOrFalse(produto.getVegetariano()));
        response.setSemGluten(booleanOrFalse(produto.getSemGluten()));
        response.setSemLactose(booleanOrFalse(produto.getSemLactose()));
        return response;
    }

    private BigDecimal validarPreco(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preco deve ser maior que zero.");
        }

        return preco;
    }

    private Boolean booleanOrFalse(Boolean valor) {
        return Boolean.TRUE.equals(valor);
    }

    private String normalizarTextoObrigatorio(String valor, String campo) {
        String texto = normalizarTextoOpcional(valor);
        if (texto == null) {
            throw new IllegalArgumentException("Campo obrigatorio: " + campo + ".");
        }

        return texto;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
