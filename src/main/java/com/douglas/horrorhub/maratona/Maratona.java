package com.douglas.horrorhub.maratona;

import com.douglas.horrorhub.auth.Usuario;
import com.douglas.horrorhub.filme.Filme;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Maratona {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;
    private int tempoDisponivelMin;
    private int nivelMedoMin;
    private int duracaoTotal;
    private LocalDateTime criadaEm;

    @ManyToMany
    @JoinTable(name = "maratona_filme",
            joinColumns = @JoinColumn(name = "maratona_id"),
            inverseJoinColumns = @JoinColumn(name = "filme_id"))
    @OrderColumn(name = "ordem")
    private List<Filme> filmes = new ArrayList<>();

    protected Maratona() {}

    public Maratona(Usuario usuario, int tempoDisponivelMin, int nivelMedoMin, List<Filme> filmes, LocalDateTime criadaEm) {
        this.usuario = usuario;
        this.tempoDisponivelMin = tempoDisponivelMin;
        this.nivelMedoMin = nivelMedoMin;
        this.filmes = new ArrayList<>(filmes);
        this.duracaoTotal = filmes.stream().mapToInt(Filme::getDuracaoMin).sum();
        this.criadaEm = criadaEm;
    }

    public Long getId() { return id; }
    public int getTempoDisponivelMin() { return tempoDisponivelMin; }
    public int getNivelMedoMin() { return nivelMedoMin; }
    public int getDuracaoTotal() { return duracaoTotal; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public List<Filme> getFilmes() { return filmes; }
}
