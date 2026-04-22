package com.example.gestao_restaurante.Dtos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PedidoCompletoRequest {

    private Instant dataHora;
    private String estado;
    private Integer reservaId;
    private List<PedidoLinhaRequest> linhas = new ArrayList<>();

    public Instant getDataHora() {
        return dataHora;
    }

    public void setDataHora(Instant dataHora) {
        this.dataHora = dataHora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getReservaId() {
        return reservaId;
    }

    public void setReservaId(Integer reservaId) {
        this.reservaId = reservaId;
    }

    public List<PedidoLinhaRequest> getLinhas() {
        return linhas;
    }

    public void setLinhas(List<PedidoLinhaRequest> linhas) {
        this.linhas = linhas == null ? new ArrayList<>() : linhas;
    }
}
