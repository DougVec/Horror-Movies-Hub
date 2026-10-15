package com.douglas.horrorhub.lista;

import com.douglas.horrorhub.auth.Usuario;
import com.douglas.horrorhub.filme.Filme;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class ItemLista {
    public enum Status { QUERO_VER, ASSISTIDO, ABANDONADO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Filme filme;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Integer nota;
    private LocalDate assistidoEm;

    protected ItemLista() {}

    public ItemLista(Usuario usuario, Filme filme, Status status, LocalDate hoje) {
        this.usuario = usuario;
        this.filme = filme;
        mudarStatus(status, hoje);
    }

    /**
     * Regras: virar ASSISTIDO marca a data (uma vez só); sair de ASSISTIDO limpa data e nota,
     * porque nota só faz sentido para filme visto.
     */
    public void mudarStatus(Status novo, LocalDate hoje) {
        if (novo == Status.ASSISTIDO) {
            if (assistidoEm == null) assistidoEm = hoje;
        } else {
            assistidoEm = null;
            nota = null;
        }
        status = novo;
    }

    public void avaliar(int nota) {
        if (status != Status.ASSISTIDO) {
            throw new IllegalStateException("só dá para dar nota a filme ASSISTIDO");
        }
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota deve estar entre 0 e 10");
        }
        this.nota = nota;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Filme getFilme() { return filme; }
    public Status getStatus() { return status; }
    public Integer getNota() { return nota; }
    public LocalDate getAssistidoEm() { return assistidoEm; }
}
