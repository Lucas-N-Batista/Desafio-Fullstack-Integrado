package com.desafio.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Tratador global de exceções da API REST.
 *
 * <p>Garante que todos os erros retornem um JSON padronizado com campos:
 * {@code timestamp}, {@code status}, {@code error}, {@code message} e {@code path}.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Benefício não encontrado → 404 */
    @ExceptionHandler(BeneficioNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            BeneficioNaoEncontradoException ex, WebRequest req) {
        log.warn("Benefício não encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    /** Saldo insuficiente → 422 */
    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErroResponse> handleSaldoInsuficiente(
            SaldoInsuficienteException ex, WebRequest req) {
        log.warn("Saldo insuficiente: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    /** Argumento inválido → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest req) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    /** Conflito de versão (optimistic lock) → 409 */
    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public ResponseEntity<ErroResponse> handleOptimisticLock(
            jakarta.persistence.OptimisticLockException ex, WebRequest req) {
        log.error("Conflito de versão (optimistic lock): {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "Conflito de dados: o registro foi modificado por outra operação. Tente novamente.", req);
    }

    /** Erro genérico → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex, WebRequest req) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Tente novamente mais tarde.", req);
    }

    /** Erros de validação (@Valid) → 400 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest req) {

        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo   = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            String mensagem = error.getDefaultMessage();
            erros.put(campo, mensagem);
        });

        ErroResponse body = new ErroResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                erros.toString(),
                req.getDescription(false).replace("uri=", "")
        );
        log.warn("Validação falhou: {}", erros);
        return ResponseEntity.badRequest().body(body);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<ErroResponse> buildResponse(
            HttpStatus status, String mensagem, WebRequest req) {
        ErroResponse body = new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                req.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(status).body(body);
    }

    // DTO de erro padronizado
    public record ErroResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}
}
