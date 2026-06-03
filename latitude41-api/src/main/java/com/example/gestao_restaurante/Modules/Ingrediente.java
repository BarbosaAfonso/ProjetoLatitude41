package com.example.gestao_restaurante.Modules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "ingrediente")
public class Ingrediente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingred", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "unidade", nullable = false, length = 20)
    private String unidade;

    @Column(name = "preco", precision = 10, scale = 2)
    private BigDecimal preco;

    @OneToMany(mappedBy = "idIngred")
    @JsonIgnore
    private Set<IngredienteProduto> ingredienteProdutos = new LinkedHashSet<>();

    @OneToOne(mappedBy = "ingrediente")
    @JsonIgnore
    private Stock stock;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Set<IngredienteProduto> getIngredienteProdutos() {
        return ingredienteProdutos;
    }

    public void setIngredienteProdutos(Set<IngredienteProduto> ingredienteProdutos) {
        this.ingredienteProdutos = ingredienteProdutos;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

}
