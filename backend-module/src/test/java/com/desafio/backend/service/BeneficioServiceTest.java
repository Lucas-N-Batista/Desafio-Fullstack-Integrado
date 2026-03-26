package com.desafio.backend.service;

import com.desafio.backend.dto.*;
import com.desafio.backend.exception.*;
import com.desafio.backend.repository.BeneficioRepository;
import com.desafio.ejb.model.Beneficio;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do {@link BeneficioService} — TDD Red-Green-Refactor.
 *
 * <h2>Estratégia</h2>
 * <ul>
 *   <li>RED: Testes escritos antes da implementação → falham no código stub.</li>
 *   <li>GREEN: Implementação mínima → testes passam.</li>
 *   <li>REFACTOR: Edge-cases e cenários adicionais após refatoração.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BeneficioService - TDD Unitário")
class BeneficioServiceTest {

    @Mock
    private BeneficioRepository repository;

    @InjectMocks
    private BeneficioService service;

    // =========================================================================
    // [RED → GREEN] transferir()
    // =========================================================================

    @Nested
    @DisplayName("[RED → GREEN] transferir()")
    class TransferirTDD {

        /**
         * RED: Sem validação de saldo no código original → não lançava exceção.
         * GREEN: Agora lança SaldoInsuficienteException.
         */
        @Test
        @DisplayName("deve lançar SaldoInsuficienteException quando saldo insuficiente")
        void deveLancarExcecaoQuandoSaldoInsuficiente() {
            // Arrange
            Beneficio origem  = beneficio(1L, "1000.00");
            Beneficio destino = beneficio(2L, "0.00");
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(origem));
            when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(destino));

            TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("1500.00"));

            // Act + Assert
            assertThatThrownBy(() -> service.transferir(dto))
                    .isInstanceOf(SaldoInsuficienteException.class)
                    .hasMessageContaining("Saldo insuficiente");
        }

        /**
         * GREEN: Transferência válida deve debitar origem e creditar destino.
         */
        @Test
        @DisplayName("deve debitar origem e creditar destino corretamente")
        void deveTransferirCorretamente() {
            // Arrange
            Beneficio origem  = beneficio(1L, "800.00");
            Beneficio destino = beneficio(2L, "200.00");
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(origem));
            when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(destino));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("300.00"));

            // Act
            service.transferir(dto);

            // Assert
            assertThat(origem.getValor()).isEqualByComparingTo("500.00");
            assertThat(destino.getValor()).isEqualByComparingTo("500.00");
            verify(repository, times(2)).save(any(Beneficio.class));
        }

        @Test
        @DisplayName("deve lançar BeneficioNaoEncontradoException para origem inexistente")
        void deveLancarExcecaoOrigemNaoExiste() {
            when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            TransferenciaRequestDTO dto = new TransferenciaRequestDTO(99L, 2L, new BigDecimal("100.00"));

            assertThatThrownBy(() -> service.transferir(dto))
                    .isInstanceOf(BeneficioNaoEncontradoException.class);
        }

        @Test
        @DisplayName("deve lançar BeneficioNaoEncontradoException para destino inexistente")
        void deveLancarExcecaoDestinoNaoExiste() {
            Beneficio origem = beneficio(1L, "500.00");
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(origem));
            when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 99L, new BigDecimal("100.00"));

            assertThatThrownBy(() -> service.transferir(dto))
                    .isInstanceOf(BeneficioNaoEncontradoException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando origem == destino")
        void deveLancarExcecaoOrigemIgualDestino() {
            TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 1L, new BigDecimal("100.00"));
            assertThatThrownBy(() -> service.transferir(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // [RED → GREEN] criar()
    // =========================================================================

    @Nested
    @DisplayName("[RED → GREEN] criar()")
    class CriarTDD {

        @Test
        @DisplayName("deve criar benefício e retornar DTO com todos os campos")
        void deveCriarBeneficio() {
            // Arrange
            BeneficioRequestDTO dto = BeneficioRequestDTO.builder()
                    .nome("Vale Alimentação")
                    .descricao("Benefício mensal")
                    .valor(new BigDecimal("1500.00"))
                    .ativo(true)
                    .build();

            Beneficio salvo = beneficio(1L, "1500.00");
            salvo.setNome("Vale Alimentação");
            salvo.setDescricao("Benefício mensal");

            when(repository.save(any(Beneficio.class))).thenReturn(salvo);

            // Act
            BeneficioResponseDTO resultado = service.criar(dto);

            // Assert
            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getNome()).isEqualTo("Vale Alimentação");
            assertThat(resultado.getValor()).isEqualByComparingTo("1500.00");
        }
    }

    // =========================================================================
    // [RED → GREEN] buscarPorId()
    // =========================================================================

    @Nested
    @DisplayName("[RED → GREEN] buscarPorId()")
    class BuscarPorIdTDD {

        @Test
        @DisplayName("deve retornar DTO quando benefício existe e está ativo")
        void deveRetornarBeneficioAtivo() {
            Beneficio beneficio = beneficio(5L, "200.00");
            when(repository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(beneficio));

            BeneficioResponseDTO resultado = service.buscarPorId(5L);

            assertThat(resultado.getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("deve lançar BeneficioNaoEncontradoException quando não existe")
        void deveLancarExcecaoNaoExiste() {
            when(repository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(BeneficioNaoEncontradoException.class);
        }
    }

    // =========================================================================
    // [REFACTOR] atualizar() e remover()
    // =========================================================================

    @Nested
    @DisplayName("[REFACTOR] atualizar() e remover()")
    class AtualizarRemoverRefactor {

        @Test
        @DisplayName("atualizar() deve salvar os novos dados")
        void deveAtualizarBeneficio() {
            Beneficio existente = beneficio(3L, "500.00");
            when(repository.findByIdAndAtivoTrue(3L)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BeneficioRequestDTO dto = BeneficioRequestDTO.builder()
                    .nome("Novo Nome")
                    .valor(new BigDecimal("600.00"))
                    .ativo(true)
                    .build();

            BeneficioResponseDTO resultado = service.atualizar(3L, dto);

            assertThat(resultado.getNome()).isEqualTo("Novo Nome");
            assertThat(resultado.getValor()).isEqualByComparingTo("600.00");
        }

        @Test
        @DisplayName("remover() deve setar ativo=false")
        void deveRemoverBeneficio() {
            Beneficio existente = beneficio(4L, "100.00");
            when(repository.findByIdAndAtivoTrue(4L)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.remover(4L);

            assertThat(existente.getAtivo()).isFalse();
            verify(repository).save(existente);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Beneficio beneficio(Long id, String valor) {
        Beneficio b = new Beneficio();
        b.setId(id);
        b.setNome("Beneficio " + id);
        b.setValor(new BigDecimal(valor));
        b.setAtivo(Boolean.TRUE);
        b.setVersion(0L);
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        return b;
    }
}
