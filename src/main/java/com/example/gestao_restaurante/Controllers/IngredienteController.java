package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Ingrediente;
import com.example.gestao_restaurante.Services.IngredienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredientes")
public class IngredienteController {

    private final IngredienteService ingredienteService;

    public IngredienteController(IngredienteService ingredienteService) {
        this.ingredienteService = ingredienteService;
    }

    // GET /ingredientes - listar todos os ingredientes
    @GetMapping
    public ResponseEntity<List<Ingrediente>> listarTodos() {
        return ResponseEntity.ok(ingredienteService.listarTodos());
    }

    // GET /ingredientes/{id} - procurar ingrediente por id
    @GetMapping("/{id}")
    public ResponseEntity<Ingrediente> procurarPorId(@PathVariable Integer id) {
        return ingredienteService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /ingredientes - criar novo ingrediente
    @PostMapping
    public ResponseEntity<Ingrediente> criar(@RequestBody Ingrediente ingrediente) {
        Ingrediente novo = ingredienteService.criar(ingrediente);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    // PUT /ingredientes/{id} - atualizar ingrediente existente
    @PutMapping("/{id}")
    public ResponseEntity<Ingrediente> atualizar(@PathVariable Integer id, @RequestBody Ingrediente ingrediente) {
        return ingredienteService.atualizar(id, ingrediente)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /ingredientes/{id} - apagar ingrediente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (ingredienteService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
