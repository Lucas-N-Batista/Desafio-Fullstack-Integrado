import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Subject, combineLatest }         from 'rxjs';
import { takeUntil, map }                 from 'rxjs/operators';
import { BeneficioStateService }          from '../../store/beneficio.state.service';
import { BeneficioQuery }                 from '../../store/beneficio.query';
import { Beneficio }                      from '../../core/models/beneficio.model';

/**
 * Componente de Transferência de Saldo entre Benefícios.
 *
 * <p>Aplica validação reativa no frontend:
 * <ul>
 *   <li>Origem e destino devem ser diferentes.</li>
 *   <li>Valor deve ser positivo e ≤ saldo da origem.</li>
 *   <li>O saldo da origem é monitorado em tempo real via Akita Query.</li>
 * </ul>
 * </p>
 */
@Component({
  selector:    'app-transferencia',
  templateUrl: './transferencia.component.html',
  styleUrls:   ['./transferencia.component.scss']
})
export class TransferenciaComponent implements OnInit, OnDestroy {

  form!: FormGroup;

  beneficios:       Beneficio[] = [];
  saldoOrigem:      number | null = null;
  transferindo$     = this.query.transferindo$;
  mensagemSucesso$  = this.query.mensagemSucesso$;
  erro$             = this.query.erro$;

  private destroy$ = new Subject<void>();

  constructor(
    private fb:      FormBuilder,
    private service: BeneficioStateService,
    private query:   BeneficioQuery
  ) {}

  ngOnInit(): void {
    // Carrega benefícios ativos se o store estiver vazio
    this.query.carregando$.pipe(takeUntil(this.destroy$)).subscribe();
    this.service.carregarTodos().pipe(takeUntil(this.destroy$)).subscribe();

    // Assina a lista de benefícios ativos do store Akita
    this.query.todosAtivos$
        .pipe(takeUntil(this.destroy$))
        .subscribe(lista => (this.beneficios = lista));

    this.form = this.fb.group({
      origemId:  [null, Validators.required],
      destinoId: [null, Validators.required],
      valor:     [null, [Validators.required, Validators.min(0.01)]]
    }, { validators: this.validarTransferencia.bind(this) });

    // Monitora mudança na origem para exibir saldo disponível
    this.form.get('origemId')!.valueChanges
        .pipe(takeUntil(this.destroy$))
        .subscribe(id => {
          const b = this.beneficios.find(x => x.id === +id);
          this.saldoOrigem = b ? b.valor : null;
          // Dispara revalidação do campo valor ao trocar origem
          this.form.get('valor')?.updateValueAndValidity();
        });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { origemId, destinoId, valor } = this.form.value;
    this.service.transferir({ origemId: +origemId, destinoId: +destinoId, valor: +valor })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next:  () => this.form.reset(),
          error: ()  => {} // erro já tratado no store
        });
  }

  /** Validador de grupo: origem ≠ destino e valor ≤ saldo. */
  private validarTransferencia(group: AbstractControl) {
    const origemId  = group.get('origemId')?.value;
    const destinoId = group.get('destinoId')?.value;
    const valor     = +group.get('valor')?.value;

    const erros: Record<string, boolean> = {};

    if (origemId && destinoId && origemId === destinoId) {
      erros['origemDestinoIguais'] = true;
    }

    if (this.saldoOrigem !== null && valor > this.saldoOrigem) {
      erros['saldoInsuficiente'] = true;
    }

    return Object.keys(erros).length ? erros : null;
  }

  hasError(campo: string, erro: string): boolean {
    const c = this.form.get(campo);
    return !!(c?.hasError(erro) && c.touched);
  }

  hasGroupError(erro: string): boolean {
    return !!(this.form.hasError(erro) && this.form.touched);
  }

  formatarValor(valor: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
  }
}
