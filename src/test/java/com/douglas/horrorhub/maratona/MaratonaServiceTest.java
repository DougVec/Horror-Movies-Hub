package com.douglas.horrorhub.maratona;

import com.douglas.horrorhub.filme.Filme;
import com.douglas.horrorhub.filme.TmdbClient.TmdbFilme;
import com.douglas.horrorhub.maratona.MaratonaRepository.Estatistica;
import com.douglas.horrorhub.maratona.MochilaSolver.Candidato;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaratonaServiceTest {

    record Stat(Filme getFilme, Double getNota, Double getMedometro) implements Estatistica {}

    private static Filme filme(long id, Integer duracao, String notaTmdb) {
        var f = new Filme(id);
        f.atualizarDe(new TmdbFilme(id, "f" + id, null, null, null, null,
                notaTmdb == null ? null : new BigDecimal(notaTmdb), duracao, null));
        ReflectionTestUtils.setField(f, "id", id);
        return f;
    }

    private static List<Long> ids(List<Candidato> c) {
        return c.stream().map(Candidato::filmeId).toList();
    }

    @Test
    void filmeSemDuracaoFicaDeFora() {
        var c = MaratonaService.candidatos(List.of(new Stat(filme(1, null, "7"), 8.0, 4.0)), 1);
        assertTrue(c.isEmpty());
    }

    @Test
    void filtraPeloMedoMinimo() {
        var stats = List.<Estatistica>of(
                new Stat(filme(1, 90, null), 8.0, 2.0),
                new Stat(filme(2, 90, null), 8.0, 3.0),
                new Stat(filme(3, 90, null), 8.0, 4.5));
        assertEquals(List.of(2L, 3L), ids(MaratonaService.candidatos(stats, 3)));
    }

    @Test
    void semReviewsSoEntraQuandoAceitaQualquerMedo() {
        var stats = List.<Estatistica>of(new Stat(filme(1, 90, "7.5"), null, null));
        assertEquals(List.of(1L), ids(MaratonaService.candidatos(stats, 1)));
        assertTrue(MaratonaService.candidatos(stats, 2).isEmpty());
    }

    @Test
    void notaDaComunidadeTemPrioridadeSobreATmdb() {
        var c = MaratonaService.candidatos(List.of(new Stat(filme(1, 90, "2.0"), 9.0, 4.0)), 1);
        assertEquals(MochilaSolver.valor(9.0, 4.0), c.getFirst().valor(), 1e-9);
    }

    @Test
    void semNotaNenhumaUsaValoresNeutros() {
        var c = MaratonaService.candidatos(List.of(new Stat(filme(1, 90, null), null, null)), 1);
        assertEquals(MochilaSolver.valor(MaratonaService.NOTA_PADRAO, MaratonaService.MEDO_PADRAO), c.getFirst().valor(), 1e-9);
    }

    @Test
    void notaTmdbEntraQuandoNaoHaReviews() {
        var c = MaratonaService.candidatos(List.of(new Stat(filme(1, 90, "8.0"), null, null)), 1);
        assertEquals(MochilaSolver.valor(8.0, MaratonaService.MEDO_PADRAO), c.getFirst().valor(), 1e-9);
    }

    @Test
    void duracaoVaiParaOCandidato() {
        var c = MaratonaService.candidatos(List.of(new Stat(filme(1, 146, null), 9.0, 5.0)), 5);
        assertEquals(146, c.getFirst().duracaoMin());
    }
}
