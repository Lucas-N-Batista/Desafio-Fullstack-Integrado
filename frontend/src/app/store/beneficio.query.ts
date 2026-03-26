import { Injectable }          from '@angular/core';
import { QueryEntity }         from '@datorama/akita';
import { BeneficioState, BeneficioStore } from './beneficio.store';
import { Beneficio }           from '../core/models/beneficio.model';
import { combineLatest, map }  from 'rxjs';

/**
 * Akita QueryEntity para leitura reativa do estado de Benefícios.
 *
 * <p>Provê observables derivados usados diretamente nos componentes.</p>
 */
@Injectable({ providedIn: 'root' })
export class BeneficioQuery extends QueryEntity<BeneficioState> {

  constructor(protected override store: BeneficioStore) {
    super(store);
  }

  /** Todos os benefícios ativos do estado. */
  todosAtivos$ = this.selectAll({
    filterBy: b => b.ativo === true
  });

  /** Lista filtrada pelo campo {@code filtroNome} do estado. */
  beneficiosFiltrados$ = combineLatest([
    this.selectAll(),
    this.select('filtroNome')
  ]).pipe(
    map(([beneficios, filtro]) =>
      filtro
        ? beneficios.filter(b =>
            b.nome.toLowerCase().includes(filtro.toLowerCase()))
        : beneficios
    )
  );

  /** Loading geral (carregando lista). */
  carregando$ = this.selectLoading();

  /** Loading específico da operação de transferência. */
  transferindo$ = this.select('transferindo');

  /** Erro do estado (null quando não há erro). */
  erro$ = this.selectError<string>();

  /** Mensagem de sucesso. */
  mensagemSucesso$ = this.select('mensagemSucesso');

  /** Total de benefícios no estado. */
  total$ = this.selectCount();

  /** Benefício pelo ID. */
  beneficioPorId$(id: number) {
    return this.selectEntity(id);
  }

  /** Soma total de todos os saldos. */
  saldoTotal$ = this.selectAll().pipe(
    map(lista => lista.reduce((soma, b) => soma + b.valor, 0))
  );
}
