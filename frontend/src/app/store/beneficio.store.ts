import { Injectable }       from '@angular/core';
import { EntityState, EntityStore, StoreConfig } from '@datorama/akita';
import { Beneficio }        from '../core/models/beneficio.model';

/**
 * Estado gerenciado pelo Akita para a entidade Benefício.
 *
 * <p>Akita EntityState inclui automaticamente:
 * {@code ids}, {@code entities}, {@code loading}, {@code error}.</p>
 */
export interface BeneficioState extends EntityState<Beneficio, number> {
  /** Filtro de busca atual. */
  filtroNome: string;
  /** Flag de operação de transferência em andamento. */
  transferindo: boolean;
  /** Mensagem de sucesso para exibição. */
  mensagemSucesso: string | null;
}

/** Valor inicial do estado. */
function createInitialState(): Partial<BeneficioState> {
  return {
    filtroNome:      '',
    transferindo:    false,
    mensagemSucesso: null
  };
}

/**
 * Akita EntityStore para Benefícios.
 *
 * <p>Usando {@code resettable: true} para facilitar limpeza de estado em testes.</p>
 */
@Injectable({ providedIn: 'root' })
@StoreConfig({ name: 'beneficios', idKey: 'id', resettable: true })
export class BeneficioStore extends EntityStore<BeneficioState> {

  constructor() {
    super(createInitialState());
  }
}
