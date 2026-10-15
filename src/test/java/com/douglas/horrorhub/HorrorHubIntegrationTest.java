package com.douglas.horrorhub;

import com.douglas.horrorhub.alerta.AlertaJob;
import com.douglas.horrorhub.filme.TmdbClient;
import com.douglas.horrorhub.filme.TmdbClient.TmdbFilme;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sobe a aplicação inteira contra Postgres e Redis reais (Testcontainers) e exercita a API por HTTP.
 * Só a TMDB é simulada, por ser externa. Flyway + ddl-auto=validate garantem que entidades e migrations batem.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"alertas.atraso-inicial=PT1H", "telegram.token="})
class HorrorHubIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @MockitoBean
    TmdbClient tmdb;

    @Autowired
    AlertaJob alertaJob;

    @LocalServerPort
    int porta;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta)
                .defaultStatusHandler(s -> true, (req, res) -> {}) // queremos ver todos os status, sem exceção
                .build();
    }

    private ResponseEntity<JsonNode> chamar(HttpMethod metodo, String uri, String token, Object corpo) {
        var req = http.method(metodo).uri(uri);
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        if (corpo != null) req = req.contentType(MediaType.APPLICATION_JSON).body(corpo);
        return req.retrieve().toEntity(JsonNode.class);
    }

    private String novoUsuario() {
        var email = UUID.randomUUID() + "@t.com";
        assertEquals(201, chamar(HttpMethod.POST, "/auth/registro", null,
                Map.of("nome", "Teste", "email", email, "senha", "senha-forte-1")).getStatusCode().value());
        return chamar(HttpMethod.POST, "/auth/login", null, Map.of("email", email, "senha", "senha-forte-1"))
                .getBody().get("token").asText();
    }

    private static TmdbFilme tmdbFilme(long id, String titulo, String data, Integer runtime) {
        return new TmdbFilme(id, titulo, titulo, "sinopse", data, "/p.jpg", new BigDecimal("7.5"), runtime, List.of(27));
    }

    @Test
    void fluxoCompletoDeListaReviewMaratonaEAlerta() {
        var ana = novoUsuario();
        var bia = novoUsuario();

        when(tmdb.buscar(anyString())).thenReturn(List.of(
                tmdbFilme(9001, "Curto", "1990-01-01", null),
                tmdbFilme(9002, "Longo", "1991-01-01", null)));
        when(tmdb.detalhe(9001)).thenReturn(tmdbFilme(9001, "Curto", "1990-01-01", 90));
        when(tmdb.detalhe(9002)).thenReturn(tmdbFilme(9002, "Longo", "1991-01-01", 150));
        when(tmdb.streamingBr(9001)).thenReturn(List.of("Netflix"));

        // catálogo é público e persiste o que veio da TMDB
        var busca = chamar(HttpMethod.GET, "/filmes/busca?q=terror", null, null);
        assertEquals(200, busca.getStatusCode().value());
        long curto = busca.getBody().get(0).get("id").asLong();
        long longo = busca.getBody().get(1).get("id").asLong();

        // lista
        assertEquals(201, chamar(HttpMethod.POST, "/minha-lista", ana, Map.of("filmeId", curto)).getStatusCode().value());
        assertEquals(201, chamar(HttpMethod.POST, "/minha-lista", ana, Map.of("filmeId", longo)).getStatusCode().value());
        assertEquals(409, chamar(HttpMethod.POST, "/minha-lista", ana, Map.of("filmeId", curto)).getStatusCode().value());
        assertEquals(2, chamar(HttpMethod.GET, "/minha-lista?status=QUERO_VER", ana, null).getBody().size());
        assertEquals(0, chamar(HttpMethod.GET, "/minha-lista", bia, null).getBody().size());

        // reviews: uma por usuário, média aparece no detalhe
        assertEquals(201, chamar(HttpMethod.POST, "/filmes/" + curto + "/reviews", bia,
                Map.of("nota", 8, "medometro", 4)).getStatusCode().value());
        assertEquals(409, chamar(HttpMethod.POST, "/filmes/" + curto + "/reviews", bia,
                Map.of("nota", 2, "medometro", 1)).getStatusCode().value());
        var detalhe = chamar(HttpMethod.GET, "/filmes/" + curto, null, null).getBody();
        assertEquals(8.0, detalhe.get("comunidade").get("nota").asDouble());
        assertEquals(90, detalhe.get("filme").get("duracaoMin").asInt());

        // maratona: 120 min só comporta o Curto (90); o Longo (150) não cabe
        var maratona = chamar(HttpMethod.POST, "/maratonas/gerar", ana, Map.of("tempoMin", 120));
        assertEquals(201, maratona.getStatusCode().value());
        assertEquals(List.of("Curto"), maratona.getBody().get("filmes").findValuesAsText("titulo"));
        assertEquals(30, maratona.getBody().get("tempoSobrando").asInt());
        assertEquals(1, chamar(HttpMethod.GET, "/maratonas", ana, null).getBody().size());

        // alerta de streaming dispara uma vez e desativa
        assertEquals(204, chamar(HttpMethod.PUT, "/usuarios/me/telegram", ana, Map.of("chatId", "42")).getStatusCode().value());
        assertEquals(201, chamar(HttpMethod.POST, "/alertas/" + curto + "?tipo=STREAMING", ana, null).getStatusCode().value());
        assertEquals(422, chamar(HttpMethod.POST, "/alertas/" + curto + "?tipo=ESTREIA", ana, null).getStatusCode().value());

        assertEquals(1, alertaJob.executar().enviados());
        assertEquals(0, alertaJob.executar().enviados());
        assertFalse(chamar(HttpMethod.GET, "/alertas", ana, null).getBody().get(0).get("ativo").asBoolean());
    }

    @Test
    void rotasProtegidasExigemToken() {
        assertEquals(401, chamar(HttpMethod.GET, "/minha-lista", null, null).getStatusCode().value());
        assertEquals(401, chamar(HttpMethod.POST, "/maratonas/gerar", null, Map.of("tempoMin", 100)).getStatusCode().value());
        assertEquals(401, chamar(HttpMethod.GET, "/minha-lista", "token-falso", null).getStatusCode().value());
    }

    @Test
    void documentacaoOpenApiEPublicaEDeclaraJwt() {
        var docs = chamar(HttpMethod.GET, "/v3/api-docs", null, null);
        assertEquals(200, docs.getStatusCode().value());
        assertEquals("bearer", docs.getBody().at("/components/securitySchemes/bearer/scheme").asText());
        assertTrue(docs.getBody().get("paths").has("/maratonas/gerar"));
    }
}
