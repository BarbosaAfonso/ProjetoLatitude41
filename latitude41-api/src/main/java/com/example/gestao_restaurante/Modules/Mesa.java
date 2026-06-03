package com.example.gestao_restaurante.Modules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "mesa")
public class Mesa {
    @Id
    @Column(name = "num_mesa", nullable = false)
    private Integer id;

    @Column(name = "num_lugares", nullable = false)
    private Integer numLugares;

    @ColumnDefault("'LIVRE'")
    @Column(name = "estado", length = 20)
    private String estado;

    @OneToMany(mappedBy = "numMesa")
    @JsonIgnore
    private Set<Reserva> reservas = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumLugares() {
        return numLugares;
    }

    public void setNumLugares(Integer numLugares) {
        this.numLugares = numLugares;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Set<Reserva> getReservas() {
        return reservas;
    }

    public void setReservas(Set<Reserva> reservas) {
        this.reservas = reservas;
    }

}
