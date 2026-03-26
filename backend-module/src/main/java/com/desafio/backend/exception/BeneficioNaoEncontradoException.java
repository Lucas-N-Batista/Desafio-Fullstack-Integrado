package com.desafio.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando um Benefício não é encontrado pelo ID fornecido.
 * Mapeada para HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BeneficioNaoEncontradoException extends RuntimeException {

    public BeneficioNaoEncontradoException(Long id) {
        super("Benefício não encontrado com id: " + id);
    }
}
