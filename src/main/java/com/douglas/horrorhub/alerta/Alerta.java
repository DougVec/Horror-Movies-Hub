package com.douglas.horrorhub.alerta;

import com.douglas.horrorhub.auth.Usuario;
import com.douglas.horrorhub.filme.Filme;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Alerta {
    public enum Tipo { ESTREIA, STREAMING }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Filme filme;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    private boolean ativo = true;
    private LocalDateTime notificadoEm;

    protected Alerta() {}

    public Alerta(Usuario usuario, Filme filme, Tipo tipo) {
        this.usuario = usuario;
        this.filme = filme;
        this.tipo = tipo;
    }

    /** Alerta dispara uma vez só. */
    public void notificado(LocalDateTime quando) {
        ativo = false;
        notificadoEm = quando;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Filme getFilme() { return filme; }
    public Tipo getTipo() { return tipo; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getNotificadoEm() { return notificadoEm; }
}
