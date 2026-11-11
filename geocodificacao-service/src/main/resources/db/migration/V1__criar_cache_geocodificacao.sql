CREATE TABLE cache_geocodificacao (
    id          BIGSERIAL PRIMARY KEY,
    chave       VARCHAR(400)     NOT NULL UNIQUE,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    endereco    VARCHAR(500)     NOT NULL,
    criado_em   TIMESTAMP        NOT NULL
);
