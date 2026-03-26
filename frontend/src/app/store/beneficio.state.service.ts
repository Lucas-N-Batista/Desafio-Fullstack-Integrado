import { Injectable }              from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, tap, finalize }     from 'rxjs/operators';
import { throwError }                    from 'rxjs';
import { BeneficioStore }                from './beneficio.store';
import { Beneficio, BeneficioRequest, TransferenciaRequest }
                                         from '../core/models/beneficio.model';
import { environment }                   from '../../environments/environment';

/**
 * Serviço de estado Akita para Benefícios.
 *
 * <p>Responsável por:
 * <ol>
 *   <li>Consumir a API REST do backend ({@code environment.apiUrl}).</li>
 *   <li>Atualizar o {@link BeneficioStore} reativamente após cada operação.</li>
 *   <li>Gerenciar flags de {@code loading} e {@code error} no store.</li>
 * </ol>
 * </p>
 */
@Injectable({ providedIn: 'root' })
export class BeneficioStateService {

  private readonly api = environment.apiUrl;

  constructor(
    private http:  HttpClient,
    private store: BeneficioStore
  ) {}

  /** Carrega todos os benefícios do backend e atualiza o store. */
  carregarTodos() {
    this.store.setLoading(true);
    this.store.setError(null);

    return this.http.get<Beneficio[]>(`${this.api}/beneficios`).pipe(
      tap(lista  => this.store.set(lista)),
      catchError(err => this.tratarErro(err)),
      finalize(()    => this.store.setLoading(false))
    );
  }

  /** Cria um novo benefício e adiciona ao store. */
  criar(dados: BeneficioRequest) {
    this.store.setError(null);

    return this.http.post<Beneficio>(`${this.api}/beneficios`, dados).pipe(
      tap(novo  => {
        this.store.add(novo);
        this.store.update({ mensagemSucesso: `Benefício "${novo.nome}" criado com sucesso!` });
        this.limparMensagemSucessoApos(3000);
      }),
      catchError(err => this.tratarErro(err))
    );
  }

  /** Atualiza um benefício existente no store. */
  atualizar(id: number, dados: BeneficioRequest) {
    this.store.setError(null);

    return this.http.put<Beneficio>(`${this.api}/beneficios/${id}`, dados).pipe(
      tap(atualizado => {
        this.store.upsert(id, atualizado);
        this.store.update({ mensagemSucesso: `Benefício "${atualizado.nome}" atualizado!` });
        this.limparMensagemSucessoApos(3000);
      }),
      catchError(err => this.tratarErro(err))
    );
  }

  /** Remove (soft delete) um benefício e atualiza o store. */
  remover(id: number) {
    this.store.setError(null);

    return this.http.delete<void>(`${this.api}/beneficios/${id}`).pipe(
      tap(() => {
        // Atualiza o registro como inativo (soft delete) no estado local
        this.store.update(id, { ativo: false });
        this.store.update({ mensagemSucesso: 'Benefício removido com sucesso!' });
        this.limparMensagemSucessoApos(3000);
      }),
      catchError(err => this.tratarErro(err))
    );
  }

  /**
   * Executa transferência de saldo.
   * Em caso de sucesso, recarrega as entidades envolvidas para refletir novos saldos.
   */
  transferir(payload: TransferenciaRequest) {
    this.store.update({ transferindo: true });
    this.store.setError(null);

    return this.http.post<void>(`${this.api}/transferencias`, payload).pipe(
      tap(() => {
        this.store.update({ mensagemSucesso: `Transferência de R$ ${payload.valor.toFixed(2)} realizada com sucesso!` });
        this.limparMensagemSucessoApos(4000);
        // Recarrega o estado para obter saldos atualizados
        this.carregarTodos().subscribe();
      }),
      catchError(err => this.tratarErro(err)),
      finalize(() => this.store.update({ transferindo: false }))
    );
  }

  /** Atualiza o filtro de busca por nome no estado. */
  filtrarPorNome(nome: string) {
    this.store.update({ filtroNome: nome });
  }

  /** Limpa o erro do store. */
  limparErro() {
    this.store.setError(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers privados
  // ---------------------------------------------------------------------------

  private tratarErro(err: HttpErrorResponse) {
    let mensagem = 'Ocorreu um erro inesperado.';

    if (err.error?.message) {
      mensagem = err.error.message;
    } else if (err.status === 404) {
      mensagem = 'Recurso não encontrado.';
    } else if (err.status === 422) {
      mensagem = 'Saldo insuficiente para realizar a transferência.';
    } else if (err.status === 409) {
      mensagem = 'Conflito: o registro foi modificado por outra operação. Tente novamente.';
    } else if (err.status === 400) {
      mensagem = 'Dados inválidos. Verifique os campos e tente novamente.';
    }

    this.store.setError(mensagem);
    return throwError(() => new Error(mensagem));
  }

  private limparMensagemSucessoApos(ms: number) {
    setTimeout(() => this.store.update({ mensagemSucesso: null }), ms);
  }
}
