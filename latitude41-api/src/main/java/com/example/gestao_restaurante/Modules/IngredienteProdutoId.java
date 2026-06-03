package com.example.gestao_restaurante.Modules;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IngredienteProdutoId implements Serializable {
    private static final long serialVersionUID = 8058565402820792461L;
    @Column(name = "id_produto", nullable = false)
    private Integer idProduto;

    @Column(name = "id_ingred", nullable = false)
    private Integer idIngred;

    public Integer getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Integer idProduto) {
        this.idProduto = idProduto;
    }

    public Integer getIdIngred() {
        return idIngred;
    }

    public void setIdIngred(Integer idIngred) {
        this.idIngred = idIngred;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        IngredienteProdutoId entity = (IngredienteProdutoId) o;
        return Objects.equals(this.idIngred, entity.idIngred) &&
                Objects.equals(this.idProduto, entity.idProduto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idIngred, idProduto);
    }

}