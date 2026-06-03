package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Repositories.UtilizadorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;

    public UtilizadorService(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    // GET - listar todos os utilizadores
    public List<Utilizador> listarTodos() {
        return (List<Utilizador>) utilizadorRepository.findAll();
    }

    // GET - procurar utilizador por id
    public Optional<Utilizador> procurarPorId(Integer id) {
        return utilizadorRepository.findById(id);
    }

    // POST - criar novo utilizador
    public Utilizador criar(Utilizador utilizador) {
        validarUtilizador(utilizador, true);
        utilizador.setTipo(normalizarTipo(utilizador.getTipo()));
        utilizador.setEstadoConta(normalizarEstadoConta(utilizador.getEstadoConta()));
        return utilizadorRepository.save(utilizador);
    }

    // PUT - atualizar utilizador existente
    public Optional<Utilizador> atualizar(Integer id, Utilizador utilizadorAtualizado) {
        return utilizadorRepository.findById(id).map(utilizador -> {
            validarUtilizador(utilizadorAtualizado, false);
            utilizador.setNome(utilizadorAtualizado.getNome());
            utilizador.setContacto(utilizadorAtualizado.getContacto());
            utilizador.setEmail(utilizadorAtualizado.getEmail());
            if (utilizadorAtualizado.getPassword() != null && !utilizadorAtualizado.getPassword().isBlank()) {
                utilizador.setPassword(utilizadorAtualizado.getPassword());
            }
            utilizador.setTipo(normalizarTipo(utilizadorAtualizado.getTipo()));
            utilizador.setEstadoConta(normalizarEstadoConta(utilizadorAtualizado.getEstadoConta()));
            return utilizadorRepository.save(utilizador);
        });
    }

    // DELETE - apagar utilizador por id
    public boolean apagar(Integer id) {
        if (utilizadorRepository.existsById(id)) {
            utilizadorRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // POST - autenticar utilizador por email/password
    public Optional<Utilizador> autenticar(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        return utilizadorRepository.findByEmailIgnoreCase(email.trim())
                .filter(utilizador -> Objects.equals(utilizador.getPassword(), password))
                .filter(utilizador -> "ATIVO".equalsIgnoreCase(normalizarEstadoConta(utilizador.getEstadoConta())));
    }

    private void validarUtilizador(Utilizador utilizador, boolean passwordObrigatoria) {
        if (utilizador == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizador invalido.");
        }
        if (utilizador.getNome() == null || utilizador.getNome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do utilizador e obrigatorio.");
        }
        if (utilizador.getEmail() == null || utilizador.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O email do utilizador e obrigatorio.");
        }
        if (passwordObrigatoria && (utilizador.getPassword() == null || utilizador.getPassword().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A password do utilizador e obrigatoria.");
        }
    }

    private String normalizarTipo(String tipo) {
        String valor = tipo == null || tipo.isBlank() ? "FUNCIONARIO" : tipo.trim();
        valor = valor.toUpperCase(Locale.ROOT);
        if (!valor.equals("ADMIN") && !valor.equals("FUNCIONARIO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de utilizador invalido.");
        }
        return valor;
    }

    private String normalizarEstadoConta(String estadoConta) {
        String valor = estadoConta == null || estadoConta.isBlank() ? "ATIVO" : estadoConta.trim();
        valor = valor.toUpperCase(Locale.ROOT);
        if (valor.equals("DESATIVADO")) {
            return "INATIVO";
        }
        if (!valor.equals("ATIVO") && !valor.equals("INATIVO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de conta invalido.");
        }
        return valor;
    }
}
