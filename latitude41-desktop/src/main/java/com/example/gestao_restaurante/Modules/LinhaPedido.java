package com.example.gestao_restaurante.Modules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "linha_pedido")
public class LinhaPedido {
    @EmbeddedId
    private LinhaPedidoId id;

    @MapsId("idPedido")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    @JsonIgnore
    private Pedido idPedido;

    @MapsId("idProduto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false)
    @JsonIgnore
    private Produto idProduto;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unit_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitVenda;

    @Column(name = "observacoes", length = Integer.MAX_VALUE)
    private String observacoes;

    public LinhaPedidoId getId() {
        return id;
    }

    public void setId(LinhaPedidoId id) {
        this.id = id;
    }

    public Pedido getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Pedido idPedido) {
        this.idPedido = idPedido;
    }

    public Produto getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Produto idProduto) {
        this.idProduto = idProduto;
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
