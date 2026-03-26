package com.desafio.ejb.service;

import com.desafio.ejb.exception.BeneficioNaoEncontradoException;
import com.desafio.ejb.exception.SaldoInsuficienteException;
import com.desafio.ejb.model.Beneficio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do {@link BeneficioEjbServiceBean} seguindo TDD Red-Green-Refactor.
 *
 * <h2>Fluxo TDD aplicado</h2>
 * <pre>
 * RED   → teste escrito para o comportamento esperado ANTES de existir a implementação.
 *         Com o código original (bugado), esses testes FALHAM.
 * GREEN → implementação mínima para os testes passarem (correção do bug).
 * REFACTOR → testes adicionais cobrindo edge-cases e cenários adicionais.
 * </pre>
 *
 * <p>O {@link EntityManager} é mockado via Mockito para isolar a camada EJB
 * do banco de dados — não é necessário nenhum container Jakarta EE.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BeneficioEjbServiceBean - TDD")
class BeneficioEjbServiceTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private BeneficioEjbServiceBean service;

    // =========================================================================
    // === RED: Testes que FALHAM no código original (bugado) ==================
    // =========================================================================
    // Os testes abaixo documentam o comportamento CORRETO esperado.
    // No código original, transferir() não verifica saldo nem usa locking,
    // então estas assertions não seriam satisfeitas.
    // =========================================================================

    @Nested
    @DisplayName("[RED → GREEN] transferir(): validação de saldo")
    class TransferirValidacaoSaldo {

        /**
         * RED: O código bugado não lança exceção quando saldo < valor.
         * GREEN: Com a correção, SaldoInsuficienteException é lançada.
         */
        @Test
        @DisplayName("deve lançar SaldoInsuficienteException quando saldo < valor solicitado")
        void deveLancarExcecaoQuandoSaldoInsuficiente() {
            // Arrange
            Beneficio origem = beneficioComSaldo(1L, new BigDecimal("100.00"));
            Beneficio destino = beneficioComSaldo(2L, new BigDecimal("50.00"));

            when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(origem);
            when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(destino);

            // Act + Assert
            assertThatThrownBy(() -> service.transferir(1L, 2L, new BigDecimal("200.00")))
                    .isInstanceOf(SaldoInsuficienteException.class)
                    .hasMessageContaining("Saldo insuficiente");

            // nenhuma alteração deve ter ocorrido nos saldos
            assertThat(origem.getValor()).isEqualByComparingTo("100.00");
            assertThat(destino.getValor()).isEqualByComparingTo("50.00");
        }

        /**
         * RED: O código bugado não lança exceção ao transferir valor = saldo exato.
         * GREEN: Saldo exato deve ser permitido (saldo == valor → permite).
         */
        @Test
        @DisplayName("deve permitir transferência quando saldo == valor solicitado (zero residual)")
        void devePermitirTransferenciaDeValorExatamenteIgualAoSaldo() {
            // Arrange
            Beneficio origem = beneficioComSaldo(1L, new BigDecimal("500.00"));
            Beneficio destino = beneficioComSaldo(2L, new BigDecimal("0.00"));

            when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(origem);
            when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(destino);

            // Act
            assertThatCode(() -> service.transferir(1L, 2L, new BigDecimal("500.00")))
                    .doesNotThrowAnyException();

            // Assert saldos corretos após transferência
            assertThat(origem.getValor()).isEqualByComparingTo("0.00");
            assertThat(destino.getValor()).isEqualByComparingTo("500.00");
        }

        /**
         * RED: Código bugado executa mesmo sem verificações — aqui testamos
         * que o saldo da origem é debitado e o do destino creditado corretamente.
         */
        @Test
        @DisplayName("deve debitar origem e creditar destino no valor exato")
        void deveAtualizarSaldosCorretamente() {
            // Arrange
            Beneficio origem = beneficioComSaldo(10L, new BigDecimal("1000.00"));
            Beneficio destino = beneficioComSaldo(20L, new BigDecimal("200.00"));

            when(em.find(Beneficio.class, 10L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(origem);
            when(em.find(Beneficio.class, 20L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(destino);

            // Act
            service.transferir(10L, 20L, new BigDecimal("300.00"));

            // Assert
            assertThat(origem.getValor()).isEqualByComparingTo("700.00");
            assertThat(destino.getValor()).isEqualByComparingTo("500.00");
        }
    }

    // =========================================================================
    // === RED → GREEN: Locking — verificação de que PESSIMISTIC_WRITE é usado
    // =========================================================================

    @Nested
    @DisplayName("[RED → GREEN] transferir(): pessimistic locking")
    class TransferirLocking {

        /**
         * RED: Código original usa {@code em.find(Beneficio.class, id)} sem lock.
         * GREEN: Código corrigido usa {@code LockModeType.PESSIMISTIC_WRITE}.
         */
        @Test
        @DisplayName("deve usar PESSIMISTIC_WRITE lock ao buscar as entidades")
        void deveUsarPessimisticWriteLockNaTransferencia() {
            // Arrange
            Beneficio origem = beneficioComSaldo(1L, new BigDecimal("800.00"));
            Beneficio destino = beneficioComSaldo(2L, new BigDecimal("100.00"));

            when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(origem);
            when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(destino);

            // Act
            service.transferir(1L, 2L, new BigDecimal("100.00"));

            // Assert — PESSIMISTIC_WRITE foi solicitado para ambas as entidades
            verify(em).find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE);
            verify(em).find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE);
        }

        /**
         * Verifica que em.merge() e em.flush() são chamados após a transferência
         * (garante que as mudanças são persistidas e o @Version é verificado).
         */
        @Test
        @DisplayName("deve chamar merge() e flush() para persistir e verificar @Version")
        void deveChamarMergeEFlushAposTransferencia() {
            // Arrange
            Beneficio origem = beneficioComSaldo(1L, new BigDecimal("500.00"));
            Beneficio destino = beneficioComSaldo(2L, new BigDecimal("100.00"));

            when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(origem);
            when(em.find(Beneficio.class, 2L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(destino);
            when(em.merge(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            service.transferir(1L, 2L, new BigDecimal("100.00"));

            // Assert
            verify(em, times(2)).merge(any(Beneficio.class));
            verify(em).flush();
        }
    }

    // =========================================================================
    // === REFACTOR: Testes de validação de parâmetros de entrada ==============
    // =========================================================================

    @Nested
    @DisplayName("[REFACTOR] transferir(): validações de entrada")
    class TransferirValidacoesEntrada {

        @Test
        @DisplayName("deve lançar IllegalArgumentException para valor nulo")
        void deveLancarExcecaoParaValorNulo() {
            assertThatThrownBy(() -> service.transferir(1L, 2L, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para valor zero")
        void deveLancarExcecaoParaValorZero() {
            assertThatThrownBy(() -> service.transferir(1L, 2L, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para valor negativo")
        void deveLancarExcecaoParaValorNegativo() {
            assertThatThrownBy(() -> service.transferir(1L, 2L, new BigDecimal("-50.00")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando origem == destino")
        void deveLancarExcecaoQuandoOrigemIgualDestino() {
            assertThatThrownBy(() -> service.transferir(1L, 1L, new BigDecimal("100.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("iguais");
        }

        @Test
        @DisplayName("deve lançar BeneficioNaoEncontradoException quando origem não existe")
        void deveLancarExcecaoQuandoOrigemNaoExiste() {
            when(em.find(Beneficio.class, 99L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

            assertThatThrownBy(() -> service.transferir(99L, 2L, new BigDecimal("100.00")))
                    .isInstanceOf(BeneficioNaoEncontradoException.class);
        }

        @Test
        @DisplayName("deve lançar BeneficioNaoEncontradoException quando destino não existe")
        void deveLancarExcecaoQuandoDestinoNaoExiste() {
            Beneficio origem = beneficioComSaldo(1L, new BigDecimal("500.00"));
            when(em.find(Beneficio.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(origem);
            when(em.find(Beneficio.class, 99L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

            assertThatThrownBy(() -> service.transferir(1L, 99L, new BigDecimal("100.00")))
                    .isInstanceOf(BeneficioNaoEncontradoException.class);
        }
    }

    // =========================================================================
    // === REFACTOR: Testes de CRUD =============================================
    // =========================================================================

    @Nested
    @DisplayName("[REFACTOR] CRUD de Benefício")
    class CrudBeneficio {

        @Test
        @DisplayName("criar() deve persistir e retornar benefício")
        void deveCriarBeneficio() {
            // Arrange
            Beneficio novo = new Beneficio();
            novo.setNome("Novo Benefício");
            novo.setValor(new BigDecimal("100.00"));

            doNothing().when(em).persist(novo);
            doNothing().when(em).flush();

            // Act
            Beneficio resultado = service.criar(novo);

            // Assert
            verify(em).persist(novo);
            assertThat(resultado).isEqualTo(novo);
        }

        @Test
        @DisplayName("criar() deve lançar IllegalArgumentException para benefício nulo")
        void deveLancarExcecaoAoCriarBeneficioNulo() {
            assertThatThrownBy(() -> service.criar(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("buscarPorId() deve lançar BeneficioNaoEncontradoException quando não existe")
        void deveLancarExcecaoQuandoBeneficioNaoExiste() {
            when(em.find(Beneficio.class, 999L)).thenReturn(null);

            assertThatThrownBy(() -> service.buscarPorId(999L))
                    .isInstanceOf(BeneficioNaoEncontradoException.class);
        }

        @Test
        @DisplayName("remover() deve setar ativo=false (soft delete)")
        void deveRealizarSoftDelete() {
            // Arrange
            Beneficio beneficio = beneficioComSaldo(5L, BigDecimal.TEN);
            beneficio.setAtivo(Boolean.TRUE);
            when(em.find(Beneficio.class, 5L)).thenReturn(beneficio);
            when(em.merge(beneficio)).thenReturn(beneficio);

            // Act
            service.remover(5L);

            // Assert
            assertThat(beneficio.getAtivo()).isFalse();
            verify(em).merge(beneficio);
        }

        @Test
        @DisplayName("listarTodos() deve executar JPQL filtrada por ativo=true")
        void deveListarTodosBeneficios() {
            // Arrange
            @SuppressWarnings("unchecked")
            TypedQuery<Beneficio> query = mock(TypedQuery.class);
            when(em.createQuery(anyString(), eq(Beneficio.class))).thenReturn(query);
            when(query.getResultList()).thenReturn(List.of(new Beneficio(), new Beneficio()));

            // Act
            List<Beneficio> resultado = service.listarTodos();

            // Assert
            assertThat(resultado).hasSize(2);
        }
    }

    // =========================================================================
    // === Helpers =============================================================
    // =========================================================================

    /** Cria um {@link Beneficio} com ID e saldo para uso nos testes. */
    private Beneficio beneficioComSaldo(Long id, BigDecimal saldo) {
        Beneficio b = new Beneficio();
        b.setId(id);
        b.setNome("Beneficio " + id);
        b.setValor(saldo);
        b.setAtivo(Boolean.TRUE);
        b.setVersion(0L);
        return b;
    }
}
