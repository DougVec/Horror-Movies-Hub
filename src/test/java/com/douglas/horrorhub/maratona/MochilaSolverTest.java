package com.douglas.horrorhub.maratona;

import com.douglas.horrorhub.maratona.MochilaSolver.Candidato;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MochilaSolverTest {

    private static List<Long> ids(MochilaSolver.Resultado r) {
        return r.escolhidos().stream().map(Candidato::filmeId).toList();
    }

    @Test
    void listaVaziaGeraMaratonaVazia() {
        var r = MochilaSolver.resolver(List.of(), 300);
        assertTrue(r.escolhidos().isEmpty());
        assertEquals(0, r.duracaoTotal());
        assertEquals(0.0, r.valorTotal());
    }

    @Test
    void tempoZeroNaoEscolheNada() {
        var r = MochilaSolver.resolver(List.of(new Candidato(1, 90, 8)), 0);
        assertTrue(r.escolhidos().isEmpty());
    }

    @Test
    void filmeMaiorQueOTempoFicaDeFora() {
        var r = MochilaSolver.resolver(List.of(new Candidato(1, 181, 10), new Candidato(2, 100, 3)), 180);
        assertEquals(List.of(2L), ids(r));
    }

    @Test
    void filmeQueCabeExatamenteEntra() {
        var r = MochilaSolver.resolver(List.of(new Candidato(1, 180, 9)), 180);
        assertEquals(List.of(1L), ids(r));
        assertEquals(180, r.duracaoTotal());
    }

    @Test
    void venceOGulosoPorValor() {
        // guloso pegaria o 1 (maior valor) e sobraria só 20 min; o ótimo é 2 + 3
        var filmes = List.of(
                new Candidato(1, 160, 9.0),
                new Candidato(2, 90, 7.0),
                new Candidato(3, 90, 7.0));
        var r = MochilaSolver.resolver(filmes, 180);
        assertEquals(List.of(2L, 3L), ids(r));
        assertEquals(14.0, r.valorTotal(), 1e-9);
        assertEquals(180, r.duracaoTotal());
    }

    @Test
    void venceOGulosoPorRazaoValorDuracao() {
        // guloso por valor/min pegaria 1 (0.1/min) e depois não cabe nada; o ótimo é só o 2
        var filmes = List.of(new Candidato(1, 80, 8.0), new Candidato(2, 150, 12.0));
        var r = MochilaSolver.resolver(filmes, 150);
        assertEquals(List.of(2L), ids(r));
    }

    @Test
    void todosCabemTodosEntram() {
        var filmes = List.of(new Candidato(1, 90, 5), new Candidato(2, 100, 6), new Candidato(3, 85, 4));
        var r = MochilaSolver.resolver(filmes, 1000);
        assertEquals(List.of(1L, 2L, 3L), ids(r));
        assertEquals(275, r.duracaoTotal());
    }

    @Test
    void duracaoTotalNuncaPassaDoTempo() {
        var filmes = List.of(new Candidato(1, 95, 7.1), new Candidato(2, 118, 8.4), new Candidato(3, 102, 6.9),
                new Candidato(4, 87, 7.7), new Candidato(5, 134, 9.0), new Candidato(6, 99, 5.2));
        for (int tempo = 0; tempo <= 700; tempo += 7) {
            assertTrue(MochilaSolver.resolver(filmes, tempo).duracaoTotal() <= tempo);
        }
    }

    @Test
    void valorCombinaNotaEMedometro() {
        assertEquals(10.0, MochilaSolver.valor(10, 5), 1e-9);
        assertTrue(MochilaSolver.valor(7, 5) > MochilaSolver.valor(7, 2));
    }

    @Test
    void entradasInvalidasSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class, () -> new Candidato(1, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Candidato(1, 90, -1));
        assertThrows(IllegalArgumentException.class, () -> MochilaSolver.resolver(List.of(), -1));
    }
}
