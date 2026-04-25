package com.example.gestao_restaurante.Controllers;

import com.example.gestao_restaurante.Dtos.RelatorioAnaliseDTO;
import com.example.gestao_restaurante.Dtos.RelatorioExportacaoResponse;
import com.example.gestao_restaurante.Services.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.LocalDate;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/analise")
    public ResponseEntity<RelatorioAnaliseDTO> obterAnalise(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        LocalDate fim = dataFim == null ? LocalDate.now() : dataFim;
        LocalDate inicio = dataInicio == null ? fim.minusDays(6) : dataInicio;
        return ResponseEntity.ok(relatorioService.gerarAnalise(inicio, fim));
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<RelatorioExportacaoResponse> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        LocalDate fim = dataFim == null ? LocalDate.now() : dataFim;
        LocalDate inicio = dataInicio == null ? fim.minusDays(6) : dataInicio;
        Path caminhoPdf = relatorioService.exportarAnaliseEmPdf(inicio, fim);

        RelatorioExportacaoResponse response = new RelatorioExportacaoResponse();
        response.setDataInicio(inicio);
        response.setDataFim(fim);
        response.setCaminhoPdf(caminhoPdf.toAbsolutePath().toString());
        return ResponseEntity.ok(response);
    }
}
