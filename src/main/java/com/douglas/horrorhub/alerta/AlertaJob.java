package com.douglas.horrorhub.alerta;

import com.douglas.horrorhub.filme.Filme;
import com.douglas.horrorhub.filme.TmdbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Varre os alertas ativos e avisa no Telegram quando o filme estreia ou chega a um streaming no Brasil.
 * Cada filme é consultado uma vez por rodada, mesmo com vários usuários interessados, com pausa entre chamadas.
 * Num 429 da TMDB a rodada para: o que sobrou continua ativo e entra na próxima.
 */
// ponytail: @Scheduled roda em toda instância; com mais de uma réplica, usar ShedLock para não avisar em dobro.
@Component
public class AlertaJob {

    private static final Logger log = LoggerFactory.getLogger(AlertaJob.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AlertaRepository alertas;
    private final TmdbClient tmdb;
    private final TelegramNotifier telegram;
    private final Clock clock;
    private final Duration pausa;

    public AlertaJob(AlertaRepository alertas, TmdbClient tmdb, TelegramNotifier telegram, Clock clock,
                     @Value("${alertas.pausa-tmdb}") Duration pausa) {
        this.alertas = alertas;
        this.tmdb = tmdb;
        this.telegram = telegram;
        this.clock = clock;
        this.pausa = pausa;
    }

    public record Resumo(int verificados, int enviados, boolean interrompidoPorRateLimit) {}

    @Scheduled(initialDelayString = "${alertas.atraso-inicial}", fixedDelayString = "${alertas.intervalo}")
    public void agendado() {
        var r = executar();
        log.info("alertas: {} verificados, {} enviados{}", r.verificados(), r.enviados(),
                r.interrompidoPorRateLimit() ? ", interrompido por rate limit da TMDB" : "");
    }

    public Resumo executar() {
        var estreias = new HashMap<Long, LocalDate>();
        var streamings = new HashMap<Long, List<String>>();
        int verificados = 0, enviados = 0;

        for (var alerta : alertas.ativosComDetalhes()) {
            var chatId = alerta.getUsuario().getChatIdTelegram();
            if (chatId == null) continue; // fica ativo até o usuário cadastrar o Telegram
            var filme = alerta.getFilme();

            Optional<String> mensagem;
            try {
                mensagem = switch (alerta.getTipo()) {
                    case ESTREIA -> mensagemEstreia(filme,
                            consultar(estreias, filme.getTmdbId(), id -> tmdb.detalhe(id).dataLancamento()));
                    case STREAMING -> mensagemStreaming(filme,
                            consultar(streamings, filme.getTmdbId(), tmdb::streamingBr));
                };
            } catch (HttpClientErrorException.TooManyRequests e) {
                return new Resumo(verificados, enviados, true);
            } catch (RestClientException e) {
                log.warn("alerta {}: falha ao consultar a TMDB para o filme {}: {}", alerta.getId(), filme.getTmdbId(), e.getMessage());
                continue;
            }
            verificados++;

            if (mensagem.isPresent() && telegram.enviar(chatId, mensagem.get())) {
                alerta.notificado(LocalDateTime.now(clock));
                alertas.save(alerta);
                enviados++;
            }
        }
        return new Resumo(verificados, enviados, false);
    }

    /** Memoriza por rodada e só pausa quando vai de fato à TMDB. O valor pode ser null (ex.: sem data). */
    private <T> T consultar(HashMap<Long, T> memo, long tmdbId, LongFunction<T> chamada) {
        if (memo.containsKey(tmdbId)) return memo.get(tmdbId);
        pausar();
        T valor = chamada.apply(tmdbId);
        memo.put(tmdbId, valor);
        return valor;
    }

    private Optional<String> mensagemEstreia(Filme filme, LocalDate lancamento) {
        if (lancamento == null || lancamento.isAfter(LocalDate.now(clock))) return Optional.empty();
        return Optional.of("🎬 " + filme.getTitulo() + " estreou em " + lancamento.format(DATA) + "!");
    }

    private Optional<String> mensagemStreaming(Filme filme, List<String> servicos) {
        if (servicos.isEmpty()) return Optional.empty();
        return Optional.of("📺 " + filme.getTitulo() + " chegou ao streaming: " + String.join(", ", servicos) + ".");
    }

    private void pausar() {
        if (pausa.isZero()) return;
        try {
            Thread.sleep(pausa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
