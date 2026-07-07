package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Repositories.UtilizadorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Construtor atualizado para injetar a dependência do codificador de segurança
    public UtilizadorService(UtilizadorRepository utilizadorRepository, BCryptPasswordEncoder passwordEncoder) {
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
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

        // Encripta a password recebida antes de salvar o registo no PostgreSQL
        utilizador.setPassword(passwordEncoder.encode(utilizador.getPassword()));

        return utilizadorRepository.save(utilizador);
    }

    // POST - registar um novo utilizador vindo da aplicacao Web/Formulario
    public Utilizador registarNovoUtilizador(String nome, String email, String password) {
        // Valida se o email ja esta registado na base de dados antes de avancar
        if (email != null && utilizadorRepository.findByEmailIgnoreCase(email.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O email ja esta em uso.");
        }

        Utilizador novoUtilizador = new Utilizador();
        novoUtilizador.setNome(nome);
        novoUtilizador.setEmail(email);
        novoUtilizador.setPassword(password);
        novoUtilizador.setTipo("CLIENTE");
        novoUtilizador.setEstadoConta("ATIVO");

        // Reutiliza o metodo criar, aplicando as validações e a cifra de segurança uniformemente
        return criar(novoUtilizador);
    }

    // PUT - atualizar utilizador existente
    public Optional<Utilizador> atualizar(Integer id, Utilizador utilizadorAtualizado) {
        return utilizadorRepository.findById(id).map(utilizador -> {
            validarUtilizador(utilizadorAtualizado, false);
            utilizador.setNome(utilizadorAtualizado.getNome());
            utilizador.setContacto(utilizadorAtualizado.getContacto());
            utilizador.setEmail(utilizadorAtualizado.getEmail());

            // Se uma nova password válida for enviada, gera uma nova hash criptográfica
            if (utilizadorAtualizado.getPassword() != null && !utilizadorAtualizado.getPassword().isBlank()) {
                utilizador.setPassword(passwordEncoder.encode(utilizadorAtualizado.getPassword()));
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

        // Procura pelo e-mail e recorre ao método passwordEncoder.matches para validar de forma segura
        return utilizadorRepository.findByEmailIgnoreCase(email.trim())
                .filter(utilizador -> passwordEncoder.matches(password, utilizador.getPassword()))
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
        String valor = tipo == null || tipo.isBlank() ? "CLIENTE" : tipo.trim();
        valor = valor.toUpperCase(Locale.ROOT);
        if (!valor.equals("ADMIN") && !valor.equals("FUNCIONARIO") && !valor.equals("CLIENTE")) {
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
