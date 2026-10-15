package com.douglas.horrorhub.maratona;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mochila 0/1: escolhe os filmes que maximizam o valor total sem passar do tempo disponível.
 * Capacidade = tempo (min), peso = duração do filme, valor = nota da comunidade combinada com o medômetro.
 * O(n * tempo) em tempo e memória — com tempo limitado a um dia (1440 min) isso é trivial.
 */
public final class MochilaSolver {

    public record Candidato(long filmeId, int duracaoMin, double valor) {
        public Candidato {
            if (duracaoMin <= 0) throw new IllegalArgumentException("duração deve ser positiva: " + filmeId);
            if (valor < 0) throw new IllegalArgumentException("valor não pode ser negativo: " + filmeId);
        }
    }

    public record Resultado(List<Candidato> escolhidos, int duracaoTotal, double valorTotal) {}

    /** nota 0–10 e medômetro 1–5 viram um valor único; peso maior para o medo, afinal é uma maratona de terror. */
    public static double valor(double notaMedia, double medometroMedio) {
        return notaMedia * 0.6 + medometroMedio * 2 * 0.4;
    }

    public static Resultado resolver(List<Candidato> candidatos, int tempoMin) {
        if (tempoMin < 0) throw new IllegalArgumentException("tempo não pode ser negativo");
        int n = candidatos.size();
        // melhor[i][t] = maior valor usando os i primeiros filmes com até t minutos
        double[][] melhor = new double[n + 1][tempoMin + 1];
        for (int i = 1; i <= n; i++) {
            var c = candidatos.get(i - 1);
            for (int t = 0; t <= tempoMin; t++) {
                melhor[i][t] = melhor[i - 1][t];
                if (c.duracaoMin() <= t) {
                    melhor[i][t] = Math.max(melhor[i][t], melhor[i - 1][t - c.duracaoMin()] + c.valor());
                }
            }
        }
        // reconstrói a escolha voltando pela tabela
        var escolhidos = new ArrayList<Candidato>();
        int t = tempoMin, duracao = 0;
        for (int i = n; i > 0; i--) {
            if (melhor[i][t] != melhor[i - 1][t]) {
                var c = candidatos.get(i - 1);
                escolhidos.add(c);
                t -= c.duracaoMin();
                duracao += c.duracaoMin();
            }
        }
        Collections.reverse(escolhidos);
        return new Resultado(List.copyOf(escolhidos), duracao, melhor[n][tempoMin]);
    }

    private MochilaSolver() {}
}
