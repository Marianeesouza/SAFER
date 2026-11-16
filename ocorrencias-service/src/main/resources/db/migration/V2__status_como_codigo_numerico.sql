-- O status passa a ser guardado como código numérico (ver StatusOcorrencia). Os valores já gravados
-- como texto são convertidos; o índice em ocorrencia.status é recriado automaticamente.

ALTER TABLE ocorrencia ALTER COLUMN status DROP DEFAULT;

ALTER TABLE ocorrencia ALTER COLUMN status TYPE INTEGER USING (
    CASE status
        WHEN 'EM_ABERTO' THEN 1
        WHEN 'EM_ATENDIMENTO' THEN 2
        WHEN 'NECESSITA_ATENDIMENTO_MEDICO' THEN 3
        WHEN 'FINALIZADO_COM_SUCESSO' THEN 4
        WHEN 'FINALIZADO_COM_COMPLICACOES' THEN 5
        WHEN 'CANCELADO' THEN 6
    END);

ALTER TABLE ocorrencia ALTER COLUMN status SET DEFAULT 1;
ALTER TABLE ocorrencia ADD CONSTRAINT ck_ocorrencia_status CHECK (status BETWEEN 1 AND 6);

ALTER TABLE historico_ocorrencia ALTER COLUMN status_registrado TYPE INTEGER USING (
    CASE status_registrado
        WHEN 'EM_ABERTO' THEN 1
        WHEN 'EM_ATENDIMENTO' THEN 2
        WHEN 'NECESSITA_ATENDIMENTO_MEDICO' THEN 3
        WHEN 'FINALIZADO_COM_SUCESSO' THEN 4
        WHEN 'FINALIZADO_COM_COMPLICACOES' THEN 5
        WHEN 'CANCELADO' THEN 6
    END);

ALTER TABLE historico_ocorrencia ADD CONSTRAINT ck_historico_status CHECK (status_registrado BETWEEN 1 AND 6);
