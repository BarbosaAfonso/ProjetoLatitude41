package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Dtos.PedidoCompletoRequest;
import com.example.gestao_restaurante.Dtos.PedidoCompletoResponse;
import com.example.gestao_restaurante.Modules.Pedido;
import com.example.gestao_restaurante.Services.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // GET /pedidos - listar todos os pedidos
    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    // GET /pedidos/{id} - procurar pedido por id
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> procurarPorId(@PathVariable Integer id) {
        return pedidoService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mesa/{mesaId}/completos")
    public ResponseEntity<List<PedidoCompletoResponse>> listarCompletosPorMesa(@PathVariable Integer mesaId) {
        return ResponseEntity.ok(pedidoService.listarCompletosPorMesa(mesaId));
    }

    // POST /pedidos - criar novo pedido
    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody Pedido pedido) {
        Pedido novo = pedidoService.criar(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    @PostMapping("/completo")
    public ResponseEntity<PedidoCompletoResponse> criarCompleto(@RequestBody PedidoCompletoRequest pedido) {
        PedidoCompletoResponse novo = pedidoService.criarCompleto(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    // PUT /pedidos/{id} - atualizar pedido existente
    @PutMapping("/{id}")
    public ResponseEntity<Pedido> atualizar(@PathVariable Integer id, @RequestBody Pedido pedido) {
        return pedidoService.atualizar(id, pedido)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /pedidos/{id} - apagar pedido
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (pedidoService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
