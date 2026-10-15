package com.douglas.horrorhub.auth;

import jakarta.persistence.*;

@Entity
public class Usuario {
    public enum Papel { USER, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    private String senha;
    @Enumerated(EnumType.STRING)
    private Papel papel = Papel.USER;
    private String chatIdTelegram;

    protected Usuario() {}

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senha = senhaHash;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public Papel getPapel() { return papel; }
    public String getChatIdTelegram() { return chatIdTelegram; }

    public void setChatIdTelegram(String chatIdTelegram) { this.chatIdTelegram = chatIdTelegram; }
}
