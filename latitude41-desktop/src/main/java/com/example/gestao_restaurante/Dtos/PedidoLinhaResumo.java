package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;

public class PedidoLinhaResumo {

    private Integer produtoId;
    private String nomeProduto;
    private String tipoProduto;
    private Integer quantidade;
    private BigDecimal precoUnitVenda;
    private String observacoes;
    private BigDecimal subtotal;

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getTipoProduto() {
        return tipoProduto;
    }

    public void setTipoProduto(String tipoProduto) {
        this.tipoProduto = tipoProduto;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
