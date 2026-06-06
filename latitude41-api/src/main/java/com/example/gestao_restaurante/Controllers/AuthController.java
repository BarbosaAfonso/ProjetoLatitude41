package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Services.UtilizadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UtilizadorService utilizadorService;

    public AuthController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }

        return utilizadorService.autenticar(request.email(), request.password())
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // 1. NOVO ENDPOINT: Criar Conta
    @PostMapping("/registar")
    public ResponseEntity<?> registar(@RequestBody RegisterRequest request) {
        if (request == null || request.nome() == null || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().body("Dados inválidos.");
        }

        try {
            // Nota: Garante que tens um método no teu utilizadorService para criar/salvar
            Utilizador novo = utilizadorService.registarNovoUtilizador(request.nome(), request.email(), request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(novo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar conta: " + e.getMessage());
        }
    }

    private LoginResponse toResponse(Utilizador utilizador) {
        return new LoginResponse(
                utilizador.getId(),
                utilizador.getNome(),
                utilizador.getEmail(),
                utilizador.getTipo(),
                utilizador.getEstadoConta()
        );
    }

    public record LoginRequest(String email, String password) {}

    // 2. NOVO RECORD: Para receber os dados de registo
    public record RegisterRequest(String nome, String email, String password) {}

    public record LoginResponse(Integer id, String nome, String email, String tipo, String estadoConta) {}
}
