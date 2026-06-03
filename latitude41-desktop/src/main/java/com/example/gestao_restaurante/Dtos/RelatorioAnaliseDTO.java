package com.example.gestao_restaurante.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelatorioAnaliseDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private BigDecimal totalFaturado;
    private BigDecimal ticketMedio;
    private Long numeroPedidos;
    private List<VendaDiariaDTO> vendasUltimos7Dias = new ArrayList<>();
    private List<CategoriaValorDTO> vendasPorCategoria = new ArrayList<>();
    private List<TopProdutoDTO> topProdutos = new ArrayList<>();
    private List<CategoriaValorDTO> gastosStockPorCategoria = new ArrayList<>();

    public RelatorioAnaliseDTO() {
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public BigDecimal getTotalFaturado() {
        return totalFaturado;
    }

    public void setTotalFaturado(BigDecimal totalFaturado) {
        this.totalFaturado = totalFaturado;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }

    public Long getNumeroPedidos() {
        return numeroPedidos;
    }

    public void setNumeroPedidos(Long numeroPedidos) {
        this.numeroPedidos = numeroPedidos;
    }

    public List<VendaDiariaDTO> getVendasUltimos7Dias() {
        return vendasUltimos7Dias;
    }

    public void setVendasUltimos7Dias(List<VendaDiariaDTO> vendasUltimos7Dias) {
        this.vendasUltimos7Dias = vendasUltimos7Dias;
    }

    public List<CategoriaValorDTO> getVendasPorCategoria() {
        return vendasPorCategoria;
    }

    public void setVendasPorCategoria(List<CategoriaValorDTO> vendasPorCategoria) {
        this.vendasPorCategoria = vendasPorCategoria;
    }

    public List<TopProdutoDTO> getTopProdutos() {
        return topProdutos;
    }

    public void setTopProdutos(List<TopProdutoDTO> topProdutos) {
        this.topProdutos = topProdutos;
    }

    public List<CategoriaValorDTO> getGastosStockPorCategoria() {
        return gastosStockPorCategoria;
    }

    public void setGastosStockPorCategoria(List<CategoriaValorDTO> gastosStockPorCategoria) {
        this.gastosStockPorCategoria = gastosStockPorCategoria;
    }
}
