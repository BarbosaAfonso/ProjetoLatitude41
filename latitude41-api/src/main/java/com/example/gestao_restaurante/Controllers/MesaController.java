package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Mesa;
import com.example.gestao_restaurante.Services.MesaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    // GET /mesas - listar todas as mesas
    @GetMapping
    public ResponseEntity<List<Mesa>> listarTodos() {
        return ResponseEntity.ok(mesaService.listarTodos());
    }

    // GET /mesas/{id} - procurar mesa por id
    @GetMapping("/{id}")
    public ResponseEntity<Mesa> procurarPorId(@PathVariable Integer id) {
        return mesaService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /mesas - criar nova mesa
    @PostMapping
    public ResponseEntity<Mesa> criar(@RequestBody Mesa mesa) {
        Mesa nova = mesaService.criar(mesa);
        return ResponseEntity.status(HttpStatus.CREATED).body(nova);
    }

    // PUT /mesas/{id} - atualizar mesa existente
    @PutMapping("/{id}")
    public ResponseEntity<Mesa> atualizar(@PathVariable Integer id, @RequestBody Mesa mesa) {
        return mesaService.atualizar(id, mesa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /mesas/{id} - apagar mesa
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (mesaService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
