# geocodificacao-service

Isola a comunicação com o provedor externo de mapas (Nominatim): converte endereço em coordenadas e
coordenadas em endereço, com cache próprio no banco. Também informa as configurações do mapa e se um
ponto está dentro do campus. É chamado pelo Serviço de Ocorrências; não há rota no gateway.

- Porta: `8083` · Eureka: `geocodificacao-service` · Banco: `geocodificacao_db` (porta local `5436`)
- Swagger: `http://localhost:8083/swagger-ui.html`

## Endpoints

| Método e caminho | Descrição |
|---|---|
| `GET /geocoding/coordinates?address=` | Coordenadas de um endereço (200, 400, 404, 503) |
| `GET /geocoding/address?latitude=&longitude=` | Endereço de um par de coordenadas (200, 400, 404, 503) |
| `GET /geocoding/map/config` | `urlTemplate`, `attribution` e `maxZoom` para o front-end |
| `GET /geocoding/campus/validate?latitude=&longitude=` | `{"insideCampus": true/false}` |

Erros vêm em `ProblemDetail`. O 503 indica que o Nominatim está fora do ar **e** a consulta não está no cache;
quem chama (Ocorrências) deve seguir sem o endereço por extenso.

## Como funciona

`controller` → `service` (cache primeiro, provedor depois) → `adapter` (Nominatim) e `repository` (cache).
O cache não expira. A chave de endereço é o texto sem espaços nas pontas e em minúsculas; a de coordenadas
é arredondada a 5 casas decimais (~1 m).

## Configuração (`application.yml`)

| Chave | Variável de ambiente | Padrão |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5436/geocodificacao_db` |
| `spring.datasource.username/password` | `DB_USER` / `DB_PASSWORD` | `safer` / `safer` |
| `eureka.client.service-url.defaultZone` | `EUREKA_URL` | `http://localhost:8761/eureka` |
| `safer.nominatim.url` | `NOMINATIM_URL` | `https://nominatim.openstreetmap.org` |
| `safer.map.*` | — | tiles do OpenStreetMap, zoom máximo 19 |
| `safer.campus.*` | — | centro do campus e raio de 1200 m |

## Rodando

```bash
docker compose up geocodificacao-db            # só o banco, na porta 5436
./gradlew :geocodificacao-service:bootRun      # o serviço, pela linha de comando
./gradlew :geocodificacao-service:test         # testes
```
