package com.example.gestao_restaurante.Dtos;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public class ProdutoRequestDTO {

    private String nome;
    private String tipo;
    private BigDecimal preco;
    private Boolean disponivel;

    @JsonAlias("url_imagem")
    private String urlImagem;

    private Boolean vegetariano;

    @JsonAlias("sem_gluten")
    private Boolean semGluten;

    @JsonAlias("sem_lactose")
    private Boolean semLactose;

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
}
