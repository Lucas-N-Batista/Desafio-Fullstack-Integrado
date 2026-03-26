package com.desafio.backend.repository;

import com.desafio.ejb.model.Beneficio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para {@link Beneficio}.
 *
 * <p>Usa a entidade definida no {@code ejb-module} — o mesmo modelo JPA é
 * compartilhado entre o módulo EJB e o backend Spring Boot.</p>
 */
@Repository
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    /** Lista apenas benefícios ativos, ordenados por nome. */
    List<Beneficio> findByAtivoTrueOrderByNome();

    /** Busca ativo por ID (retorna Optional vazio para inativo ou inexistente). */
    Optional<Beneficio> findByIdAndAtivoTrue(Long id);

    /**
     * Busca por ID com Pessimistic Write Lock — mesma estratégia do EJB.
     * Usado na operação de transferência para garantir exclusividade.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Beneficio b WHERE b.id = :id AND b.ativo = true")
    Optional<Beneficio> findByIdForUpdate(@Param("id") Long id);

    /** Verifica duplicidade de nome (case-insensitive). */
    boolean existsByNomeIgnoreCaseAndAtivoTrue(String nome);
}
