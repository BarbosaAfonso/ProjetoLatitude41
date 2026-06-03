package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Mesa;
import com.example.gestao_restaurante.Modules.Reserva;
import com.example.gestao_restaurante.Modules.Utilizador;
import com.example.gestao_restaurante.Repositories.MesaRepository;
import com.example.gestao_restaurante.Repositories.ReservaRepository;
import com.example.gestao_restaurante.Repositories.UtilizadorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final UtilizadorRepository utilizadorRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          MesaRepository mesaRepository,
                          UtilizadorRepository utilizadorRepository) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    public List<Reserva> listarTodos() {
        return (List<Reserva>) reservaRepository.findAll();
    }

    public Optional<Reserva> procurarPorId(Integer id) {
        return reservaRepository.findById(id);
    }

    public Reserva criar(Reserva reserva) {
        Reserva novaReserva = new Reserva();
        aplicarDados(novaReserva, reserva, false);
        return reservaRepository.save(novaReserva);
    }

    public Optional<Reserva> atualizar(Integer id, Reserva reservaAtualizada) {
        return reservaRepository.findById(id).map(reserva -> {
            aplicarDados(reserva, reservaAtualizada, true);
            return reservaRepository.save(reserva);
        });
    }

    public boolean apagar(Integer id) {
        if (reservaRepository.existsById(id)) {
            reservaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void aplicarDados(Reserva destino, Reserva origem, boolean preservarRelacoesAtuais) {
        if (origem == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva invalida.");
        }

        if (origem.getDataHora() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data e hora da reserva sao obrigatorias.");
        }

        destino.setDataHora(origem.getDataHora());
        String estado = normalizarEstado(origem.getEstado(), "CONFIRMADA");
        validarDataHora(destino.getDataHora(), estado);
        destino.setEstado(estado);
        destino.setNumMesa(procurarMesa(origem, destino, preservarRelacoesAtuais));
        destino.setIdUtilizador(procurarUtilizador(origem, destino, preservarRelacoesAtuais));
    }

    private Mesa procurarMesa(Reserva reserva, Reserva reservaAtual, boolean preservarRelacoesAtuais) {
        Integer mesaId = reserva.getNumMesa() == null ? null : reserva.getNumMesa().getId();
        if (mesaId == null && preservarRelacoesAtuais && reservaAtual.getNumMesa() != null) {
            return reservaAtual.getNumMesa();
        }
        if (mesaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma mesa valida.");
        }

        return mesaRepository.findById(mesaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mesa nao encontrada."));
    }

    private Utilizador procurarUtilizador(Reserva reserva, Reserva reservaAtual, boolean preservarRelacoesAtuais) {
        Integer utilizadorId = reserva.getIdUtilizador() == null ? null : reserva.getIdUtilizador().getId();
        if (utilizadorId == null && preservarRelacoesAtuais && reservaAtual.getIdUtilizador() != null) {
            return reservaAtual.getIdUtilizador();
        }
        if (utilizadorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um utilizador valido.");
        }

        return utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizador nao encontrado."));
    }

    private String normalizarEstado(String estado, String valorPadrao) {
        String valor = estado == null || estado.isBlank() ? valorPadrao : estado.trim();
        return valor.toUpperCase(Locale.ROOT);
    }

    private void validarDataHora(Instant dataHora, String estado) {
        if (dataHora == null) {
            return;
        }
        if (dataHora.isBefore(Instant.now()) && !"CANCELADA".equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao pode criar ou alterar reservas para datas passadas.");
        }
    }
}
