package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Fatura;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FaturaRepository extends CrudRepository<Fatura, Integer> {

    Optional<Fatura> findByIdPedidoId(Integer pedidoId);
}
