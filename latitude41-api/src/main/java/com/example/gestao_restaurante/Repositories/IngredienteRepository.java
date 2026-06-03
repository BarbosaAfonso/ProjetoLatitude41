package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Ingrediente;
import org.springframework.data.repository.CrudRepository;

public interface IngredienteRepository extends CrudRepository<Ingrediente, Integer> {
}