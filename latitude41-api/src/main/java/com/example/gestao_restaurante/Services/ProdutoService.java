package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Produto;
import com.example.gestao_restaurante.Repositories.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    // GET - listar todos os produtos
    public List<Produto> listarTodos() {
        return (List<Produto>) produtoRepository.findAll();
    }

    // GET - procurar produto por id
    public Optional<Produto> procurarPorId(Integer id) {
        return produtoRepository.findById(id);
    }

    // POST - criar novo produto
    public Produto criar(Produto produto) {
        return produtoRepository.save(produto);
    }

    // PUT - atualizar produto existente
    public Optional<Produto> atualizar(Integer id, Produto produtoAtualizado) {
        return produtoRepository.findById(id).map(produto -> {
            produto.setNome(produtoAtualizado.getNome());
            produto.setTipo(produtoAtualizado.getTipo());
            produto.setPreco(produtoAtualizado.getPreco());
            produto.setDisponivel(produtoAtualizado.getDisponivel());
            return produtoRepository.save(produto);
        });
    }

    // DELETE - apagar produto por id
    public boolean apagar(Integer id) {
        if (produtoRepository.existsById(id)) {
            produtoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}