package com.example.gestao_restaurante.Modules;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LinhaPedidoId implements Serializable {
    private static final long serialVersionUID = 7906821375147473427L;
    @Column(name = "id_pedido", nullable = false)
    private Integer idPedido;

    @Column(name = "id_produto", nullable = false)
    private Integer idProduto;

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public Integer getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Integer idProduto) {
        this.idProduto = idProduto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        LinhaPedidoId entity = (LinhaPedidoId) o;
        return Objects.equals(this.idProduto, entity.idProduto) &&
                Objects.equals(this.idPedido, entity.idPedido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProduto, idPedido);
    }

}