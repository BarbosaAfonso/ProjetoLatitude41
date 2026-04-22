package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Reserva;
import org.springframework.data.repository.CrudRepository;

public interface ReservaRepository extends CrudRepository<Reserva, Integer> {
}