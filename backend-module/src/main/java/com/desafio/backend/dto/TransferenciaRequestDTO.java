package com.desafio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para a operação de transferência de saldo entre benefícios.
 * Endpoint: POST /api/v1/transferencias
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para transferência de saldo entre dois Benefícios")
public class TransferenciaRequestDTO {

    @NotNull(message = "ID de origem é obrigatório")
    @Positive(message = "ID de origem deve ser positivo")
    @Schema(description = "ID do benefício de origem (que cede o saldo)", example = "1")
    private Long origemId;

    @NotNull(message = "ID de destino é obrigatório")
    @Positive(message = "ID de destino deve ser positivo")
    @Schema(description = "ID do benefício de destino (que recebe o saldo)", example = "2")
    private Long destinoId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor da transferência deve ser maior que zero")
    @Digits(integer = 13, fraction = 2, message = "Valor inválido")
    @Schema(description = "Valor a transferir", example = "300.00")
    private BigDecimal valor;
}
