package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Produto;
import org.springframework.data.repository.CrudRepository;

public interface ProdutoRepository extends CrudRepository<Produto, Integer> {
}