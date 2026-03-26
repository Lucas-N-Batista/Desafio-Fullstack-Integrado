package com.desafio.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando o saldo da origem é insuficiente para a transferência.
 * Mapeada automaticamente para HTTP 422 Unprocessable Entity pelo {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
