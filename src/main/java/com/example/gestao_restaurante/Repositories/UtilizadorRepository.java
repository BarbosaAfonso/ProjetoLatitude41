package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Utilizador;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UtilizadorRepository extends CrudRepository<Utilizador, Integer> {
    Optional<Utilizador> findByEmailIgnoreCase(String email);
}
