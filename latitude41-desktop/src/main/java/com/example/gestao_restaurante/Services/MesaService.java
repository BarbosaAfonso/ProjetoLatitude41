package com.example.gestao_restaurante.Services;

import com.example.gestao_restaurante.Modules.Mesa;
import com.example.gestao_restaurante.Repositories.MesaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;

    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    // GET - listar todas as mesas
    public List<Mesa> listarTodos() {
        return (List<Mesa>) mesaRepository.findAll();
    }

    // GET - procurar mesa por id
    public Optional<Mesa> procurarPorId(Integer id) {
        return mesaRepository.findById(id);
    }

    // POST - criar nova mesa
    public Mesa criar(Mesa mesa) {
        return mesaRepository.save(mesa);
    }

    // PUT - atualizar mesa existente
    public Optional<Mesa> atualizar(Integer id, Mesa mesaAtualizada) {
        return mesaRepository.findById(id).map(mesa -> {
            mesa.setNumLugares(mesaAtualizada.getNumLugares());
            mesa.setEstado(mesaAtualizada.getEstado());
            return mesaRepository.save(mesa);
        });
    }

    // DELETE - apagar mesa por id
    public boolean apagar(Integer id) {
        if (mesaRepository.existsById(id)) {
            mesaRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
