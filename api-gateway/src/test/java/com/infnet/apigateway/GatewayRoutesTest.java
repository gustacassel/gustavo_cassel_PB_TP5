package com.infnet.apigateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutesTest {

    private static HttpServer libraryApi;
    private static HttpServer studentsApi;

    @Autowired
    private WebTestClient client;

    // backend falso que devolve o nome do servico e o caminho que recebeu
    private static HttpServer fakeBackend(String name) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var body = "{\"service\":\"%s\",\"path\":\"%s\"}".formatted(name, exchange.getRequestURI().getPath())
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    @BeforeAll
    static void startBackends() throws IOException {
        libraryApi = fakeBackend("library-api");
        studentsApi = fakeBackend("students-api");
    }

    @AfterAll
    static void stopBackends() {
        libraryApi.stop(0);
        studentsApi.stop(0);
    }

    @DynamicPropertySource
    static void backendUrls(DynamicPropertyRegistry registry) {
        registry.add("LIBRARY_API_URL", () -> "http://localhost:" + libraryApi.getAddress().getPort());
        registry.add("STUDENTS_API_URL", () -> "http://localhost:" + studentsApi.getAddress().getPort());
        registry.add("FRONTEND_URL", () -> "http://localhost:" + libraryApi.getAddress().getPort());
    }

    @Test
    void shouldRouteLibraryRequestsRemovingThePrefix() {
        client.get().uri("/library-api/api/books").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("library-api")
                .jsonPath("$.path").isEqualTo("/api/books");
    }

    @Test
    void shouldRouteStudentsRequestsRemovingThePrefix() {
        client.get().uri("/students-api/api/students/1").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("students-api")
                .jsonPath("$.path").isEqualTo("/api/students/1");
    }

    @Test
    void shouldSendAnyOtherPathToTheFrontend() {
        client.get().uri("/loans").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/loans");
    }

    @Test
    void shouldExposeHealthAndTheConfiguredRoutes() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");

        client.get().uri("/actuator/gateway/routes").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.route_id == 'library-api')]").exists()
                .jsonPath("$[?(@.route_id == 'students-api')]").exists()
                .jsonPath("$[?(@.route_id == 'frontend')]").exists();
    }
}
