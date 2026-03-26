package com.desafio.ejb.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um Benefício.
 *
 * <p>O campo {@code version} habilita <b>Optimistic Locking</b> via {@link Version},
 * garantindo que duas transações concorrentes não sobrescrevam silenciosamente
 * o mesmo registro — a segunda lançará {@link jakarta.persistence.OptimisticLockException}.</p>
 *
 * <p>Para <b>Pessimistic Locking</b> (exclusivo, bloqueia a linha no banco),
 * o serviço EJB usa {@code LockModeType.PESSIMISTIC_WRITE} ao chamar
 * {@code em.find(..., LockModeType.PESSIMISTIC_WRITE)}.</p>
 */
@Entity
@Table(name = "BENEFICIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "version")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Beneficio {

    /** Identificador gerado pelo banco (BIGSERIAL / IDENTITY). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Nome do benefício (obrigatório, máx. 100 chars). */
    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    /** Descrição detalhada (opcional, máx. 255 chars). */
    @Column(name = "DESCRICAO", length = 255)
    private String descricao;

    /**
     * Saldo/valor do benefício.
     * <p>Nunca deve ser negativo — regra de negócio garantida pelo
     * {@code CHECK (VALOR >= 0)} no schema e pela validação no serviço.</p>
     */
    @Column(name = "VALOR", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    /** Indica se o benefício está ativo. */
    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo = Boolean.TRUE;

    /**
     * Campo de versão para <b>Optimistic Locking</b>.
     * O JPA incrementa automaticamente este valor a cada {@code merge()}.
     * Se duas transações leram a mesma versão e tentam atualizar, a segunda falha.
     */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version = 0L;

    /** Data de criação do registro (imutável após inserção). */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Data da última atualização do registro. */
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Atualiza {@code updatedAt} automaticamente antes de cada update JPA. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
