# SAFER — Sistema de Apoio à Fauna em Emergências da Rural

Sistema para registrar e acompanhar ocorrências com animais em situação de risco no campus da UFRPE. Qualquer
pessoa registra uma ocorrência (mapa do campus, espécie, descrição e fotos) e acompanha o andamento pelo
protocolo. Analistas e administradores triam e atualizam as ocorrências, e os envolvidos são avisados por e-mail.

Microsserviços Spring Boot (Java 21, Gradle multi-módulo) com Eureka, gateway, RabbitMQ e um frontend estático
em Nginx. Cada serviço tem banco e imagem próprios e **nenhum módulo depende do código de outro**: a comunicação
é só por HTTP (via Eureka) ou RabbitMQ.

## Arquitetura

```
Navegador ─► Nginx :5500 ─► api-gateway :8080 ─┬─► usuarios-service :8081
                                               ├─► ocorrencias-service :8082 ──RabbitMQ──► notificacoes-service :8084 ─► SMTP
                                               └─► geocodificacao-service :8083
Todos se registram no discovery-server (Eureka) :8761. O gateway valida o token JWT de cada chamada (veja Segurança). O ocorrencias consulta o usuarios (permissões, destinatários
dos avisos) e a geocodificação (ponto dentro do campus) por HTTP, pelo Eureka.
```

Só o Nginx (5500), o RabbitMQ (5672/15672), o MailHog (1025/8025) e os bancos
(5433 a 5436, para uso pela IDE) publicam porta no host. O gateway, o Eureka e os serviços de negócio não publicam porta: só se alcançam dentro da rede do compose (o gateway, via Nginx).

## Como rodar

Pré-requisitos: **Docker** com Compose v2 (no Windows/macOS, o Docker Desktop aberto), **Git** e internet.
Java 21 só é necessário para rodar testes ou serviços fora do Docker.

**1. Clonar**

```bash
git clone https://github.com/Marianeesouza/SAFER.git
cd SAFER
```

**2. Criar o `.env`.** Nenhuma senha ou chave está no código: o compose **não sobe sem o `.env`**.

```bash
cp .env.example .env
```

(No PowerShell: `copy .env.example .env`.) Preencha todos os valores:

| Variável | Para que serve |
|---|---|
| `DB_PASSWORD` | Senha dos bancos Postgres (usuário `safer`) |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Acesso ao RabbitMQ e ao painel |
| `JWT_SECRET` | Chave dos tokens de login (o usuarios assina, o gateway valida), **mínimo 32 caracteres** (ex.: `openssl rand -base64 48`) |
| `ADMIN_EMAIL`, `ADMIN_SENHA` | Primeiro administrador, criado ao subir |

O `.env` está no `.gitignore`: nunca o commite.

**3. Subir**

```bash
docker compose up --build
```

A primeira vez demora cerca de 3 minutos (cada serviço compila no Docker; depende da internet). O cache do Gradle
fica guardado, então refazer o build de um serviço depois de mudar código leva poucos segundos. Se uma imagem
falhar por queda de rede, rode o comando de novo. Os serviços levam cerca de um minuto para se registrar no
Eureka; nesse intervalo o gateway pode responder 500.

**4. Acessar**

| O quê | Endereço |
|---|---|
| Aplicação | http://localhost:5500 |
| E-mails enviados (MailHog) | http://localhost:8025 |
| RabbitMQ | http://localhost:15672 (usuário e senha do `.env`) |

**5. Parar**

```bash
docker compose down        # mantém os dados
docker compose down -v     # apaga também os volumes (dados e senhas dos bancos)
```

## Primeiro uso

1. Entre em http://localhost:5500 com o `ADMIN_EMAIL` e a `ADMIN_SENHA`.
2. Em **Gestão de Usuários**, o administrador promove usuários. Todo cadastro nasce `COMUM`; só o administrador
   cria `ANALISTA`.
3. Registre uma ocorrência na página inicial (não precisa de login) e guarde o **protocolo**.
4. Veja os e-mails no MailHog: o comunicante recebe a confirmação, e analistas e administradores recebem o aviso.

Cadastro de usuário: senha com no mínimo 10 caracteres, com maiúscula, minúscula, número e um caractere especial
(`@#$%&*!?-_`); celular obrigatório.

## Regras principais

- **Cargos** (padrão State no `usuarios-service`): `COMUM` registra e cancela as próprias ocorrências;
  `ANALISTA` assume ocorrências e atualiza o status das que assumiu; `ADMINISTRADOR` faz tudo e gerencia usuários.
  O ocorrencias pergunta ao usuarios se a ação é permitida e **nega se ele estiver indisponível**.
- **Status** (código numérico): 1 `EM_ABERTO` → 2 `EM_ATENDIMENTO` → 3 `NECESSITA_ATENDIMENTO_MEDICO` →
  4 `FINALIZADO_COM_SUCESSO` ou 5 `FINALIZADO_COM_COMPLICACOES`; 6 `CANCELADO` (só enquanto está em aberto).
- **Registro:** espécie e descrição obrigatórias, até 5 imagens e coordenadas dentro do campus (ou só o endereço).
- **E-mails:** o ocorrencias monta o e-mail e publica no RabbitMQ; o notificacoes o envia sem duplicar
  (idempotente), com 3 tentativas e fila de mensagens mortas.

## API

Entra-se por `http://localhost:5500/api/...` (Nginx, que repassa ao gateway), com prefixos
`/api/auth` e `/api/usuarios`, `/api/ocorrencias` e `/api/geocoding`. Rotas protegidas exigem
`Authorization: Bearer <token>` (o `tokenSessao` do login, válido por 24 h):

```bash
curl -s -X POST http://localhost:5500/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"SEU_ADMIN_EMAIL","senha":"SUA_ADMIN_SENHA"}'
```

Para ver todas as rotas, ligue o Swagger (veja abaixo).

## Desenvolvimento

Testes (não precisam do Docker nem do `.env`):

```bash
./gradlew test
```

Para rodar os serviços pela IDE (`bootRun` por módulo), suba só a infraestrutura e o Eureka:

```bash
docker compose up usuarios-db ocorrencias-db notificacoes-db geocodificacao-db rabbitmq mailhog
./gradlew :discovery-server:bootRun
```

Exporte no ambiente de cada serviço as variáveis do `.env` que ele usa (`DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_SENHA`,
`RABBITMQ_USER`, `RABBITMQ_PASSWORD`; `JWT_SECRET` só no usuarios e no api-gateway). Os padrões de `localhost` já apontam para o
compose.

**Swagger** vem desligado. Rodando o serviço pela IDE com `SWAGGER_ENABLED=true`, ele fica em
`http://localhost:<porta>/swagger-ui/index.html`.

Estrutura: um módulo por serviço (`usuarios-service`, `ocorrencias-service`, `geocodificacao-service`,
`notificacoes-service`, `api-gateway`, `discovery-server`), `frontend/` (HTML/CSS/JS + Nginx), `compose.yaml` e
`docs/modelagem-astah/` (modelagem original).

## Segurança

- **A autenticação acontece só no gateway.** O usuarios emite o token no login, e o gateway o valida (assinatura e
  validade) antes de rotear. `JWT_SECRET` é o mesmo nos dois; os demais serviços não o conhecem.
- Rotas públicas (sem token): `POST /api/auth/login`, `POST /api/usuarios` (cadastro), `POST /api/ocorrencias`,
  `GET /api/ocorrencias/protocolo/**` e `GET /api/geocoding/**`. Qualquer outra rota sem token válido recebe `401`
  direto do gateway.
- Com o token válido, o gateway repassa a identidade nos cabeçalhos `X-User-Id`, `X-User-Email`, `X-User-Nome` e
  `X-User-Cargo`, e o token não segue adiante. Cabeçalhos `X-User-*` enviados pelo cliente são descartados. As
  regras por cargo continuam em cada serviço, a partir desses cabeçalhos.
- Esse modelo só é seguro porque os serviços de negócio não publicam porta: qualquer um que alcançasse um serviço
  direto poderia forjar os cabeçalhos. Não publique as portas deles.
- A rota `/internal/...` do usuarios não exige token: ela só é segura porque o gateway não a roteia e o serviço
  não publica porta.

Mais detalhes: [`frontend/README.md`](frontend/README.md) e [`geocodificacao-service/README.md`](geocodificacao-service/README.md).
