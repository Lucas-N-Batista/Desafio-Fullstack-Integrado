package com.desafio.backend.controller;

import com.desafio.backend.dto.*;
import com.desafio.backend.exception.*;
import com.desafio.backend.service.BeneficioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração da camada Controller usando {@link WebMvcTest}.
 *
 * <p>TDD: Testa os contratos HTTP (status codes, JSON de resposta, validações)
 * de todos os endpoints do {@link BeneficioController}.</p>
 */
@WebMvcTest(BeneficioController.class)
@DisplayName("BeneficioController - Testes de Contrato HTTP")
class BeneficioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BeneficioService beneficioService;

    // =========================================================================
    // GET /api/v1/beneficios
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/beneficios → 200 com lista de benefícios")
    void deveRetornarListaDeBeneficios() throws Exception {
        List<BeneficioResponseDTO> lista = List.of(
                beneficioResponseDTO(1L, "Vale Alimentação", "1500.00"),
                beneficioResponseDTO(2L, "Vale Refeição",    "800.00")
        );
        when(beneficioService.listarTodos()).thenReturn(lista);

        mockMvc.perform(get("/api/v1/beneficios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Vale Alimentação"))
                .andExpect(jsonPath("$[1].valor").value(800.00));
    }

    // =========================================================================
    // GET /api/v1/beneficios/{id}
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/beneficios/{id} → 200 quando encontrado")
    void deveRetornarBeneficioPorId() throws Exception {
        when(beneficioService.buscarPorId(1L))
                .thenReturn(beneficioResponseDTO(1L, "Vale Alimentação", "1500.00"));

        mockMvc.perform(get("/api/v1/beneficios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Vale Alimentação"));
    }

    @Test
    @DisplayName("GET /api/v1/beneficios/{id} → 404 quando não encontrado")
    void deveRetornar404QuandoBeneficioNaoExiste() throws Exception {
        when(beneficioService.buscarPorId(99L))
                .thenThrow(new BeneficioNaoEncontradoException(99L));

        mockMvc.perform(get("/api/v1/beneficios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // POST /api/v1/beneficios
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/beneficios → 201 com dados válidos")
    void deveCriarBeneficio() throws Exception {
        BeneficioRequestDTO request = BeneficioRequestDTO.builder()
                .nome("Novo Benefício")
                .valor(new BigDecimal("500.00"))
                .ativo(true)
                .build();

        when(beneficioService.criar(any()))
                .thenReturn(beneficioResponseDTO(10L, "Novo Benefício", "500.00"));

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Novo Benefício"));
    }

    @Test
    @DisplayName("POST /api/v1/beneficios → 400 quando nome está em branco")
    void deveRetornar400QuandoNomeVazio() throws Exception {
        BeneficioRequestDTO request = BeneficioRequestDTO.builder()
                .nome("")                        // nome inválido
                .valor(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios → 400 quando valor é nulo")
    void deveRetornar400QuandoValorNulo() throws Exception {
        BeneficioRequestDTO request = BeneficioRequestDTO.builder()
                .nome("Beneficio Teste")
                .valor(null)                     // valor inválido
                .build();

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // PUT /api/v1/beneficios/{id}
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/beneficios/{id} → 200 com dados válidos")
    void deveAtualizarBeneficio() throws Exception {
        BeneficioRequestDTO request = BeneficioRequestDTO.builder()
                .nome("Nome Atualizado")
                .valor(new BigDecimal("999.00"))
                .ativo(true)
                .build();

        when(beneficioService.atualizar(eq(1L), any()))
                .thenReturn(beneficioResponseDTO(1L, "Nome Atualizado", "999.00"));

        mockMvc.perform(put("/api/v1/beneficios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Atualizado"));
    }

    // =========================================================================
    // DELETE /api/v1/beneficios/{id}
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/beneficios/{id} → 204 quando removido")
    void deveRemoverBeneficio() throws Exception {
        doNothing().when(beneficioService).remover(1L);

        mockMvc.perform(delete("/api/v1/beneficios/1"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // POST /api/v1/transferencias
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/transferencias → 204 com dados válidos")
    void deveTransferirComSucesso() throws Exception {
        TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("300.00"));
        doNothing().when(beneficioService).transferir(any());

        mockMvc.perform(post("/api/v1/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/transferencias → 422 quando saldo insuficiente")
    void deveRetornar422QuandoSaldoInsuficiente() throws Exception {
        TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 2L, new BigDecimal("9999.00"));
        doThrow(new SaldoInsuficienteException("Saldo insuficiente"))
                .when(beneficioService).transferir(any());

        mockMvc.perform(post("/api/v1/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("POST /api/v1/transferencias → 400 quando valor é zero")
    void deveRetornar400QuandoValorTransferenciaZero() throws Exception {
        TransferenciaRequestDTO dto = new TransferenciaRequestDTO(1L, 2L, BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private BeneficioResponseDTO beneficioResponseDTO(Long id, String nome, String valor) {
        return BeneficioResponseDTO.builder()
                .id(id)
                .nome(nome)
                .valor(new BigDecimal(valor))
                .ativo(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
