package com.example.gestao_restaurante.Dtos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PedidoCompletoRequest {

    private Instant dataHora;
    private String estado;
    private Integer mesaId;
    private Integer reservaId;
    private Integer utilizadorId;
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

    public Integer getMesaId() {
        return mesaId;
    }

    public void setMesaId(Integer mesaId) {
        this.mesaId = mesaId;
    }

    public Integer getUtilizadorId() {
        return utilizadorId;
    }

    public void setUtilizadorId(Integer utilizadorId) {
        this.utilizadorId = utilizadorId;
    }

    public List<PedidoLinhaRequest> getLinhas() {
        return linhas;
    }

    public void setLinhas(List<PedidoLinhaRequest> linhas) {
        this.linhas = linhas == null ? new ArrayList<>() : linhas;
    }
}
