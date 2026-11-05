# SAFER — Sistema de Apoio à Fauna em Emergências da Rural

Microsserviços Spring Boot (Java 21, Gradle multi-módulo). Cada módulo é uma aplicação independente
com banco, porta e imagem próprios; **nenhum módulo depende de outro** — a comunicação é só por
HTTP (descoberta via Eureka) ou RabbitMQ. Arquitetura completa: *Documento de Arquitetura de Serviços*.

| Módulo | Porta | Papel | Banco |
|---|---|---|---|
| `discovery-server` | 8761 | Eureka (Service Discovery) | — |
| `api-gateway` | 8080 | Entrada única, (futuramente) valida JWT, roteia (`/api/usuarios`, `/api/auth`, `/api/ocorrencias`) | — |
| `usuarios-service` | 8081 | Cadastro, login (emitirá JWT), perfil, cargos | `usuarios_db` (5433) |
| `ocorrencias-service` | 8082 | Ocorrências e histórico; publica eventos no RabbitMQ | `ocorrencias_db` (5434) |
| `geocodificacao-service` | 8083 | Endereço ⇄ coordenadas (Nominatim) com cache | `geocodificacao_db` (5436) |
| `notificacoes-service` | 8084 | Consome eventos e envia e-mail (SMTP) | `notificacoes_db` (5435) |

Dentro de cada serviço: `controller` → `service` → `repository` (+ `model`, `dto`, `config`);
integrações externas ficam atrás de `adapter` (geocodificação, e-mail) ou `client` (outros serviços).

`docs/modelagem-astah/` guarda os exports do Astah (referência, não compilam).

## Rodando

```bash
./gradlew build                     # compila e roda os testes de todos os módulos
docker compose up --build           # sobe infraestrutura + serviços
```

Só a infraestrutura, para rodar os serviços pela IDE (`bootRun` por módulo):

```bash
docker compose up usuarios-db ocorrencias-db notificacoes-db geocodificacao-db rabbitmq mailhog
```

Painéis: Eureka `localhost:8761`, RabbitMQ `localhost:15672` (guest/guest), MailHog `localhost:8025`.

## Estado

Só a estrutura: módulos, dependências, configuração, `compose.yaml` e pacotes por camada
(cada `package-info.java` descreve o que entra ali). Nenhuma regra de negócio implementada ainda.
Segurança/JWT ainda não foi adicionada (as dependências entram na parte de autenticação).
