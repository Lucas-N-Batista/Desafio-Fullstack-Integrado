package com.desafio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para criação e atualização de Benefício.
 * Usado nas operações POST e PUT do {@code /api/v1/beneficios}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para criar ou atualizar um Benefício")
public class BeneficioRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Schema(description = "Nome do benefício", example = "Vale Alimentação")
    private String nome;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    @Schema(description = "Descrição detalhada", example = "Benefício de alimentação mensal")
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor não pode ser negativo")
    @Digits(integer = 13, fraction = 2, message = "Valor inválido")
    @Schema(description = "Saldo inicial do benefício", example = "1500.00")
    private BigDecimal valor;

    @Schema(description = "Indica se o benefício está ativo", example = "true")
    @Builder.Default
    private Boolean ativo = Boolean.TRUE;
}
