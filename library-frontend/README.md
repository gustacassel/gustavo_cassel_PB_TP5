# Library Frontend - Biblioteca Aurora

Interface web do sistema de biblioteca do Projeto de Bloco (*Engenharia de Softwares Escaláveis*). Consome as duas APIs do sistema, a [`library-api`](../library-api) (livros, empréstimos e cópia local dos alunos) e a [`students-api`](../students-api) (estudantes e cursos), sempre através do [`api-gateway`](../api-gateway). A arquitetura geral está no [README da raiz](../README.md).

## Tecnologias

| Tecnologia | Uso |
|---|---|
| React 19 + TypeScript | Componentes e tipagem dos contratos das APIs |
| Vite | Servidor de desenvolvimento e build |
| React Router | Navegação entre as páginas |
| Bootstrap 5.3 + React Bootstrap | Layout, componentes e tema claro/escuro (`data-bs-theme`) |
| Bootstrap Icons | Ícones |
| SweetAlert2 | Formulários e confirmações em modal |
| Vitest + Testing Library | Testes |
| nginx | Servidor da imagem Docker |

## Telas

| Rota | Tela | API |
|---|---|---|
| `/` | **Painel**: métricas dos dois serviços, empréstimos recentes e o card *Integração por eventos* (alunos na cópia local, último evento recebido, estado da sincronização e os fluxos `students.events` / `library.events`) | as duas |
| `/loans` | **Empréstimos**: listar, filtrar (ativos, atrasados, devolvidos), registrar, devolver e excluir | library-api |
| `/books` | **Livros**: CRUD e indicação de livro emprestado | library-api |
| `/students` | **Estudantes**: CRUD, situação e quantidade de empréstimos ativos | students-api |
| `/courses` | **Cursos**: CRUD e alunos ativos por curso | students-api |

Destaques ligados à arquitetura orientada a eventos (TP4):

- O formulário de novo empréstimo busca os alunos na **cópia local da library-api** (`/api/integration/students`), não na `students-api`. Por isso continua funcionando com a `students-api` fora do ar.
- A barra lateral mostra o **status de cada serviço em tempo real** (verificado a cada 10 s). Serve para demonstrar os cenários de falha: com a `students-api` offline, o painel indica que está usando a cópia local e os empréstimos seguem normalmente.
- A coluna **Empréstimos ativos** da tela de estudantes vem da `students-api`, que conta os empréstimos a partir dos eventos publicados pela biblioteca.
- Erros de regra de negócio (`409`) aparecem com a mensagem do back-end, por exemplo ao tentar excluir um aluno com livro emprestado.

## Estrutura

```
src/
├── App.tsx                  rotas, todas dentro do AppLayout
├── components/
│   ├── AppLayout.tsx        barra lateral, status dos serviços e botão de tema
│   ├── PageHeader.tsx       cabeçalho padrão das páginas
│   └── StatCard.tsx         card de métrica
├── hooks/
│   ├── useServiceStatus.ts  verifica library-api e students-api periodicamente
│   └── useTheme.ts          tema claro/escuro salvo no navegador
├── pages/                   home, loans, books, students, courses
├── services/                clientes HTTP de cada recurso
├── types/                   contratos das APIs
├── utils/format.ts          datas e escape de HTML
└── styles/pages.css         estilos compartilhados das páginas
```

## Configuração

As chamadas usam **caminhos relativos**, e o gateway encaminha cada prefixo para o serviço certo ([`src/services/api-client.ts`](src/services/api-client.ts)):

```ts
export const LIBRARY_API_URL = "/library-api";
export const STUDENTS_API_URL = "/students-api";
```

- **No compose e no Kubernetes** o front-end é servido pelo próprio gateway (`http://localhost:8000`), então os caminhos relativos já chegam nele.
- **Em desenvolvimento** o Vite repassa `/library-api` e `/students-api` para o gateway (`GATEWAY_URL`, padrão `http://localhost:8000`), configurado em [`vite.config.ts`](vite.config.ts).

## Imagem Docker

[`Dockerfile`](Dockerfile) multi-stage: build com `node:24-alpine` (`npm ci` e `npm run build`) e runtime com `nginx:1.29-alpine`. O [`nginx.conf`](nginx.conf) devolve o `index.html` para as rotas do React Router (links diretos como `/loans` funcionam) e expõe `/healthz` para o healthcheck do compose e as probes do Kubernetes. A imagem é publicada em `ghcr.io/gustacassel/library-frontend`.

## Como executar

Pelo sistema completo (na raiz do repositório): `docker compose up -d --build` e acesse `http://localhost:8000`.

Em desenvolvimento, com o gateway e as APIs no ar:

```bash
npm install
npm run dev      # http://localhost:5173
```

| Script | Descrição |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com recarga automática |
| `npm test` | Testes (Vitest) |
| `npm run build` | Checagem de tipos (`tsc -b`) e build de produção em `dist/` |
| `npm run lint` | ESLint |
| `npm run preview` | Serve o build de produção localmente |

## Testes

16 testes com Vitest e Testing Library (ambiente jsdom), executados no CI a cada push:

| Arquivo | O que cobre |
|---|---|
| `utils/format.test.ts` | Datas (sem deslocamento de fuso), conversão para input, escape de HTML, mensagem de erro |
| `services/api-client.test.ts` | Prefixos do gateway, corpo JSON, resposta 204, mensagem de erro do back-end |
| `hooks/useServiceStatus.test.ts` | Serviços online, offline e com erro do gateway |
| `components/components.test.tsx` | `StatCard` (valor, dica, carregamento acessível) e `PageHeader` |
