package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;

public class PedidoLinhaRequest {

    private Integer produtoId;
    private Integer quantidade;
    private BigDecimal precoUnitVenda;
    private String observacoes;

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitVenda() {
        return precoUnitVenda;
    }

    public void setPrecoUnitVenda(BigDecimal precoUnitVenda) {
        this.precoUnitVenda = precoUnitVenda;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
