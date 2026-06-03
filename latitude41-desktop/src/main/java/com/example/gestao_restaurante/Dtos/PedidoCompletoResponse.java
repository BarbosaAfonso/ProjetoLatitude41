package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PedidoCompletoResponse {

    private Integer id;
    private Instant dataHora;
    private String estado;
    private Integer reservaId;
    private Integer mesaId;
    private Integer quantidadeItens;
    private BigDecimal subtotal;
    private List<PedidoLinhaResumo> linhas = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Integer getQuantidadeItens() {
        return quantidadeItens;
    }

    public void setQuantidadeItens(Integer quantidadeItens) {
        this.quantidadeItens = quantidadeItens;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public List<PedidoLinhaResumo> getLinhas() {
        return linhas;
    }

    public void setLinhas(List<PedidoLinhaResumo> linhas) {
        this.linhas = linhas == null ? new ArrayList<>() : linhas;
    }
}
