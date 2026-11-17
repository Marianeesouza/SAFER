-- Espécie do animal e imagens anexadas à ocorrência (ver Ocorrencia.especieAnimal e Ocorrencia.imagens).
-- A espécie fica opcional no banco: as ocorrências já registradas não têm esse dado.

ALTER TABLE ocorrencia ADD COLUMN especie_animal VARCHAR(150);

CREATE TABLE ocorrencia_imagens (
    ocorrencia_id BIGINT NOT NULL REFERENCES ocorrencia(id),
    imagem_url    TEXT
);

CREATE INDEX idx_ocorrencia_imagens_ocorrencia_id ON ocorrencia_imagens (ocorrencia_id);
