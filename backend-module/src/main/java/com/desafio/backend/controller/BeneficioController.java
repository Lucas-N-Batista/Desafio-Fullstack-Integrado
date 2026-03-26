package com.desafio.backend.controller;

import com.desafio.backend.dto.*;
import com.desafio.backend.service.BeneficioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para CRUD de Benefícios e Transferência.
 *
 * <p>Todos os endpoints estão documentados via <b>Swagger/OpenAPI 3</b>
 * acessível em {@code /swagger-ui.html}.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Benefícios", description = "CRUD de Benefícios e operação de Transferência")
public class BeneficioController {

    private final BeneficioService beneficioService;

    // =========================================================================
    // CRUD endpoints — /api/v1/beneficios
    // =========================================================================

    @GetMapping("/api/v1/beneficios")
    @Operation(summary = "Lista todos os benefícios ativos")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<BeneficioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(beneficioService.listarTodos());
    }

    @GetMapping("/api/v1/beneficios/{id}")
    @Operation(summary = "Busca um benefício pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício encontrado"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado")
    })
    public ResponseEntity<BeneficioResponseDTO> buscarPorId(
            @Parameter(description = "ID do benefício", required = true)
                        @PathVariable("id") Long id) {
        return ResponseEntity.ok(beneficioService.buscarPorId(id));
    }

    @PostMapping("/api/v1/beneficios")
    @Operation(summary = "Cria um novo benefício")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Benefício criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<BeneficioResponseDTO> criar(
            @Valid @RequestBody BeneficioRequestDTO dto) {
        BeneficioResponseDTO criado = beneficioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/api/v1/beneficios/{id}")
    @Operation(summary = "Atualiza um benefício existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado")
    })
    public ResponseEntity<BeneficioResponseDTO> atualizar(
                        @PathVariable("id") Long id,
            @Valid @RequestBody BeneficioRequestDTO dto) {
        return ResponseEntity.ok(beneficioService.atualizar(id, dto));
    }

    @DeleteMapping("/api/v1/beneficios/{id}")
    @Operation(summary = "Remove logicamente um benefício (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Benefício removido"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado")
    })
        public ResponseEntity<Void> remover(@PathVariable("id") Long id) {
        beneficioService.remover(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Transferência — POST /api/v1/transferencias
    // =========================================================================

    @PostMapping("/api/v1/transferencias")
    @Operation(
            summary = "Transfere saldo entre dois benefícios",
            description = "Opera com Pessimistic Write Lock e verifica saldo antes da transferência"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "404", description = "Benefício de origem ou destino não encontrado"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente na origem"),
            @ApiResponse(responseCode = "409", description = "Conflito de versão (optimistic lock)")
    })
    public ResponseEntity<Void> transferir(
            @Valid @RequestBody TransferenciaRequestDTO dto) {
        beneficioService.transferir(dto);
        return ResponseEntity.noContent().build();
    }
}
