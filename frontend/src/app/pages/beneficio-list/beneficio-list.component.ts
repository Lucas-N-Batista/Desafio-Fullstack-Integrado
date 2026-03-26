import { Component, OnInit, OnDestroy } from '@angular/core';
import { Observable, Subject }           from 'rxjs';
import { takeUntil }                     from 'rxjs/operators';
import { BeneficioQuery }                from '../../store/beneficio.query';
import { BeneficioStateService }         from '../../store/beneficio.state.service';
import { Beneficio }                     from '../../core/models/beneficio.model';

/**
 * Componente de listagem de benefícios.
 *
 * <p>Consome o estado do {@link BeneficioQuery} (Akita) de forma reativa.
 * Suporta filtro por nome, ações de editar e remover.</p>
 */
@Component({
  selector:    'app-beneficio-list',
  templateUrl: './beneficio-list.component.html',
  styleUrls:   ['./beneficio-list.component.scss']
})
export class BeneficioListComponent implements OnInit, OnDestroy {

  beneficios$:       Observable<Beneficio[]>;
  carregando$:      Observable<boolean>;
  erro$:            Observable<string | null>;
  mensagemSucesso$: Observable<string | null>;
  saldoTotal$:      Observable<number>;

  private destroy$ = new Subject<void>();

  constructor(
    private query:   BeneficioQuery,
    private service: BeneficioStateService
  ) {
    this.beneficios$       = this.query.beneficiosFiltrados$;
    this.carregando$       = this.query.carregando$;
    this.erro$             = this.query.erro$;
    this.mensagemSucesso$  = this.query.mensagemSucesso$;
    this.saldoTotal$       = this.query.saldoTotal$;
  }

  ngOnInit(): void {
    // Carrega a lista ao inicializar o componente
    this.service.carregarTodos()
        .pipe(takeUntil(this.destroy$))
        .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Atualiza filtro no estado Akita. */
  onFiltrar(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;
    this.service.filtrarPorNome(valor);
  }

  /** Remove logicamente um benefício após confirmação do usuário. */
  onRemover(id: number, nome: string): void {
    if (!confirm(`Deseja remover o benefício "${nome}"?`)) return;

    this.service.remover(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe();
  }

  /** Formata valor monetário em BRL. */
  formatarValor(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style:    'currency',
      currency: 'BRL'
    }).format(valor);
  }

  /** Rastreamento por ID para performance do *ngFor. */
  trackById(_index: number, item: Beneficio): number {
    return item.id;
  }
}
