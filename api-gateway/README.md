# API Gateway

Porta de entrada única do sistema, construída com **Spring Cloud Gateway** (servidor reativo, WebFlux). O navegador fala somente com o gateway, que encaminha cada requisição para o serviço certo. A visão geral está no [README principal](../README.md).

## Por que um gateway

- **Um único endereço** para o front-end e as APIs (`http://localhost:8000`), sem URLs fixas de cada serviço no front-end e sem CORS.
- **APIs internas:** no compose e no Kubernetes só o gateway é exposto; `library-api`, `students-api` e o front-end ficam acessíveis apenas dentro da rede do sistema.
- **Ponto central para tracing:** cada requisição começa um trace no gateway, que é propagado para as APIs.
- É o componente de **Spring Cloud** do sistema para comunicação distribuída; a comunicação entre as APIs é feita por eventos no RabbitMQ.

## Rotas

| Rota | Destino | Filtro |
|---|---|---|
| `/library-api/**` | `LIBRARY_API_URL` (padrão `http://localhost:8080`) | `StripPrefix=1` |
| `/students-api/**` | `STUDENTS_API_URL` (padrão `http://localhost:8081`) | `StripPrefix=1` |
| `/**` (ordem 100, avaliada por último) | `FRONTEND_URL` (padrão `http://localhost:5173`) | - |

Exemplo: `GET http://localhost:8000/library-api/api/books` chega na `library-api` como `GET /api/books`.

As rotas ficam em [`application.properties`](src/main/resources/application.properties), com o prefixo `spring.cloud.gateway.server.webflux.routes` (nome das propriedades no Spring Cloud 2025, compatível com o Spring Boot 4):

```properties
spring.cloud.gateway.server.webflux.routes[0].id=library-api
spring.cloud.gateway.server.webflux.routes[0].uri=${LIBRARY_API_URL:http://localhost:8080}
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/library-api/**
spring.cloud.gateway.server.webflux.routes[0].filters[0]=StripPrefix=1
```

**Timeouts:** conexão de 2 s e resposta de 15 s. Sem isso, um serviço fora do ar fazia o gateway esperar o timeout padrão de 30 s antes de responder.

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 / Spring Boot 4.1.0 | Base |
| Spring Cloud Gateway (`spring-cloud-starter-gateway-server-webflux`, release train 2025.1.2) | Roteamento |
| Spring Boot Actuator | Health e consulta das rotas |
| Micrometer Tracing + Zipkin | Início e propagação dos traces |
| Loki4j | Envio dos logs para o Loki (perfil `loki`) |

## Actuator

| Endpoint | Retorno |
|---|---|
| `/actuator/health` | Estado do gateway, com `liveness` e `readiness` para as probes |
| `/actuator/gateway/routes` | Rotas ativas e seus destinos |

## Como executar

```bash
./mvnw spring-boot:run     # http://localhost:8000
```

No compose e no Kubernetes os destinos vêm das variáveis `LIBRARY_API_URL`, `STUDENTS_API_URL` e `FRONTEND_URL` (nomes dos serviços na rede interna, como `http://library-api:8080`). A imagem Docker é multi-stage, igual à das APIs, e é publicada em `ghcr.io/gustacassel/api-gateway`.

## Testes

```bash
./mvnw test     # 4 testes
```

O [`GatewayRoutesTest`](src/test/java/com/infnet/apigateway/GatewayRoutesTest.java) sobe o gateway em uma porta aleatória com dois backends falsos (servidor HTTP do próprio JDK) e verifica, com `WebTestClient`:

- roteamento de `/library-api/**` e `/students-api/**`, removendo o prefixo;
- qualquer outro caminho indo para o front-end;
- `/actuator/health` e `/actuator/gateway/routes` respondendo com as três rotas.
