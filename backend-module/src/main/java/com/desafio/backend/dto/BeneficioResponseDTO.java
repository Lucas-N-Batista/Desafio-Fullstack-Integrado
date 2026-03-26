package com.desafio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta para leitura de Benefício.
 * Nunca expõe o campo {@code version} diretamente ao cliente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados de um Benefício")
public class BeneficioResponseDTO {

    @Schema(description = "ID único do benefício", example = "1")
    private Long id;

    @Schema(description = "Nome do benefício", example = "Vale Alimentação")
    private String nome;

    @Schema(description = "Descrição", example = "Benefício de alimentação mensal")
    private String descricao;

    @Schema(description = "Saldo atual", example = "1500.00")
    private BigDecimal valor;

    @Schema(description = "Ativo", example = "true")
    private Boolean ativo;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Schema(description = "Data da última atualização")
    private LocalDateTime updatedAt;
}
