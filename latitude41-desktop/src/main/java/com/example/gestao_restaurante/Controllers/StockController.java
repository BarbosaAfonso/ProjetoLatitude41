package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Stock;
import com.example.gestao_restaurante.Services.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // GET /stocks - listar todos os registos de stock
    @GetMapping
    public ResponseEntity<List<Stock>> listarTodos() {
        return ResponseEntity.ok(stockService.listarTodos());
    }

    // GET /stocks/{id} - procurar stock por id
    @GetMapping("/{id}")
    public ResponseEntity<Stock> procurarPorId(@PathVariable Integer id) {
        return stockService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /stocks - criar novo stock
    @PostMapping
    public ResponseEntity<Stock> criar(@RequestBody Stock stock) {
        Stock novo = stockService.criar(stock);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    // PUT /stocks/{id} - atualizar stock existente
    @PutMapping("/{id}")
    public ResponseEntity<Stock> atualizar(@PathVariable Integer id, @RequestBody Stock stock) {
        return stockService.atualizar(id, stock)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /stocks/{id} - apagar stock
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (stockService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
