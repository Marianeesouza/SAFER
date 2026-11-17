CREATE TABLE notificacao (
    id                 BIGSERIAL PRIMARY KEY,
    chave_idempotencia VARCHAR(200) NOT NULL UNIQUE,
    destinatario       VARCHAR(150) NOT NULL,
    assunto            VARCHAR(255) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    tentativas         INTEGER      NOT NULL,
    criado_em          TIMESTAMP    NOT NULL,
    atualizado_em      TIMESTAMP    NOT NULL
);
