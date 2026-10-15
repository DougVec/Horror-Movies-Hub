package com.douglas.horrorhub.filme;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Filme {
    private static final String POSTER_BASE = "https://image.tmdb.org/t/p/w500";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tmdbId;
    private String titulo;
    private String tituloOriginal;
    private String sinopse;
    private Integer duracaoMin;
    private LocalDate dataLancamento;
    private String posterUrl;
    private BigDecimal notaTmdb;
    private String subgenero;

    protected Filme() {}

    public Filme(long tmdbId) {
        this.tmdbId = tmdbId;
    }

    /** Copia o que veio da TMDB; campos ausentes (ex.: runtime na busca) não apagam o que já temos. */
    public void atualizarDe(TmdbClient.TmdbFilme t) {
        titulo = t.title();
        tituloOriginal = t.originalTitle();
        sinopse = t.overview();
        dataLancamento = t.dataLancamento();
        posterUrl = t.posterPath() == null ? null : POSTER_BASE + t.posterPath();
        notaTmdb = t.voteAverage();
        if (t.runtime() != null && t.runtime() > 0) duracaoMin = t.runtime();
    }

    public Long getId() { return id; }
    public Long getTmdbId() { return tmdbId; }
    public String getTitulo() { return titulo; }
    public String getTituloOriginal() { return tituloOriginal; }
    public String getSinopse() { return sinopse; }
    public Integer getDuracaoMin() { return duracaoMin; }
    public LocalDate getDataLancamento() { return dataLancamento; }
    public String getPosterUrl() { return posterUrl; }
    public BigDecimal getNotaTmdb() { return notaTmdb; }
    public String getSubgenero() { return subgenero; }
}
