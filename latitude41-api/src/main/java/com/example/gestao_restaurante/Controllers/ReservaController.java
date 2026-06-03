package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Reserva;
import com.example.gestao_restaurante.Services.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // GET /reservas - listar todas as reservas
    @GetMapping
    public ResponseEntity<List<Reserva>> listarTodos() {
        return ResponseEntity.ok(reservaService.listarTodos());
    }

    // GET /reservas/{id} - procurar reserva por id
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> procurarPorId(@PathVariable Integer id) {
        return reservaService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /reservas - criar nova reserva
    @PostMapping
    public ResponseEntity<Reserva> criar(@RequestBody Reserva reserva) {
        Reserva nova = reservaService.criar(reserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(nova);
    }

    // PUT /reservas/{id} - atualizar reserva existente
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> atualizar(@PathVariable Integer id, @RequestBody Reserva reserva) {
        return reservaService.atualizar(id, reserva)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /reservas/{id} - apagar reserva
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (reservaService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
