package com.douglas.horrorhub.filme;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Respostas ficam em cache no Redis (TTL em spring.cache.redis.time-to-live) para poupar o rate limit da TMDB. */
@Component
public class TmdbClient {
    static final int GENERO_HORROR = 27;

    private final RestClient http;

    public TmdbClient(RestClient.Builder builder, @Value("${tmdb.base-url}") String baseUrl,
                      @Value("${tmdb.token}") String token) {
        this.http = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    /** /search/movie não filtra por gênero, então o filtro de Horror é feito aqui. */
    @Cacheable(value = "tmdb-busca", key = "#q.trim().toLowerCase()")
    public List<TmdbFilme> buscar(String q) {
        var resp = http.get()
                .uri(u -> u.path("/search/movie").queryParam("query", q.trim()).queryParam("language", "pt-BR").build())
                .retrieve().body(Pagina.class);
        return resp.results().stream().filter(f -> f.generos() != null && f.generos().contains(GENERO_HORROR)).toList();
    }

    @Cacheable("tmdb-em-breve")
    public List<TmdbFilme> emBreve(LocalDate aPartirDe) {
        return http.get()
                .uri(u -> u.path("/discover/movie")
                        .queryParam("with_genres", GENERO_HORROR)
                        .queryParam("primary_release_date.gte", aPartirDe)
                        .queryParam("sort_by", "primary_release_date.asc")
                        .queryParam("region", "BR")
                        .queryParam("language", "pt-BR").build())
                .retrieve().body(Pagina.class).results();
    }

    /** A duração (runtime) só vem no endpoint de detalhes. */
    @Cacheable("tmdb-detalhe")
    public TmdbFilme detalhe(long tmdbId) {
        return http.get().uri("/movie/{id}?language=pt-BR", tmdbId).retrieve().body(TmdbFilme.class);
    }

    /**
     * Serviços de assinatura (flatrate) onde o filme está no Brasil. Sem cache de propósito:
     * só o job de alertas chama, e ele quer o dado fresco.
     */
    public List<String> streamingBr(long tmdbId) {
        var resp = http.get().uri("/movie/{id}/watch/providers", tmdbId).retrieve().body(Provedores.class);
        if (resp == null || resp.results() == null) return List.of();
        var br = resp.results().get("BR");
        if (br == null || br.flatrate() == null) return List.of();
        return br.flatrate().stream().map(Provedor::nome).toList();
    }

    record Pagina(List<TmdbFilme> results) {}

    record Provedores(Map<String, Regiao> results) {}

    record Regiao(List<Provedor> flatrate) {}

    record Provedor(@JsonProperty("provider_name") String nome) {}

    /** Mesmo formato para busca e detalhe: runtime só vem no detalhe, genre_ids só na busca. */
    public record TmdbFilme(long id, String title,
                            @JsonProperty("original_title") String originalTitle,
                            String overview,
                            @JsonProperty("release_date") String releaseDate,
                            @JsonProperty("poster_path") String posterPath,
                            @JsonProperty("vote_average") BigDecimal voteAverage,
                            Integer runtime,
                            @JsonProperty("genre_ids") List<Integer> generos) implements Serializable {

        /** A TMDB manda "" quando não há data. */
        public LocalDate dataLancamento() {
            return releaseDate == null || releaseDate.isBlank() ? null : LocalDate.parse(releaseDate);
        }
    }
}
