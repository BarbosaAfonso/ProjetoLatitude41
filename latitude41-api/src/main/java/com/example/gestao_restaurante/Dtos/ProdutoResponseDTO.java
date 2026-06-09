package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;

public class ProdutoResponseDTO {

    private Integer id;
    private String nome;
    private String tipo;
    private BigDecimal preco;
    private Boolean disponivel;
    private String urlImagem;
    private Boolean vegetariano;
    private Boolean semGluten;
    private Boolean semLactose;

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
}
