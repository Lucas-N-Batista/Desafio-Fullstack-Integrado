package com.desafio.ejb.service;

import com.desafio.ejb.model.Beneficio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface local do Session Bean EJB para operações de Benefício.
 *
 * <p>Expõe as operações CRUD e o método de transferência de saldo.</p>
 */
public interface BeneficioEjbService {

    /** Retorna todos os benefícios cadastrados. */
    List<Beneficio> listarTodos();

    /** Busca um benefício pelo ID; lança {@code BeneficioNaoEncontradoException} se não existir. */
    Beneficio buscarPorId(Long id);

    /** Persiste um novo benefício. */
    Beneficio criar(Beneficio beneficio);

    /** Atualiza um benefício existente; lança {@code BeneficioNaoEncontradoException} se não existir. */
    Beneficio atualizar(Long id, Beneficio beneficioAtualizado);

    /** Remove logicamente um benefício (seta ativo=false). */
    void remover(Long id);

    /**
     * Transfere {@code valor} do benefício de origem para o de destino.
     *
     * <p><b>Regras de negócio:</b>
     * <ul>
     *   <li>Origem e destino devem existir.</li>
     *   <li>O {@code valor} deve ser positivo.</li>
     *   <li>O saldo da origem deve ser &ge; {@code valor}.</li>
     *   <li>A operação é atômica — ou ambas as atualizações ocorrem, ou nenhuma.</li>
     * </ul>
     * </p>
     *
     * @param origemId  ID do benefício que cede o saldo
     * @param destinoId ID do benefício que recebe o saldo
     * @param valor     Quantia a transferir (deve ser &gt; 0)
     * @throws com.desafio.ejb.exception.SaldoInsuficienteException  se saldo &lt; valor
     * @throws com.desafio.ejb.exception.BeneficioNaoEncontradoException se algum ID não existir
     * @throws IllegalArgumentException se valor &le; 0
     */
    void transferir(Long origemId, Long destinoId, BigDecimal valor);
}
