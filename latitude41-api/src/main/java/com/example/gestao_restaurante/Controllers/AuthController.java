package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Config.JwtUtil;
import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Services.UtilizadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UtilizadorService utilizadorService;
    private final JwtUtil jwtUtil;

    // Injeção por construtor adaptada para incluir o utilitário de tokens
    public AuthController(UtilizadorService utilizadorService, JwtUtil jwtUtil) {
        this.utilizadorService = utilizadorService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Autentica através do serviço e, se o utilizador existir, gera o token JWT
        return utilizadorService.autenticar(request.email(), request.password())
                .map(utilizador -> {
                    String token = jwtUtil.gerarToken(utilizador.getEmail(), utilizador.getTipo());
                    return toResponse(utilizador, token);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/registar")
    public ResponseEntity<?> registar(@RequestBody RegisterRequest request) {
        if (request == null || request.nome() == null || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().body("Dados inválidos.");
        }

        try {
            Utilizador novo = utilizadorService.registarNovoUtilizador(request.nome(), request.email(), request.password());
            String token = jwtUtil.gerarToken(novo.getEmail(), novo.getTipo());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(novo, token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar conta: " + e.getMessage());
        }
    }

    // Método utilitário atualizado para acoplar o token gerado ao record de resposta
    private LoginResponse toResponse(Utilizador utilizador, String token) {
        return new LoginResponse(
                token,
                utilizador.getId(),
                utilizador.getNome(),
                utilizador.getEmail(),
                utilizador.getTipo(),
                utilizador.getEstadoConta()
        );
    }

    public record LoginRequest(String email, String password) {}

    public record RegisterRequest(String nome, String email, String password) {}

    // Record de resposta atualizado para incluir o token recebido pelo frontend/desktop
    public record LoginResponse(String token, Integer id, String nome, String email, String tipo, String estadoConta) {}
}
