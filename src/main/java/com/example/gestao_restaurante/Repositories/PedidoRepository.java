package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Pedido;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends CrudRepository<Pedido, Integer> {

    Optional<Pedido> findFirstByIdReservaNumMesaIdAndEstadoInOrderByDataHoraDesc(Integer mesaId, Collection<String> estados);

    Optional<Pedido> findFirstByIdReservaIdOrderByDataHoraDesc(Integer reservaId);

    List<Pedido> findByIdReservaNumMesaIdOrderByDataHoraDesc(Integer mesaId);

    default Optional<Pedido> findTopByMesaIdAndStatusInOrderByDataHoraDesc(Integer mesaId, Collection<String> estados) {
        return findFirstByIdReservaNumMesaIdAndEstadoInOrderByDataHoraDesc(mesaId, estados);
    }
}
