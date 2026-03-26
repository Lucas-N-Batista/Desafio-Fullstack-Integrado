package com.desafio.ejb.service;

import com.desafio.ejb.exception.BeneficioNaoEncontradoException;
import com.desafio.ejb.exception.SaldoInsuficienteException;
import com.desafio.ejb.model.Beneficio;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementação corrigida do serviço EJB de Benefícios.
 *
 * <h2>Bug original identificado</h2>
 * <pre>{@code
 * // BUG: sem validações, sem locking, pode gerar saldo negativo e lost update
 * public void transfer(Long fromId, Long toId, BigDecimal amount) {
 *     Beneficio from = em.find(Beneficio.class, fromId);
 *     Beneficio to   = em.find(Beneficio.class, toId);
 *     from.setValor(from.getValor().subtract(amount));  // saldo pode ficar negativo!
 *     to.setValor(to.getValor().add(amount));
 *     em.merge(from);
 *     em.merge(to);
 * }
 * }</pre>
 *
 * <h2>Problemas corrigidos</h2>
 * <ol>
 *   <li><b>Validação de saldo:</b> lança {@link SaldoInsuficienteException} antes de alterar dados.</li>
 *   <li><b>Pessimistic Locking:</b> usa {@code LockModeType.PESSIMISTIC_WRITE} para bloquear as
 *       linhas no banco, evitando race condition (lost update) em acessos concorrentes.</li>
 *   <li><b>Optimistic Locking:</b> o campo {@code @Version} na entidade garante que se duas
 *       transações paralelas lerem a mesma versão, apenas a primeira confirma — a segunda
 *       recebe {@code OptimisticLockException} e faz rollback automático.</li>
 *   <li><b>Rollback automático:</b> {@code @TransactionAttribute(REQUIRED)} + exceção
 *       unchecked → container EJB efetua rollback caso qualquer erro ocorra.</li>
 *   <li><b>Validação de entrada:</b> IDs e valor são validados antes de qualquer acesso ao banco.</li>
 * </ol>
 *
 * <h2>Estratégia de locking escolhida</h2>
 * <p>Pessimistic Write Lock: mais seguro em cenários de alta contenção onde o conflito
 * é esperado. Para baixa contenção, o Optimistic Lock (via {@code @Version}) já seria
 * suficiente e mais performático.</p>
 */
@Stateless
public class BeneficioEjbServiceBean implements BeneficioEjbService {

    private static final Logger LOG = Logger.getLogger(BeneficioEjbServiceBean.class.getName());

    @PersistenceContext
    EntityManager em; // visibilidade de pacote para facilitar injeção nos testes

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Beneficio> listarTodos() {
        TypedQuery<Beneficio> query = em.createQuery(
                "SELECT b FROM Beneficio b WHERE b.ativo = true ORDER BY b.nome",
                Beneficio.class);
        return query.getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Beneficio buscarPorId(Long id) {
        validarId(id);
        Beneficio beneficio = em.find(Beneficio.class, id);
        if (beneficio == null) {
            throw new BeneficioNaoEncontradoException(id);
        }
        return beneficio;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Beneficio criar(Beneficio beneficio) {
        if (beneficio == null) {
            throw new IllegalArgumentException("Benefício não pode ser nulo");
        }
        em.persist(beneficio);
        em.flush(); // garante que o ID gerado está disponível
        LOG.info(() -> "Benefício criado com id=" + beneficio.getId());
        return beneficio;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Beneficio atualizar(Long id, Beneficio dadosAtualizados) {
        validarId(id);
        Beneficio existente = buscarPorId(id);
        existente.setNome(dadosAtualizados.getNome());
        existente.setDescricao(dadosAtualizados.getDescricao());
        existente.setValor(dadosAtualizados.getValor());
        existente.setAtivo(dadosAtualizados.getAtivo());
        Beneficio salvo = em.merge(existente);
        LOG.info(() -> "Benefício atualizado: id=" + id);
        return salvo;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remover(Long id) {
        validarId(id);
        Beneficio beneficio = buscarPorId(id);
        beneficio.setAtivo(Boolean.FALSE); // soft delete
        em.merge(beneficio);
        LOG.info(() -> "Benefício removido (soft delete): id=" + id);
    }

    // -------------------------------------------------------------------------
    // TRANSFERÊNCIA — método com bug corrigido
    // -------------------------------------------------------------------------

    /**
     * Transferência de saldo com <b>validações</b>, <b>pessimistic locking</b> e
     * <b>rollback automático</b>.
     *
     * <p>Fluxo:
     * <ol>
     *   <li>Valida parâmetros de entrada (IDs e valor &gt; 0).</li>
     *   <li>Carrega as duas entidades com {@code PESSIMISTIC_WRITE} lock —
     *       o banco de dados bloqueia as linhas até o fim da transação,
     *       impedindo leituras e escritas concorrentes nessas linhas.</li>
     *   <li>Verifica se o saldo da origem é suficiente.</li>
     *   <li>Subtrai da origem, soma no destino.</li>
     *   <li>Chama {@code em.flush()} para forçar escritas e detectar conflitos
     *       de versão ({@code OptimisticLockException}) antes do commit.</li>
     * </ol>
     * </p>
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transferir(Long origemId, Long destinoId, BigDecimal valor) {
        // 1. Validação de parâmetros de entrada
        validarId(origemId);
        validarId(destinoId);
        validarValorTransferencia(valor);

        if (origemId.equals(destinoId)) {
            throw new IllegalArgumentException(
                    "Benefício de origem e destino não podem ser iguais");
        }

        LOG.info(() -> String.format(
                "Iniciando transferência: origemId=%d, destinoId=%d, valor=%s",
                origemId, destinoId, valor));

        // 2. Pessimistic Write Lock — bloqueia as linhas no banco para esta transação.
        //    Garante que nenhuma outra transação concorrente leia ou escreva esses registros
        //    antes deste commit. Evita o problema de "lost update".
        Beneficio origem  = em.find(Beneficio.class, origemId,  LockModeType.PESSIMISTIC_WRITE);
        Beneficio destino = em.find(Beneficio.class, destinoId, LockModeType.PESSIMISTIC_WRITE);

        // 3. Verifica existência (em.find retorna null se não encontrar)
        if (origem == null) {
            throw new BeneficioNaoEncontradoException(origemId);
        }
        if (destino == null) {
            throw new BeneficioNaoEncontradoException(destinoId);
        }

        // 4. CORREÇÃO DO BUG: validação de saldo suficiente
        //    O código original não fazia esta verificação → saldo negativo era possível!
        if (origem.getValor().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(
                    String.format(
                            "Saldo insuficiente no benefício id=%d. Saldo atual: %s, Valor solicitado: %s",
                            origemId, origem.getValor(), valor));
        }

        // 5. Atualiza saldos
        origem.setValor(origem.getValor().subtract(valor));
        destino.setValor(destino.getValor().add(valor));

        // merge explícito — o flush/merge dispara a verificação do @Version (optimistic locking).
        // Se outra transação já modificou o registro com uma versão diferente,
        // o JPA lança OptimisticLockException → rollback automático pelo container EJB.
        em.merge(origem);
        em.merge(destino);
        em.flush(); // força escrita imediata e detecção de conflitos de versão

        LOG.info(() -> String.format(
                "Transferência concluída: origemId=%d (novo saldo=%s), destinoId=%d (novo saldo=%s)",
                origemId, origem.getValor(), destinoId, destino.getValor()));
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido: " + id);
        }
    }

    private void validarValorTransferencia(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Valor da transferência deve ser positivo. Recebido: " + valor);
        }
    }
}
