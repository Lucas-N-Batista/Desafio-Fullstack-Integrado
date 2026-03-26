# 🏗️ Desafio Fullstack Integrado

Solução completa em camadas corrigindo o bug de locking/saldo no EJB e entregando
CRUD + transferência funcional com frontend reativo.

---

## 📦 Estrutura do Projeto

```
desafio-fullstack/
├── db/                         # Scripts SQL (schema + seed)
├── ejb-module/                 # EJB puro Jakarta EE 10 (bug corrigido)
├── backend-module/             # API REST Spring Boot 3 + Swagger
├── frontend/                   # Angular 18 + Akita state management
├── docs/                       # Documentação
├── .github/workflows/          # CI GitHub Actions
├── docker-compose.yml          # Orquestração de containers
└── pom.xml                     # Maven multi-módulo (parent)
```

---

## 🐞 Bug Corrigido – `BeneficioEjbServiceBean.transferir()`

### Código original (bugado)
```java
// BUG: sem validações, sem locking, pode gerar saldo negativo e lost update
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    Beneficio from = em.find(Beneficio.class, fromId);
    Beneficio to   = em.find(Beneficio.class, toId);
    from.setValor(from.getValor().subtract(amount));  // saldo pode ficar negativo!
    to.setValor(to.getValor().add(amount));
    em.merge(from);
    em.merge(to);
}
```

### Problemas identificados
| Problema | Consequência |
|----------|-------------|
| Sem validação de saldo | Saldo pode ficar negativo |
| Sem verificação de existência | NullPointerException em runtime |
| Sem Pessimistic Lock | Lost update em acessos concorrentes |
| Sem verificação `@Version` | Optimistic lock jamais ativado |
| Valor não validado | Transfer de R$ 0 ou negativo possível |

### Correção aplicada
```java
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public void transferir(Long origemId, Long destinoId, BigDecimal valor) {
    // 1. Valida parâmetros de entrada
    validarId(origemId); validarId(destinoId); validarValor(valor);
    if (origemId.equals(destinoId)) throw new IllegalArgumentException(...);

    // 2. Pessimistic Write Lock — bloqueia as linhas no banco
    Beneficio origem  = em.find(Beneficio.class, origemId,  LockModeType.PESSIMISTIC_WRITE);
    Beneficio destino = em.find(Beneficio.class, destinoId, LockModeType.PESSIMISTIC_WRITE);

    // 3. Verifica existência
    if (origem == null)  throw new BeneficioNaoEncontradoException(origemId);
    if (destino == null) throw new BeneficioNaoEncontradoException(destinoId);

    // 4. Validação de saldo
    if (origem.getValor().compareTo(valor) < 0)
        throw new SaldoInsuficienteException(...);

    // 5. Atualização + flush (detecta OptimisticLockException via @Version)
    origem.setValor(origem.getValor().subtract(valor));
    destino.setValor(destino.getValor().add(valor));
    em.merge(origem); em.merge(destino);
    em.flush();
}
```

### Estratégia de locking
- **Pessimistic Write Lock** (`LockModeType.PESSIMISTIC_WRITE`): bloqueia as linhas no
  banco durante toda a transação. Ideal para alta contenção.
- **Optimistic Lock** (`@Version Long version` na entidade): se duas transações leram a
  mesma versão e tentam atualizar, a segunda recebe `OptimisticLockException` → rollback
  automático. Ideal para baixa contenção.

---

## 🚀 Setup e Deploy

### Pré-requisitos
- Docker 24+ e Docker Compose v2
- Java 17 (baseline oficial do projeto para build local)
- Node.js 20+ (para build local do frontend)

> Importante: este projeto permanece em `Java 17`. O uso de `Java 21` pode ser feito no sistema,
> mas o workspace, Maven e o Java Language Server devem estar alinhados com `JavaSE-17`.

### 1. Deploy com Docker (recomendado)
```bash
# Clona o repositório
git clone https://github.com/SEU_USUARIO/desafio-fullstack-integrado.git
cd desafio-fullstack-integrado

# Sobe toda a stack (db + backend + frontend)
docker compose up --build -d

# Aguarda health checks...
docker compose ps
```

**Serviços disponíveis:**

| Serviço | URL |
|---------|-----|
| Frontend Angular | http://localhost:4200 |
| API REST (Swagger UI) | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| PostgreSQL | localhost:5432 (db: desafio, user: desafio) |

### 2. Build e testes locais

```bash
# Build completo + testes (EJB + Backend)
mvn clean verify

# Apenas testes do EJB
mvn test -pl ejb-module

# Apenas testes do Backend
mvn test -pl backend-module

# Frontend: instalar dependências e rodar testes
cd frontend
npm ci --legacy-peer-deps
npm test

# Frontend: build de produção
npm run build
```

### 3. Banco de dados manual (sem Docker)
```bash
# PostgreSQL deve estar rodando em localhost:5432
psql -U postgres -c "CREATE DATABASE desafio; CREATE USER desafio WITH PASSWORD 'desafio'; GRANT ALL ON DATABASE desafio TO desafio;"
psql -U desafio -d desafio -f db/schema.sql
psql -U desafio -d desafio -f db/seed.sql
```

---

## 📡 Endpoints da API

### Benefícios
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/beneficios` | Lista todos os benefícios ativos |
| `GET` | `/api/v1/beneficios/{id}` | Busca por ID |
| `POST` | `/api/v1/beneficios` | Cria novo benefício |
| `PUT` | `/api/v1/beneficios/{id}` | Atualiza benefício |
| `DELETE` | `/api/v1/beneficios/{id}` | Remove (soft delete) |

### Transferência
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/v1/transferencias` | Transfere saldo entre benefícios |

### Exemplo de transferência
```json
POST /api/v1/transferencias
{
  "origemId":  1,
  "destinoId": 2,
  "valor":     300.00
}
```

---

## 🧪 Metodologia TDD

Todos os testes seguem **Red-Green-Refactor**:

1. 🔴 **RED**: Teste escrito para o comportamento esperado → falha no código original/stub
2. 🟢 **GREEN**: Implementação mínima para o teste passar
3. 🔵 **REFACTOR**: Melhoria do código + testes de edge-cases

### Cobertura de testes
| Módulo | Testes | Estratégia |
|--------|--------|-----------|
| `ejb-module` | `BeneficioEjbServiceTest` | JUnit 5 + Mockito (mock do EntityManager) |
| `backend-module/service` | `BeneficioServiceTest` | JUnit 5 + Mockito (mock do Repository) |
| `backend-module/controller` | `BeneficioControllerTest` | @WebMvcTest + MockMvc |
| `frontend` | `BeneficioStateService.spec.ts` | Jasmine + HttpClientTestingModule |

---

## 📊 Critérios de Avaliação

| Critério | Implementação |
|----------|--------------|
| **Arquitetura em camadas (20%)** | DB → EJB Module → Backend REST → Frontend. Docker compõe todas as camadas. |
| **Correção EJB (20%)** | `BeneficioEjbServiceBean.transferir()` com validação de saldo, PESSIMISTIC_WRITE lock, `@Version` e rollback automático. |
| **CRUD + Transferência (15%)** | 5 endpoints REST + `POST /transferencias` com `@Valid`. |
| **Qualidade de código (10%)** | Lombok, exceções customizadas, logs SLF4J, DTOs separados, Clean Code. |
| **Testes (15%)** | TDD em todas as camadas, > 80% cobertura de serviços. |
| **Documentação (10%)** | Swagger UI, README completo, comentários PT-BR. |
| **Frontend (10%)** | Angular 18, Akita store, formulários reativos, validação de saldo em tempo real. |

---

## 🏛️ Arquitetura

```
                    ┌───────────────────┐
                    │  Frontend Angular  │
                    │   (Akita Store)    │
                    └────────┬──────────┘
                             │ HTTP REST
                    ┌────────▼──────────┐
                    │  Backend Spring    │
                    │  Boot REST API     │
                    │  (BeneficioService)│
                    └────────┬──────────┘
                             │ JPA / JDBC
                    ┌────────▼──────────┐
                    │  EJB Module       │
                    │  (corrigido)      │
                    │  @Stateless EJB   │
                    └────────┬──────────┘
                             │ SQL
                    ┌────────▼──────────┐
                    │  PostgreSQL 15    │
                    │  (schema + seed)  │
                    └───────────────────┘
```

---

## 🔒 Segurança

- Usuário não-root no container Docker
- Validação de entrada com `@Valid` (JSR-380)
- Sem exposição de campos sensíveis nos DTOs
- SQL via JPA (sem SQL literal / injeção)
- `DDL-auto: validate` em produção (schema estável)
