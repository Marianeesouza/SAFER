-- Administrador inicial (fixo): sem ele ninguém consegue promover analistas, pois só administradores
-- alteram cargos e o cadastro sempre cria usuários COMUM.
--
-- E-mail: admin@safer.ufrpe.br | senha de desenvolvimento: SaferAdmin@2026
-- Troque a senha (PATCH /usuarios/me) antes de usar fora do ambiente de desenvolvimento.

INSERT INTO usuario (nome, email, senha_hash, relacao_ufrpe, cargo)
VALUES ('Administrador SAFER', 'admin@safer.ufrpe.br',
        '$2a$10$Nbj9KeWoSwEKH5.eCJUghez7yun5h/SKNK62DtxDwRCvsPFB3iPh2',
        'TECNICO', 'ADMINISTRADOR')
ON CONFLICT (email) DO NOTHING;
