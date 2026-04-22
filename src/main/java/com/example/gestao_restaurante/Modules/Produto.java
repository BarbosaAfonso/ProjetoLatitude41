package com.example.gestao_restaurante.Modules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "produto")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "tipo", length = 50)
    private String tipo;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @ColumnDefault("true")
    @Column(name = "disponivel")
    private Boolean disponivel;

    @OneToMany(mappedBy = "idProduto")
    @JsonIgnore
    private Set<IngredienteProduto> ingredienteProdutos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idProduto")
    @JsonIgnore
    private Set<LinhaPedido> linhaPedidos = new LinkedHashSet<>();

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Boolean getDisponivel() {
        return disponivel;
    }

    public void setDisponivel(Boolean disponivel) {
        this.disponivel = disponivel;
    }

    public Set<IngredienteProduto> getIngredienteProdutos() {
        return ingredienteProdutos;
    }

    public void setIngredienteProdutos(Set<IngredienteProduto> ingredienteProdutos) {
        this.ingredienteProdutos = ingredienteProdutos;
    }

    public Set<LinhaPedido> getLinhaPedidos() {
        return linhaPedidos;
    }

    public void setLinhaPedidos(Set<LinhaPedido> linhaPedidos) {
        this.linhaPedidos = linhaPedidos;
    }

}
