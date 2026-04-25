package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;
import java.time.Instant;

public class FaturaGeracaoResponse {

    private Integer faturaId;
    private Integer pedidoId;
    private String metodoPagamento;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private Instant dataHora;
    private String caminhoPdf;

    public Integer getFaturaId() {
        return faturaId;
    }

    public void setFaturaId(Integer faturaId) {
        this.faturaId = faturaId;
    }

    public Integer getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Integer pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public void setDataHora(Instant dataHora) {
        this.dataHora = dataHora;
    }

    public String getCaminhoPdf() {
        return caminhoPdf;
    }

    public void setCaminhoPdf(String caminhoPdf) {
        this.caminhoPdf = caminhoPdf;
    }
}
