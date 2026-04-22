package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Fatura;
import com.example.gestao_restaurante.Services.FaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faturas")
public class FaturaController {

    private final FaturaService faturaService;

    public FaturaController(FaturaService faturaService) {
        this.faturaService = faturaService;
    }

    // GET /faturas - listar todas as faturas
    @GetMapping
    public ResponseEntity<List<Fatura>> listarTodos() {
        return ResponseEntity.ok(faturaService.listarTodos());
    }

    // GET /faturas/{id} - procurar fatura por id
    @GetMapping("/{id}")
    public ResponseEntity<Fatura> procurarPorId(@PathVariable Integer id) {
        return faturaService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /faturas - criar nova fatura
    @PostMapping
    public ResponseEntity<Fatura> criar(@RequestBody Fatura fatura) {
        Fatura nova = faturaService.criar(fatura);
        return ResponseEntity.status(HttpStatus.CREATED).body(nova);
    }

    // PUT /faturas/{id} - atualizar fatura existente
    @PutMapping("/{id}")
    public ResponseEntity<Fatura> atualizar(@PathVariable Integer id, @RequestBody Fatura fatura) {
        return faturaService.atualizar(id, fatura)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /faturas/{id} - apagar fatura
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (faturaService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
