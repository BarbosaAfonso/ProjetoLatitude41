package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.LinhaPedido;
import com.example.gestao_restaurante.Modules.LinhaPedidoId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LinhaPedidoRepository extends CrudRepository<LinhaPedido, LinhaPedidoId> {

    List<LinhaPedido> findByIdIdPedidoOrderByIdIdProdutoAsc(Integer pedidoId);
}
