# 🏗️ Desafio Fullstack Integrado

[![CI](https://github.com/SEU_USUARIO/desafio-fullstack-integrado/actions/workflows/ci.yml/badge.svg)](https://github.com/SEU_USUARIO/desafio-fullstack-integrado/actions)

> Solução completa em camadas: banco de dados PostgreSQL, módulo EJB (bug corrigido),
> API REST Spring Boot e frontend Angular 18 com Akita state management.

## ☕ Baseline Java

Este projeto deve ser mantido em `Java 17`.

- `Java 17` é a baseline oficial do projeto para desenvolvimento local, build, testes e CI.
- `Java 21` não é o alvo atual desta solução.
- Se o VS Code estiver usando um JDK mais novo, alinhe o workspace para `JavaSE-17` em vez de alterar o projeto.

### Configuração local recomendada

```powershell
java -version
where.exe java
```

Saída esperada: JDK `17.x`, por exemplo `Temurin 17`.

No VS Code:

1. Execute `Java: Configure Java Runtime`
2. Aponte o workspace para o JDK 17
3. Execute `Maven: Reload Projects`
4. Execute `Java: Clean Java Language Server Workspace`

## 🐞 Bug Corrigido

O método `BeneficioEjbServiceBean.transferir()` no repositório original **não verificava
saldo**, **não usava locking** e **podia gerar saldo negativo** em acessos concorrentes.

**Correções aplicadas:**
- ✅ Validação de saldo suficiente antes de debitar
- ✅ `LockModeType.PESSIMISTIC_WRITE` para evitar *lost update* concorrente
- ✅ Campo `@Version` na entidade para *Optimistic Locking*
- ✅ `@TransactionAttribute(REQUIRED)` + rollback automático em exceção unchecked
- ✅ Validação de todos os parâmetros de entrada

## 🚀 Deploy Rápido

```bash
git clone https://github.com/SEU_USUARIO/desafio-fullstack-integrado.git
cd desafio-fullstack-integrado
docker compose up --build -d
```

| Serviço | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

## 📖 Documentação completa

Veja [docs/README.md](docs/README.md) para setup detalhado, endpoints, arquitetura e critérios.

## 🧪 Testes

```bash
# Java (EJB + Backend)
mvn clean verify

# Frontend
cd frontend && npm test
```

> Metodologia TDD Red-Green-Refactor em todas as camadas.

## 🛠️ Desenvolvimento Local

### Backend / EJB

- Requer `Java 17`
- Requer Maven disponível no PATH ou instalado na máquina

### Frontend

- Requer Node.js instalado localmente
- Em PowerShell, prefira `npm.cmd` se houver bloqueio de execution policy

```powershell
cd .\frontend
npm.cmd install --legacy-peer-deps
npm.cmd start
```

## 🔥 Smoke Test

Com a stack em execucao (`docker compose up -d`), rode:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

Parametros opcionais:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1 -ApiBase "http://localhost:8080/api/v1" -FrontendBase "http://localhost:4200"
```
