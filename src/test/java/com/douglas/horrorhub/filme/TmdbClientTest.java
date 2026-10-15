package com.douglas.horrorhub.filme;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TmdbClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer tmdb = MockRestServiceServer.bindTo(builder).build();
    private final TmdbClient client = new TmdbClient(builder, "https://tmdb.test/3", "tok");

    private static final String BUSCA = """
            {"results":[
              {"id":694,"title":"O Iluminado","original_title":"The Shining","overview":"Jack...",
               "release_date":"1980-05-23","poster_path":"/a.jpg","vote_average":8.2,"genre_ids":[27,53]},
              {"id":1,"title":"Shining Comédia","release_date":"","genre_ids":[35]},
              {"id":2,"title":"Sem gêneros","release_date":""}
            ]}""";

    @Test
    void buscaMandaTokenEFiltraSoTerror() {
        tmdb.expect(requestTo("https://tmdb.test/3/search/movie?query=shining&language=pt-BR"))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess(BUSCA, MediaType.APPLICATION_JSON));

        var r = client.buscar("  shining ");

        assertEquals(List.of(694L), r.stream().map(TmdbClient.TmdbFilme::id).toList());
        var f = r.getFirst();
        assertEquals("The Shining", f.originalTitle());
        assertEquals(LocalDate.of(1980, 5, 23), f.dataLancamento());
        assertEquals(new BigDecimal("8.2"), f.voteAverage());
        tmdb.verify();
    }

    @Test
    void emBreveFiltraPorGeneroEDataNaTmdb() {
        tmdb.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/discover/movie?"),
                        org.hamcrest.Matchers.containsString("with_genres=27"),
                        org.hamcrest.Matchers.containsString("primary_release_date.gte=2026-10-15"))))
                .andRespond(withSuccess("{\"results\":[{\"id\":9,\"title\":\"Novo\",\"release_date\":\"2026-10-31\"}]}",
                        MediaType.APPLICATION_JSON));

        var r = client.emBreve(LocalDate.of(2026, 10, 15));

        assertEquals(LocalDate.of(2026, 10, 31), r.getFirst().dataLancamento());
    }

    @Test
    void detalheTrazDuracao() {
        tmdb.expect(requestTo("https://tmdb.test/3/movie/694?language=pt-BR"))
                .andRespond(withSuccess("{\"id\":694,\"title\":\"O Iluminado\",\"runtime\":146,\"genres\":[{\"id\":27}]}",
                        MediaType.APPLICATION_JSON));

        assertEquals(146, client.detalhe(694).runtime());
    }

    @Test
    void erro404DaTmdbSobeComoNotFound() {
        tmdb.expect(requestTo("https://tmdb.test/3/movie/999?language=pt-BR")).andRespond(withResourceNotFound());
        assertThrows(HttpClientErrorException.NotFound.class, () -> client.detalhe(999));
    }

    @Test
    void filmeVazioNaoApagaDuracaoJaConhecida() {
        var filme = new Filme(694L);
        filme.atualizarDe(new TmdbClient.TmdbFilme(694, "O Iluminado", null, null, "1980-05-23", "/a.jpg", null, 146, null));
        filme.atualizarDe(new TmdbClient.TmdbFilme(694, "O Iluminado", null, null, "", null, null, null, List.of(27)));

        assertEquals(146, filme.getDuracaoMin());
        assertNull(filme.getDataLancamento());
        assertNull(filme.getPosterUrl());
    }

    @Test
    void streamingPegaSoAssinaturaNoBrasil() {
        tmdb.expect(requestTo("https://tmdb.test/3/movie/694/watch/providers"))
                .andRespond(withSuccess("""
                        {"results":{
                          "BR":{"flatrate":[{"provider_name":"Netflix"},{"provider_name":"Max"}],"rent":[{"provider_name":"Apple TV"}]},
                          "US":{"flatrate":[{"provider_name":"Hulu"}]}}}""", MediaType.APPLICATION_JSON));
        assertEquals(List.of("Netflix", "Max"), client.streamingBr(694));
    }

    @Test
    void streamingSemBrasilOuSemAssinaturaEVazio() {
        tmdb.expect(requestTo("https://tmdb.test/3/movie/1/watch/providers"))
                .andRespond(withSuccess("{\"results\":{\"US\":{\"flatrate\":[{\"provider_name\":\"Hulu\"}]}}}", MediaType.APPLICATION_JSON));
        tmdb.expect(requestTo("https://tmdb.test/3/movie/2/watch/providers"))
                .andRespond(withSuccess("{\"results\":{\"BR\":{\"rent\":[{\"provider_name\":\"Apple TV\"}]}}}", MediaType.APPLICATION_JSON));
        assertTrue(client.streamingBr(1).isEmpty());
        assertTrue(client.streamingBr(2).isEmpty());
    }
}
