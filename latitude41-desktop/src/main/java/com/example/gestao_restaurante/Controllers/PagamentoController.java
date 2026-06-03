package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Pagamento;
import com.example.gestao_restaurante.Services.PagamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    // GET /pagamentos - listar todos os pagamentos
    @GetMapping
    public ResponseEntity<List<Pagamento>> listarTodos() {
        return ResponseEntity.ok(pagamentoService.listarTodos());
    }

    // GET /pagamentos/{id} - procurar pagamento por id
    @GetMapping("/{id}")
    public ResponseEntity<Pagamento> procurarPorId(@PathVariable Integer id) {
        return pagamentoService.procurarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /pagamentos - criar novo pagamento
    @PostMapping
    public ResponseEntity<Pagamento> criar(@RequestBody Pagamento pagamento) {
        Pagamento novo = pagamentoService.criar(pagamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    // PUT /pagamentos/{id} - atualizar pagamento existente
    @PutMapping("/{id}")
    public ResponseEntity<Pagamento> atualizar(@PathVariable Integer id, @RequestBody Pagamento pagamento) {
        return pagamentoService.atualizar(id, pagamento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /pagamentos/{id} - apagar pagamento
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Integer id) {
        if (pagamentoService.apagar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
