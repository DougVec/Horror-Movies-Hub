package com.douglas.horrorhub.review;

import com.douglas.horrorhub.auth.Usuario;
import com.douglas.horrorhub.filme.Filme;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Filme filme;
    private String texto;
    private int nota;
    private int medometro;
    private LocalDateTime data;

    protected Review() {}

    public Review(Usuario usuario, Filme filme, String texto, int nota, int medometro, LocalDateTime data) {
        this.usuario = usuario;
        this.filme = filme;
        this.texto = texto;
        this.nota = nota;
        this.medometro = medometro;
        this.data = data;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Filme getFilme() { return filme; }
    public String getTexto() { return texto; }
    public int getNota() { return nota; }
    public int getMedometro() { return medometro; }
    public LocalDateTime getData() { return data; }
}
