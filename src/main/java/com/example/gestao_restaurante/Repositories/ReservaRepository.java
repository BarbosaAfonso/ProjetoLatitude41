package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Reserva;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Optional;

public interface ReservaRepository extends CrudRepository<Reserva, Integer> {

    Optional<Reserva> findFirstByNumMesaIdAndEstadoInOrderByDataHoraDesc(Integer mesaId, Collection<String> estados);
}
