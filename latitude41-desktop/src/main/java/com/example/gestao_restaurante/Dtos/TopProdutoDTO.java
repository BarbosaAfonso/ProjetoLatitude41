package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;

public class TopProdutoDTO {

    private Integer produtoId;
    private String nomeProduto;
    private String categoria;
    private Long quantidadeVendida;
    private BigDecimal valorTotal;

    public TopProdutoDTO() {
    }

    public TopProdutoDTO(Integer produtoId,
                         String nomeProduto,
                         String categoria,
                         Long quantidadeVendida,
                         BigDecimal valorTotal) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.categoria = categoria;
        this.quantidadeVendida = quantidadeVendida;
        this.valorTotal = valorTotal;
    }

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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getQuantidadeVendida() {
        return quantidadeVendida;
    }

    public void setQuantidadeVendida(Long quantidadeVendida) {
        this.quantidadeVendida = quantidadeVendida;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
}
