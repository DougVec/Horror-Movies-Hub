package com.douglas.horrorhub.alerta;

import com.douglas.horrorhub.alerta.Alerta.Tipo;
import com.douglas.horrorhub.auth.Usuario;
import com.douglas.horrorhub.filme.Filme;
import com.douglas.horrorhub.filme.TmdbClient;
import com.douglas.horrorhub.filme.TmdbClient.TmdbFilme;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlertaJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);

    private final AlertaRepository repo = mock(AlertaRepository.class);
    private final TmdbClient tmdb = mock(TmdbClient.class);
    private final TelegramNotifier telegram = mock(TelegramNotifier.class);
    private final AlertaJob job = new AlertaJob(repo, tmdb, telegram, CLOCK, Duration.ZERO);

    private static Usuario usuario(String chatId) {
        var u = new Usuario("u", "u@x", "h");
        u.setChatIdTelegram(chatId);
        return u;
    }

    private static Filme filme(long tmdbId, String titulo) {
        var f = new Filme(tmdbId);
        f.atualizarDe(new TmdbFilme(tmdbId, titulo, null, null, null, null, null, null, null));
        return f;
    }

    private static TmdbFilme lancadoEm(long id, String data) {
        return new TmdbFilme(id, "x", null, null, data, null, null, null, null);
    }

    private void ativos(Alerta... a) {
        when(repo.ativosComDetalhes()).thenReturn(List.of(a));
        when(telegram.enviar(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void estreiaFuturaNaoAvisa() {
        var a = new Alerta(usuario("1"), filme(10, "Novo"), Tipo.ESTREIA);
        ativos(a);
        when(tmdb.detalhe(10)).thenReturn(lancadoEm(10, "2026-10-31"));

        assertEquals(0, job.executar().enviados());
        assertTrue(a.isAtivo());
        verifyNoInteractions(telegram);
    }

    @Test
    void estreiaHojeAvisaEDesativa() {
        var a = new Alerta(usuario("1"), filme(10, "Novo"), Tipo.ESTREIA);
        ativos(a);
        when(tmdb.detalhe(10)).thenReturn(lancadoEm(10, "2026-10-15"));

        assertEquals(1, job.executar().enviados());
        verify(telegram).enviar("1", "🎬 Novo estreou em 15/10/2026!");
        assertFalse(a.isAtivo());
        assertEquals(LocalDateTime.of(2026, 10, 15, 12, 0), a.getNotificadoEm());
        verify(repo).save(a);
    }

    @Test
    void estreiaSemDataNaoAvisa() {
        ativos(new Alerta(usuario("1"), filme(10, "Novo"), Tipo.ESTREIA));
        when(tmdb.detalhe(10)).thenReturn(lancadoEm(10, ""));
        assertEquals(0, job.executar().enviados());
    }

    @Test
    void streamingListaOsServicos() {
        var a = new Alerta(usuario("1"), filme(10, "X"), Tipo.STREAMING);
        ativos(a);
        when(tmdb.streamingBr(10)).thenReturn(List.of("Netflix", "Max"));

        job.executar();
        verify(telegram).enviar("1", "📺 X chegou ao streaming: Netflix, Max.");
        assertFalse(a.isAtivo());
    }

    @Test
    void foraDoStreamingContinuaAtivo() {
        var a = new Alerta(usuario("1"), filme(10, "X"), Tipo.STREAMING);
        ativos(a);
        when(tmdb.streamingBr(10)).thenReturn(List.of());

        job.executar();
        assertTrue(a.isAtivo());
        verifyNoInteractions(telegram);
    }

    @Test
    void mesmoFilmeParaVariosUsuariosConsultaATmdbUmaVez() {
        var f = filme(10, "X");
        ativos(new Alerta(usuario("1"), f, Tipo.STREAMING), new Alerta(usuario("2"), f, Tipo.STREAMING),
                new Alerta(usuario("3"), f, Tipo.STREAMING));
        when(tmdb.streamingBr(10)).thenReturn(List.of("Netflix"));

        assertEquals(3, job.executar().enviados());
        verify(tmdb, times(1)).streamingBr(10);
    }

    @Test
    void usuarioSemTelegramNaoGastaChamadaNaTmdb() {
        var a = new Alerta(usuario(null), filme(10, "X"), Tipo.STREAMING);
        ativos(a);

        assertEquals(0, job.executar().verificados());
        verifyNoInteractions(tmdb, telegram);
        assertTrue(a.isAtivo());
    }

    @Test
    void falhaNoTelegramMantemAtivoParaTentarDeNovo() {
        var a = new Alerta(usuario("1"), filme(10, "X"), Tipo.STREAMING);
        when(repo.ativosComDetalhes()).thenReturn(List.of(a));
        when(tmdb.streamingBr(10)).thenReturn(List.of("Netflix"));
        when(telegram.enviar(anyString(), anyString())).thenReturn(false);

        assertEquals(0, job.executar().enviados());
        assertTrue(a.isAtivo());
        verify(repo, never()).save(any());
    }

    @Test
    void rateLimitInterrompeARodada() {
        var primeiro = new Alerta(usuario("1"), filme(10, "A"), Tipo.STREAMING);
        var segundo = new Alerta(usuario("1"), filme(20, "B"), Tipo.STREAMING);
        ativos(primeiro, segundo);
        when(tmdb.streamingBr(10)).thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "", null, null, null));

        var r = job.executar();
        assertTrue(r.interrompidoPorRateLimit());
        verify(tmdb, never()).streamingBr(20);
        assertTrue(segundo.isAtivo());
    }

    @Test
    void falhaEmUmFilmeNaoImpedeOsOutros() {
        var quebrado = new Alerta(usuario("1"), filme(10, "A"), Tipo.STREAMING);
        var ok = new Alerta(usuario("1"), filme(20, "B"), Tipo.STREAMING);
        ativos(quebrado, ok);
        when(tmdb.streamingBr(10)).thenThrow(new ResourceAccessException("timeout"));
        when(tmdb.streamingBr(20)).thenReturn(List.of("Netflix"));

        assertEquals(1, job.executar().enviados());
        assertTrue(quebrado.isAtivo());
        assertFalse(ok.isAtivo());
    }
}
