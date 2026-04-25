package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VendaDiariaDTO {

    private LocalDate data;
    private BigDecimal total;

    public VendaDiariaDTO() {
    }

    public VendaDiariaDTO(LocalDate data, BigDecimal total) {
        this.data = data;
        this.total = total;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
