package com.desafio.backend.service;

import com.desafio.backend.dto.*;
import com.desafio.backend.exception.*;
import com.desafio.backend.repository.BeneficioRepository;
import com.desafio.ejb.model.Beneficio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Camada de serviço do backend Spring Boot para Benefícios.
 *
 * <p>Integra com o {@code ejb-module} reutilizando a entidade {@link Beneficio}
 * do modelo compartilhado. As mesmas regras de negócio do EJB (validação de saldo,
 * locking) são aplicadas aqui para o contexto Spring Boot + JPA.</p>
 *
 * <p>Usa {@code @Transactional} do Spring em vez de {@code @TransactionAttribute}
 * do EJB — mesma semântica, adaptada ao container Spring.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class BeneficioService {

    private final BeneficioRepository repository;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    /** Retorna todos os benefícios ativos. */
    @Transactional(readOnly = true)
    public List<BeneficioResponseDTO> listarTodos() {
        return repository.findByAtivoTrueOrderByNome()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /** Busca um benefício ativo pelo ID. */
    @Transactional(readOnly = true)
    public BeneficioResponseDTO buscarPorId(Long id) {
        Beneficio beneficio = encontrarAtivo(id);
        return toResponseDTO(beneficio);
    }

    /** Cria um novo benefício. */
    @Transactional
    public BeneficioResponseDTO criar(BeneficioRequestDTO dto) {
        Beneficio beneficio = fromRequestDTO(dto);
        beneficio = repository.save(beneficio);
        log.info("Benefício criado: id={}, nome={}", beneficio.getId(), beneficio.getNome());
        return toResponseDTO(beneficio);
    }

    /** Atualiza os dados de um benefício existente. */
    @Transactional
    public BeneficioResponseDTO atualizar(Long id, BeneficioRequestDTO dto) {
        Beneficio existente = encontrarAtivo(id);
        existente.setNome(dto.getNome());
        existente.setDescricao(dto.getDescricao());
        existente.setValor(dto.getValor());
        existente.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : Boolean.TRUE);
        existente = repository.save(existente);
        log.info("Benefício atualizado: id={}", id);
        return toResponseDTO(existente);
    }

    /** Remove logicamente (soft-delete) um benefício. */
    @Transactional
    public void remover(Long id) {
        Beneficio beneficio = encontrarAtivo(id);
        beneficio.setAtivo(Boolean.FALSE);
        repository.save(beneficio);
        log.info("Benefício removido (soft delete): id={}", id);
    }

    // -------------------------------------------------------------------------
    // TRANSFERÊNCIA — lógica espelhada ao BeneficioEjbServiceBean corrigido
    // -------------------------------------------------------------------------

    /**
     * Transfere {@code valor} do benefício de origem para o de destino.
     *
     * <p>Aplicando as mesmas correções do EJB:
     * <ol>
     *   <li>Valida parâmetros.</li>
     *   <li>Carrega com Pessimistic Lock via {@code findByIdForUpdate}.</li>
     *   <li>Verifica saldo suficiente.</li>
     *   <li>Atualiza ambos os saldos em transação única.</li>
     * </ol>
     * </p>
     */
    @Transactional
    public void transferir(TransferenciaRequestDTO dto) {
        Long origemId  = dto.getOrigemId();
        Long destinoId = dto.getDestinoId();
        BigDecimal valor = dto.getValor();

        if (origemId.equals(destinoId)) {
            throw new IllegalArgumentException(
                    "Benefício de origem e destino não podem ser iguais");
        }

        log.info("Iniciando transferência: origemId={}, destinoId={}, valor={}", origemId, destinoId, valor);

        // Pessimistic Write Lock — bloqueia as linhas durante a transação
        Beneficio origem  = repository.findByIdForUpdate(origemId)
                .orElseThrow(() -> new BeneficioNaoEncontradoException(origemId));
        Beneficio destino = repository.findByIdForUpdate(destinoId)
                .orElseThrow(() -> new BeneficioNaoEncontradoException(destinoId));

        // Validação de saldo (correção do bug original)
        if (origem.getValor().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(
                    String.format(
                            "Saldo insuficiente no benefício id=%d. Saldo atual: %s, Valor solicitado: %s",
                            origemId, origem.getValor(), valor));
        }

        // Atualiza saldos atomicamente dentro da mesma transação
        origem.setValor(origem.getValor().subtract(valor));
        destino.setValor(destino.getValor().add(valor));

        repository.save(origem);
        repository.save(destino);

        log.info("Transferência concluída: origemId={} (novo saldo={}), destinoId={} (novo saldo={})",
                origemId, origem.getValor(), destinoId, destino.getValor());
    }

    // -------------------------------------------------------------------------
    // Helpers de mapeamento (DTO ↔ Entidade)
    // -------------------------------------------------------------------------

    private Beneficio encontrarAtivo(Long id) {
        return repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new BeneficioNaoEncontradoException(id));
    }

    private Beneficio fromRequestDTO(BeneficioRequestDTO dto) {
        Beneficio b = new Beneficio();
        b.setNome(dto.getNome());
        b.setDescricao(dto.getDescricao());
        b.setValor(dto.getValor());
        b.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : Boolean.TRUE);
        return b;
    }

    BeneficioResponseDTO toResponseDTO(Beneficio b) {
        return BeneficioResponseDTO.builder()
                .id(b.getId())
                .nome(b.getNome())
                .descricao(b.getDescricao())
                .valor(b.getValor())
                .ativo(b.getAtivo())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
