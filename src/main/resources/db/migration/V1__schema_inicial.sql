CREATE TABLE usuario (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    senha            VARCHAR(100) NOT NULL,
    papel            VARCHAR(10)  NOT NULL DEFAULT 'USER' CHECK (papel IN ('USER', 'ADMIN')),
    chat_id_telegram VARCHAR(50)
);

CREATE TABLE filme (
    id               BIGSERIAL PRIMARY KEY,
    tmdb_id          BIGINT       NOT NULL UNIQUE,
    titulo           VARCHAR(255) NOT NULL,
    titulo_original  VARCHAR(255),
    sinopse          TEXT,
    duracao_min      INT CHECK (duracao_min > 0),
    data_lancamento  DATE,
    poster_url       VARCHAR(500),
    nota_tmdb        NUMERIC(3, 1),
    subgenero        VARCHAR(50)
);

CREATE TABLE item_lista (
    id           BIGSERIAL PRIMARY KEY,
    usuario_id   BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    filme_id     BIGINT      NOT NULL REFERENCES filme (id),
    status       VARCHAR(15) NOT NULL CHECK (status IN ('QUERO_VER', 'ASSISTIDO', 'ABANDONADO')),
    nota         INT CHECK (nota BETWEEN 0 AND 10),
    assistido_em DATE,
    UNIQUE (usuario_id, filme_id)
);

CREATE TABLE review (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT    NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    filme_id   BIGINT    NOT NULL REFERENCES filme (id),
    texto      TEXT,
    nota       INT       NOT NULL CHECK (nota BETWEEN 0 AND 10),
    medometro  INT       NOT NULL CHECK (medometro BETWEEN 1 AND 5),
    data       TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, filme_id)
);

CREATE TABLE alerta (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    filme_id      BIGINT      NOT NULL REFERENCES filme (id),
    tipo          VARCHAR(10) NOT NULL CHECK (tipo IN ('ESTREIA', 'STREAMING')),
    ativo         BOOLEAN     NOT NULL DEFAULT TRUE,
    notificado_em TIMESTAMP,
    UNIQUE (usuario_id, filme_id, tipo)
);

CREATE TABLE maratona (
    id                   BIGSERIAL PRIMARY KEY,
    usuario_id           BIGINT    NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tempo_disponivel_min INT       NOT NULL CHECK (tempo_disponivel_min > 0),
    nivel_medo_min       INT       NOT NULL CHECK (nivel_medo_min BETWEEN 1 AND 5),
    duracao_total        INT       NOT NULL,
    criada_em            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE maratona_filme (
    maratona_id BIGINT NOT NULL REFERENCES maratona (id) ON DELETE CASCADE,
    filme_id    BIGINT NOT NULL REFERENCES filme (id),
    ordem       INT    NOT NULL,
    PRIMARY KEY (maratona_id, filme_id)
);

CREATE INDEX idx_alerta_ativo ON alerta (ativo) WHERE ativo;
