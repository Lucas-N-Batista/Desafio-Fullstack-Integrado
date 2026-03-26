import { TestBed }          from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BeneficioStateService } from './beneficio.state.service';
import { BeneficioStore }        from './beneficio.store';
import { BeneficioQuery }        from './beneficio.query';
import { Beneficio }             from '../core/models/beneficio.model';

/**
 * Testes unitários do {@link BeneficioStateService}.
 *
 * <p>TDD: verifica que o store Akita é atualizado corretamente
 * após cada operação HTTP.</p>
 */
describe('BeneficioStateService', () => {

  let service:    BeneficioStateService;
  let httpMock:   HttpTestingController;
  let store:      BeneficioStore;
  let query:      BeneficioQuery;

  const mockBeneficios: Beneficio[] = [
    { id: 1, nome: 'Vale Alimentação', valor: 1500, ativo: true },
    { id: 2, nome: 'Vale Refeição',    valor: 800,  ativo: true }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports:   [HttpClientTestingModule],
      providers: [BeneficioStateService, BeneficioStore, BeneficioQuery]
    });

    service  = TestBed.inject(BeneficioStateService);
    httpMock = TestBed.inject(HttpTestingController);
    store    = TestBed.inject(BeneficioStore);
    query    = TestBed.inject(BeneficioQuery);

    // Reseta o store antes de cada teste
    store.reset();
  });

  afterEach(() => httpMock.verify());

  // ==========================================================================
  // carregarTodos()
  // ==========================================================================

  it('[RED→GREEN] carregarTodos() deve popular o store com os benefícios retornados', () => {
    service.carregarTodos().subscribe();

    const req = httpMock.expectOne('/api/v1/beneficios');
    expect(req.request.method).toBe('GET');
    req.flush(mockBeneficios);

    let total = 0;
    query.selectCount().subscribe(n => (total = n));
    expect(total).toBe(2);
  });

  it('[RED→GREEN] carregarTodos() deve setar loading=false após completar', () => {
    let carregando = true;
    service.carregarTodos().subscribe();
    httpMock.expectOne('/api/v1/beneficios').flush(mockBeneficios);

    query.carregando$.subscribe(v => (carregando = v));
    expect(carregando).toBeFalse();
  });

  // ==========================================================================
  // criar()
  // ==========================================================================

  it('[RED→GREEN] criar() deve adicionar o novo benefício ao store', () => {
    const novo = { nome: 'Gympass', valor: 200, ativo: true, descricao: '' };
    const resposta: Beneficio = { id: 10, ...novo };

    service.criar(novo).subscribe();
    httpMock.expectOne('/api/v1/beneficios').flush(resposta);

    let encontrado: Beneficio | undefined;
    query.beneficioPorId$(10).subscribe(b => (encontrado = b));
    expect(encontrado?.id).toBe(10);
    expect(encontrado?.nome).toBe('Gympass');
  });

  // ==========================================================================
  // remover()
  // ==========================================================================

  it('[RED→GREEN] remover() deve marcar benefício como inativo no store', () => {
    // Prepara estado com benefícios
    store.set(mockBeneficios);

    service.remover(1).subscribe();
    httpMock.expectOne('/api/v1/beneficios/1').flush(null, { status: 204, statusText: 'No Content' });

    let beneficio: Beneficio | undefined;
    query.beneficioPorId$(1).subscribe(b => (beneficio = b));
    expect(beneficio?.ativo).toBeFalse();
  });

  // ==========================================================================
  // transferir()
  // ==========================================================================

  it('[RED→GREEN] transferir() deve chamar o endpoint correto com payload', () => {
    const payload = { origemId: 1, destinoId: 2, valor: 300 };

    // Stub para o recarregamento que ocorre após a transferência
    service.transferir(payload).subscribe();

    const reqTransfer = httpMock.expectOne('/api/v1/transferencias');
    expect(reqTransfer.request.method).toBe('POST');
    expect(reqTransfer.request.body).toEqual(payload);
    reqTransfer.flush(null, { status: 204, statusText: 'No Content' });

    // Recarregamento automático após transferência
    const reqReload = httpMock.expectOne('/api/v1/beneficios');
    reqReload.flush(mockBeneficios);
  });

  it('[REFACTOR] tratarErro() deve setar mensagem de erro 422 para saldo insuficiente', () => {
    service.transferir({ origemId: 1, destinoId: 2, valor: 9999 }).subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/v1/transferencias');
    req.flush({ message: 'Saldo insuficiente' }, { status: 422, statusText: 'Unprocessable Entity' });

    let erro: string | null = null;
    query.erro$.subscribe(e => (erro = e as string | null));
    expect(erro).toContain('Saldo insuficiente');
  });
});
