import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router }             from '@angular/router';
import { Subject }                            from 'rxjs';
import { takeUntil }                          from 'rxjs/operators';
import { BeneficioStateService }              from '../../store/beneficio.state.service';
import { BeneficioQuery }                     from '../../store/beneficio.query';

/**
 * Componente de formulário para criação e edição de Benefícios.
 *
 * <p>No modo edição, detecta o parâmetro {@code id} na rota e
 * pré-preenche o formulário com os dados do estado Akita.</p>
 */
@Component({
  selector:    'app-beneficio-form',
  templateUrl: './beneficio-form.component.html',
  styleUrls:   ['./beneficio-form.component.scss']
})
export class BeneficioFormComponent implements OnInit, OnDestroy {

  form!:       FormGroup;
  editandoId:  number | null = null;
  salvando  = false;
  erroMsg:    string | null  = null;

  private destroy$ = new Subject<void>();

  get modoEdicao(): boolean { return this.editandoId !== null; }
  get titulo():     string  { return this.modoEdicao ? '✏️ Editar Benefício' : '➕ Novo Benefício'; }

  constructor(
    private fb:      FormBuilder,
    private route:   ActivatedRoute,
    private router:  Router,
    private service: BeneficioStateService,
    private query:   BeneficioQuery
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nome:      ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      descricao: ['', [Validators.maxLength(255)]],
      valor:     [null, [Validators.required, Validators.min(0)]],
      ativo:     [true]
    });

    // Verifica se é modo edição pelo parâmetro :id na rota
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editandoId = +idParam;
      this.query.beneficioPorId$(this.editandoId)
          .pipe(takeUntil(this.destroy$))
          .subscribe(b => {
            if (b) {
              this.form.patchValue({
                nome:      b.nome,
                descricao: b.descricao ?? '',
                valor:     b.valor,
                ativo:     b.ativo
              });
            }
          });
    }
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

    this.salvando = true;
    this.erroMsg  = null;

    const dados = this.form.value;
    const op$ = this.modoEdicao
        ? this.service.atualizar(this.editandoId!, dados)
        : this.service.criar(dados);

    op$.pipe(takeUntil(this.destroy$))
       .subscribe({
         next:  ()    => this.router.navigate(['/beneficios']),
         error: (err) => { this.erroMsg = err.message; this.salvando = false; }
       });
  }

  onCancelar(): void {
    this.router.navigate(['/beneficios']);
  }

  /** Helpers para validação no template */
  campo(name: string) { return this.form.get(name)!; }
  hasError(name: string, erro: string) {
    return this.campo(name).hasError(erro) && this.campo(name).touched;
  }
}
