package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Pagamento;
import org.springframework.data.repository.CrudRepository;

public interface PagamentoRepository extends CrudRepository<Pagamento, Integer> {
}