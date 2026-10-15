package com.douglas.horrorhub.maratona;

import com.douglas.horrorhub.auth.UsuarioRepository;
import com.douglas.horrorhub.filme.Filme;
import com.douglas.horrorhub.filme.TmdbClient;
import com.douglas.horrorhub.maratona.MaratonaRepository.Estatistica;
import com.douglas.horrorhub.maratona.MochilaSolver.Candidato;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MaratonaService {

    /** Sem reviews da comunidade: usa a nota da TMDB; sem nada, um valor neutro. */
    static final double NOTA_PADRAO = 5.0;
    static final double MEDO_PADRAO = 3.0;

    private static final Logger log = LoggerFactory.getLogger(MaratonaService.class);

    private final MaratonaRepository maratonas;
    private final UsuarioRepository usuarios;
    private final TmdbClient tmdb;
    private final Clock clock;

    public MaratonaService(MaratonaRepository maratonas, UsuarioRepository usuarios, TmdbClient tmdb, Clock clock) {
        this.maratonas = maratonas;
        this.usuarios = usuarios;
        this.tmdb = tmdb;
        this.clock = clock;
    }

    @Transactional
    public Maratona gerar(long usuarioId, int tempoMin, int nivelMedoMin, boolean apenasQueroVer) {
        // ponytail: no hub inteiro só entram filmes com duração já conhecida, para não disparar N chamadas à TMDB
        var ids = apenasQueroVer ? maratonas.idsQueroVer(usuarioId) : maratonas.idsNaoAssistidos(usuarioId);
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    apenasQueroVer ? "sua lista QUERO_VER está vazia" : "nenhum filme disponível no hub");
        }

        var estatisticas = maratonas.estatisticas(ids);
        if (apenasQueroVer) completarDuracoes(estatisticas);
        var porId = estatisticas.stream().collect(Collectors.toMap(e -> e.getFilme().getId(), Function.identity()));
        var resultado = MochilaSolver.resolver(candidatos(estatisticas, nivelMedoMin), tempoMin);
        if (resultado.escolhidos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "nenhum filme cabe em " + tempoMin + " min com medômetro >= " + nivelMedoMin);
        }

        var filmes = ordemCrescenteDeMedo(resultado.escolhidos(), porId);
        return maratonas.save(new Maratona(usuarios.getReferenceById(usuarioId), tempoMin, nivelMedoMin,
                filmes, LocalDateTime.now(clock)));
    }

    /**
     * Filmes salvos pela busca ainda não têm duração; a lista do usuário é pequena, então completa na TMDB.
     * Falha num filme não derruba a maratona: ele só fica de fora.
     */
    private void completarDuracoes(List<Estatistica> estatisticas) {
        for (var e : estatisticas) {
            var filme = e.getFilme();
            if (filme.getDuracaoMin() != null) continue;
            try {
                filme.atualizarDe(tmdb.detalhe(filme.getTmdbId()));
            } catch (RestClientException ex) {
                log.warn("sem duração para o filme {} (tmdb {}): {}", filme.getId(), filme.getTmdbId(), ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Maratona> historico(long usuarioId) {
        return maratonas.findByUsuarioIdOrderByCriadaEmDesc(usuarioId, PageRequest.of(0, 20));
    }

    /**
     * Filtra por duração conhecida e medo mínimo e pontua cada filme.
     * Filme sem nenhuma review tem medo desconhecido: só entra quando o usuário aceita qualquer nível (1).
     */
    static List<Candidato> candidatos(List<Estatistica> estatisticas, int nivelMedoMin) {
        return estatisticas.stream()
                .filter(e -> e.getFilme().getDuracaoMin() != null)
                .filter(e -> e.getMedometro() == null ? nivelMedoMin <= 1 : e.getMedometro() >= nivelMedoMin)
                .map(e -> new Candidato(e.getFilme().getId(), e.getFilme().getDuracaoMin(),
                        MochilaSolver.valor(nota(e), e.getMedometro() == null ? MEDO_PADRAO : e.getMedometro())))
                .toList();
    }

    private static double nota(Estatistica e) {
        if (e.getNota() != null) return e.getNota();
        var tmdb = e.getFilme().getNotaTmdb();
        return tmdb != null ? tmdb.doubleValue() : NOTA_PADRAO;
    }

    /** O mais leve primeiro e o mais assustador por último: a maratona vai esquentando. */
    private static List<Filme> ordemCrescenteDeMedo(List<Candidato> escolhidos, Map<Long, Estatistica> porId) {
        return escolhidos.stream()
                .map(c -> porId.get(c.filmeId()))
                .sorted(Comparator.comparingDouble((Estatistica e) -> e.getMedometro() == null ? MEDO_PADRAO : e.getMedometro()))
                .map(Estatistica::getFilme)
                .toList();
    }
}
