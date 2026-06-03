package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Services.UtilizadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private LoginResponse toResponse(Utilizador utilizador) {
        return new LoginResponse(
                utilizador.getId(),
                utilizador.getNome(),
                utilizador.getEmail(),
                utilizador.getTipo(),
                utilizador.getEstadoConta()
        );
    }

    public record LoginRequest(String email, String password) {
    }

    public record LoginResponse(Integer id, String nome, String email, String tipo, String estadoConta) {
    }
}
