CREATE TABLE ocorrencia (
    id                      BIGSERIAL PRIMARY KEY,
    protocolo               VARCHAR(30)      NOT NULL UNIQUE,
    usuario_id              BIGINT,
    comunicante_nome        VARCHAR(150)     NOT NULL,
    comunicante_email       VARCHAR(150)     NOT NULL,
    comunicante_telefone    VARCHAR(30),
    comunicante_relacao_ufrpe VARCHAR(50),
    descricao_animal        TEXT             NOT NULL,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    endereco                VARCHAR(500)     NOT NULL,
    status                  VARCHAR(40)      NOT NULL DEFAULT 'EM_ABERTO',
    analista_responsavel_id BIGINT,
    versao                  BIGINT           NOT NULL DEFAULT 0,
    criado_em               TIMESTAMP        NOT NULL,
    atualizado_em           TIMESTAMP        NOT NULL
);

CREATE INDEX idx_ocorrencia_status ON ocorrencia (status);
CREATE INDEX idx_ocorrencia_usuario_id ON ocorrencia (usuario_id);

CREATE TABLE historico_ocorrencia (
    id                      BIGSERIAL PRIMARY KEY,
    ocorrencia_id           BIGINT           NOT NULL REFERENCES ocorrencia(id),
    status_registrado       VARCHAR(40)      NOT NULL,
    usuario_id              BIGINT,
    observacao              TEXT,
    data_hora               TIMESTAMP        NOT NULL
);

CREATE INDEX idx_historico_ocorrencia_id ON historico_ocorrencia (ocorrencia_id);
