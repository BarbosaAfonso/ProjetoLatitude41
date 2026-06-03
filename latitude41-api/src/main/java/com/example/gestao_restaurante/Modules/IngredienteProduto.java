package com.example.gestao_restaurante.Modules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "ingrediente_produto")
public class IngredienteProduto {
    @EmbeddedId
    private IngredienteProdutoId id;

    @MapsId("idProduto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false)
    @JsonIgnore
    private Produto idProduto;

    @MapsId("idIngred")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ingred", nullable = false)
    @JsonIgnore
    private Ingrediente idIngred;

    @Column(name = "quant_usada", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantUsada;

    public IngredienteProdutoId getId() {
        return id;
    }

    public void setId(IngredienteProdutoId id) {
        this.id = id;
    }

    public Produto getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Produto idProduto) {
        this.idProduto = idProduto;
    }

    public Ingrediente getIdIngred() {
        return idIngred;
    }

    public void setIdIngred(Ingrediente idIngred) {
        this.idIngred = idIngred;
    }

    public BigDecimal getQuantUsada() {
        return quantUsada;
    }

    public void setQuantUsada(BigDecimal quantUsada) {
        this.quantUsada = quantUsada;
    }

}
