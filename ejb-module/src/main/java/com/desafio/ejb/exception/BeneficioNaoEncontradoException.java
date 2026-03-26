package com.desafio.ejb.exception;

/**
 * Exceção lançada quando não se encontra um {@link com.desafio.ejb.model.Beneficio}
 * pelo ID fornecido.
 */
public class BeneficioNaoEncontradoException extends RuntimeException {

    public BeneficioNaoEncontradoException(Long id) {
        super("Benefício não encontrado com id: " + id);
    }
}
