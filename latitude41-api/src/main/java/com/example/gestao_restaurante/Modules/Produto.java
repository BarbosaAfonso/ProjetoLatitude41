package com.example.gestao_restaurante.Modules;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("urlImagem")
    @JsonAlias("url_imagem")
    @Column(name = "url_imagem", length = 500)
    private String urlImagem;

    @JsonProperty("vegetariano")
    @ColumnDefault("false")
    @Column(name = "vegetariano")
    private Boolean vegetariano = false;

    @JsonProperty("semGluten")
    @JsonAlias("sem_gluten")
    @ColumnDefault("false")
    @Column(name = "sem_gluten")
    private Boolean semGluten = false;

    @JsonProperty("semLactose")
    @JsonAlias("sem_lactose")
    @ColumnDefault("false")
    @Column(name = "sem_lactose")
    private Boolean semLactose = false;

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

    public String getUrlImagem() {
        return urlImagem;
    }

    public void setUrlImagem(String urlImagem) {
        this.urlImagem = urlImagem;
    }

    public Boolean getVegetariano() {
        return vegetariano;
    }

    public void setVegetariano(Boolean vegetariano) {
        this.vegetariano = vegetariano;
    }

    public Boolean getSemGluten() {
        return semGluten;
    }

    public void setSemGluten(Boolean semGluten) {
        this.semGluten = semGluten;
    }

    public Boolean getSemLactose() {
        return semLactose;
    }

    public void setSemLactose(Boolean semLactose) {
        this.semLactose = semLactose;
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
