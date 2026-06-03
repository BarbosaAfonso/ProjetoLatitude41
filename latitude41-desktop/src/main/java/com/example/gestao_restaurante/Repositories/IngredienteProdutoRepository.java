package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.IngredienteProduto;
import com.example.gestao_restaurante.Modules.IngredienteProdutoId;
import org.springframework.data.repository.CrudRepository;

public interface IngredienteProdutoRepository extends CrudRepository<IngredienteProduto, IngredienteProdutoId> {
}