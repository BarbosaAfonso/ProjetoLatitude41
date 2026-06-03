package com.example.gestao_restaurante.Repositories;

import com.example.gestao_restaurante.Modules.Stock;
import org.springframework.data.repository.CrudRepository;

public interface StockRepository extends CrudRepository<Stock, Integer> {
}