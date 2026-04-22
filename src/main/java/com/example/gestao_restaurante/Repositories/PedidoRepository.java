package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Pedido;
import org.springframework.data.repository.CrudRepository;

public interface PedidoRepository extends CrudRepository<Pedido, Integer> {
}