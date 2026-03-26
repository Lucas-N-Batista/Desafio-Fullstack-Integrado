package com.desafio.ejb.exception;

/**
 * Exceção lançada quando o benefício de origem não possui saldo
 * suficiente para realizar a transferência solicitada.
 *
 * <p>É uma {@link RuntimeException} para que o container EJB
 * efetue rollback automático da transação corrente (comportamento
 * padrão para unchecked exceptions marcadas com {@code @ApplicationException(rollback=true)}).</p>
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }

    public SaldoInsuficienteException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
