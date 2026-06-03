package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;

public class CategoriaValorDTO {

    private String categoria;
    private BigDecimal valor;

    public CategoriaValorDTO() {
    }

    public CategoriaValorDTO(String categoria, BigDecimal valor) {
        this.categoria = categoria;
        this.valor = valor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
