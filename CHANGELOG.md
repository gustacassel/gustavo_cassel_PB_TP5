# Changelog

Evolução do sistema ao longo do Projeto de Bloco. Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [1.0.0] - 2026-09-27 - TP5: operação

### Adicionado
- **API Gateway** com Spring Cloud Gateway: porta de entrada única, roteando `/library-api/**`, `/students-api/**` e o front-end, com timeouts de conexão e rotas expostas no Actuator.
- **PostgreSQL** em contêiner, um banco por serviço, configurado por variáveis de ambiente (H2 continua nos testes).
- **Spring Boot Actuator** com health, liveness e readiness nas APIs e no gateway.
- **Dockerfiles multi-stage** para `library-api`, `students-api`, `api-gateway` e `library-frontend` (nginx).
- **docker-compose** com o sistema completo: 10 contêineres com healthcheck e ordem de subida.
- **Rastreamento distribuído** com Micrometer Tracing e Zipkin, atravessando gateway, APIs e RabbitMQ.
- **Logs centralizados** com Loki4j, Loki e Grafana, com `traceId` em cada linha, link do log para o trace e dashboard pronto.
- **Kubernetes**: manifests com StatefulSets para PostgreSQL e RabbitMQ, probes do Actuator, initContainers, `library-api` com 2 réplicas, gateway exposto por NodePort, configuração do kind e script de deploy.
- **Pipeline de CI/CD** no GitHub Actions: build e testes, testes de ponta a ponta no compose, publicação das imagens no GHCR, deploy em cluster Kubernetes efêmero e release por tag.
- **Testes do front-end** com Vitest e Testing Library.
- **Smoke test de ponta a ponta** (`scripts/smoke-test.mjs`), executado contra o compose e contra o Kubernetes.

### Alterado
- Front-end passou a chamar as APIs por caminhos relativos, através do gateway.
- `spring.application.name` da biblioteca padronizado para `library-api`.

## [0.4.0] - 2026-09-27 - TP4: arquitetura orientada a eventos

### Adicionado
- RabbitMQ com exchanges `students.events` e `library.events` (topic).
- `students-api` publica `student.created`, `student.updated` e `student.deleted`; a `library-api` mantém uma cópia local dos alunos.
- `library-api` publica `loan.created`, `loan.returned` e `loan.deleted`; a `students-api` mantém os empréstimos ativos por aluno.
- Publicação somente após o commit, consumo idempotente e tolerante a eventos fora de ordem.
- Bloqueio de exclusão de aluno e de livro com empréstimo ativo.
- Front-end repaginado: tela de empréstimos, painel de integração por eventos, status dos serviços e tema escuro.

### Removido
- Spring Cloud OpenFeign e Resilience4j da `library-api` (comunicação síncrona substituída por eventos).

## [0.3.0] - 2026-08-12 - TP3: microsserviço

### Adicionado
- Microsserviço `students-api` com alunos e cursos, banco próprio e histórico de mudanças.
- Integração da `library-api` com o microsserviço via Spring Cloud OpenFeign e circuit breaker (Resilience4j).
- Telas de estudantes e cursos no front-end.

## [0.2.0] - 2026-07-12 - TP2: persistência

### Adicionado
- Persistência com JPA e Spring Data: livros, alunos e empréstimos com relacionamentos.
- Histórico de mudanças com JPA Auditing e tabela `audit_log` com diff por campo.
- Testes da camada de persistência.

## [0.1.0] - 2026-06-03 - TP1: monólito

### Adicionado
- Monólito Spring Boot em camadas (controller, service, repository) com API REST.
- Front-end React consumindo a API.
