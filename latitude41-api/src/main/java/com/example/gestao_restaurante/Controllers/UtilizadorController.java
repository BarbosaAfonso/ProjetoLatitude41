package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Services.UtilizadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/utilizadores")
public class UtilizadorController {

    private final UtilizadorService utilizadorService;

    public UtilizadorController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    // GET /utilizadores - listar todos os utilizadores
    @GetMapping
    public ResponseEntity<List<Utilizador>> listarTodos() {
        return ResponseEntity.ok(utilizadorService.listarTodos());
    }

    // GET /utilizadores/{id} - procurar utilizador por id
    @GetMapping("/{id}")
    public ResponseEntity<Utilizador> procurarPorId(@PathVariable Integer id) {
        return utilizadorService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /utilizadores - criar novo utilizador
    @PostMapping
    public ResponseEntity<Utilizador> criar(@RequestBody Utilizador utilizador) {
        Utilizador novo = utilizadorService.criar(utilizador);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    // PUT /utilizadores/{id} - atualizar utilizador existente
    @PutMapping("/{id}")
    public ResponseEntity<Utilizador> atualizar(@PathVariable Integer id, @RequestBody Utilizador utilizador) {
        return utilizadorService.atualizar(id, utilizador)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /utilizadores/{id} - apagar utilizador
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (utilizadorService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
