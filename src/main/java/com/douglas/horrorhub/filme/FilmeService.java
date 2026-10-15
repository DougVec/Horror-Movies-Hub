package com.douglas.horrorhub.filme;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

// ponytail: chamadas HTTP rodam dentro da transação (segura uma conexão do pool durante a ida à TMDB);
// separar em dois beans se o pool apertar sob carga.
@Service
public class FilmeService {

    private final TmdbClient tmdb;
    private final FilmeRepository filmes;
    private final Clock clock;

    public FilmeService(TmdbClient tmdb, FilmeRepository filmes, Clock clock) {
        this.tmdb = tmdb;
        this.filmes = filmes;
        this.clock = clock;
    }

    @Transactional
    public List<Filme> buscar(String q) {
        return salvar(tmdb.buscar(q));
    }

    @Transactional
    public List<Filme> emBreve() {
        return salvar(tmdb.emBreve(LocalDate.now(clock)));
    }

    /** Busca e salvamento não trazem a duração; o primeiro acesso ao detalhe completa pela TMDB. */
    @Transactional
    public Filme detalhe(long id) {
        var filme = filmes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "filme não encontrado"));
        if (filme.getDuracaoMin() == null) {
            filme.atualizarDe(tmdb.detalhe(filme.getTmdbId()));
        }
        return filme;
    }

    public FilmeRepository.Comunidade comunidade(long id) {
        return filmes.comunidade(id);
    }

    public List<FilmeRepository.Ranking> emAlta() {
        return filmes.emAlta(3, PageRequest.of(0, 20));
    }

    /** Upsert por tmdbId, mantendo a ordem que a TMDB devolveu. */
    private List<Filme> salvar(List<TmdbClient.TmdbFilme> resultados) {
        var existentes = filmes.findByTmdbIdIn(resultados.stream().map(TmdbClient.TmdbFilme::id).toList())
                .stream().collect(Collectors.toMap(Filme::getTmdbId, Function.identity()));
        return resultados.stream().map(t -> {
            var filme = existentes.computeIfAbsent(t.id(), Filme::new);
            filme.atualizarDe(t);
            return filmes.save(filme);
        }).toList();
    }
}
