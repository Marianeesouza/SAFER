# frontend (página de teste)

Interface em HTML, CSS e JavaScript simples para testar e demonstrar visualmente os serviços de
**geocodificação** (com mapa Leaflet) e de **ocorrências** (CDU05, CDU06, CDU07 e CDU08). O formulário
em 3 etapas, o centro do mapa e as cores vêm do protótipo anterior (`Bits-Busters/SAFER`).

A página não conhece o endereço de nenhum serviço: ela chama caminhos relativos (`/api/geocoding/...` e
`/api/ocorrencias/...`) e um Nginx (serviço `frontend` do `compose.yaml`, configurado em `nginx.conf`)
repassa as chamadas ao `geocodificacao-service` e ao `ocorrencias-service`.

| Aba / Ação na tela | Chamada |
|---|---|
| 1. Registrar — Abrir a página (tiles do mapa) | `GET /api/geocoding/map/config` |
| 1. Registrar — Buscar um local pelo nome | `GET /api/geocoding/coordinates?address=` |
| 1. Registrar — Clicar no mapa | `GET /api/geocoding/address?latitude=&longitude=` |
| 1. Registrar — Marcar um ponto | `GET /api/geocoding/campus/validate?latitude=&longitude=` |
| 1. Registrar — Enviar formulário (CDU05) | `POST /api/ocorrencias` |
| 2. Consultar — Buscar por protocolo (CDU06) | `GET /api/ocorrencias/protocolo/{protocolo}` |
| 2. Consultar — Buscar por ID com histórico (CDU06) | `GET /api/ocorrencias/{id}` |
| 2. Consultar — Listar por usuário (CDU06) | `GET /api/ocorrencias/usuario/{usuarioId}` |
| 2. Cancelar — Cancelar ocorrência pendente (CDU08) | `PATCH /api/ocorrencias/{id}/cancelar` |
| 3. Analista — Listar em aberto / filtrar status (CDU06/CDU07) | `GET /api/ocorrencias/em-aberto`, `GET /api/ocorrencias?status=` |
| 3. Analista — Assumir atendimento (CDU07) | `POST /api/ocorrencias/{id}/vincular-analista` |
| 3. Analista — Atualizar status (CDU07) | `PATCH /api/ocorrencias/{id}/status` |

## Como rodar

```bash
docker compose up --build frontend
```

Isso sobe o banco, o `geocodificacao-service` e o Nginx. Abra `http://localhost:5500`.

Estrutura: `public/` (a página, servida pelo Nginx) e `nginx.conf`.
