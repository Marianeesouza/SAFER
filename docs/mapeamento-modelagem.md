# Mapeamento entre a modelagem (Astah) e a implementação

Este documento relaciona os três diagramas do projeto (casos de uso, componentes e classes do caso de uso
*Registrar Ocorrência*) com o código. Refere-se ao estado da branch `eureka-desacoplamento` no commit `377a943`
(25/11/2026), com os diagramas de casos de uso e de componentes já na versão corrigida da Parte 2.

**Conclusão:** a implementação cobre todos os casos de uso e todos os componentes do modelo, e mantém os padrões
de projeto pedidos (State, Adapter, Repository). As diferenças vêm de o sistema ter sido dividido em
microsserviços e estão listadas, com a justificativa, na seção [Desvios](#desvios-em-relação-à-modelagem).

## 1. Casos de uso

Atores: **Visitante** (quem não está logado; não é um cargo), **Usuário Comum**, **Analista de Incidentes** (um
Usuário Comum com mais permissões) e **Administrador de Sistema** (um Analista com mais permissões). A herança
vale no código: cada cargo faz tudo o que o anterior faz.

### Controle de Acesso (`usuarios-service`)

| Caso de uso | Implementação |
|---|---|
| Realizar Autocadastro | `POST /usuarios` (público). O usuário nasce com cargo `COMUM` |
| Realizar Login | `POST /auth/login`, devolve o JWT (`tokenSessao`, 24 h) |
| Gerenciar Próprio Perfil | `PATCH /usuarios/me` e `GET /auth/me`; tela "Meu Perfil" |
| Gerenciar Cargos de Usuário | `PATCH /usuarios/{id}/cargo` (só administrador), com as regras de transição do State |
| Validar Permissão por Cargo (incluído) | `POST /usuarios/{id}/permissoes/validar`. Ver a seção 1.3 |
| Obter Destinatários do Aviso (incluído) | `GET /internal/usuarios/destinatarios-novas-ocorrencias`. Ver a seção 1.3 |

### Controle de Ocorrências (`ocorrencias-service`)

| Caso de uso | Implementação |
|---|---|
| Registrar Ocorrência | `POST /ocorrencias`. Público, com ou sem login |
| Visualizar e Acompanhar Ocorrências | Visitante: `GET /ocorrencias/protocolo/{protocolo}` (público). Usuário logado: `GET /ocorrencias/usuario/{id}` (as próprias) e `GET /ocorrencias/{id}`. Analista e administrador: `GET /ocorrencias/em-aberto` e `GET /ocorrencias?status=` |
| Cancelar Ocorrência | `PATCH /ocorrencias/{id}/cancelar`: criador ou administrador, só enquanto `EM_ABERTO` |
| Gerenciar Status de Ocorrência | `POST /ocorrencias/{id}/vincular-analista` (assumir) e `PATCH /ocorrencias/{id}/status` |

### Geocodificação e Notificações

| Caso de uso | Implementação |
|---|---|
| Geocodificar Endereço | `geocodificacao-service`: `GET /geocoding/coordinates`, `/address`, `/map/config` e `/campus/validate`. Ator externo: Nominatim |
| Enviar Notificação | `notificacoes-service`: consome o pedido de e-mail e envia por SMTP. Ator externo: serviço de e-mail (MailHog no desenvolvimento) |

### 1.3 As relações `<<include>>`

| Caso de uso que inclui | Caso de uso incluído | Como acontece no código |
|---|---|---|
| Registrar Ocorrência | Geocodificar Endereço | `GeocodificacaoClient` valida se as coordenadas estão no campus |
| Visualizar e Acompanhar | Geocodificar Endereço | **não é realizado como chamada** (ver o desvio D7): a tela mostra o endereço e as coordenadas gravados no registro, com um mapa Leaflet |
| Registrar, Cancelar e Gerenciar Status | Enviar Notificação | `EmailNotificacaoPublisher` publica o pedido de e-mail em cada um |
| Cancelar Ocorrência e Gerenciar Status | **Validar Permissão por Cargo** | `UsuariosClient.validarPermissaoOuFalhar` pergunta ao usuarios se o cargo permite a ação; **se ele estiver fora do ar, a ação é abortada** (resposta 503) |
| Registrar Ocorrência | **Obter Destinatários do Aviso** | `UsuariosClient.listarDestinatariosDeNovasOcorrencias` traz quem deve ser avisado (`notificarNovaOcorrencia()` do State) |

Os dois últimos casos de uso incluídos não têm ator: são acionados por outros casos de uso, e conversam só com o
banco do usuarios. Só têm ator os casos de uso que falam com um sistema externo (Nominatim e e-mail).

## 2. Diagrama de componentes

| Componente do modelo | Módulo no código | Porta |
|---|---|---|
| Front-end Web (HTML5, CSS3, JS, Nginx, Leaflet) | `frontend/` + serviço `frontend` do `compose.yaml` | 5500 |
| APIGateway (Spring Cloud Gateway) | `api-gateway` (roteia e valida o JWT) | 8080 (interna) |
| ServiceUsuarios (Spring Security) | `usuarios-service` | 8081 (interna) |
| ServiceOcorrencias | `ocorrencias-service` | 8082 (interna) |
| ServiceNotificacoes (Spring Boot Mail) | `notificacoes-service` | 8084 (interna) |
| ServiceGeocodificacao | `geocodificacao-service` | 8083 (interna) |
| Discovery (Netflix Eureka) | `discovery-server` | 8761 (interna) |
| APIGeocodificacao (Nominatim) | externa, chamada por `NominatimAdapter` | — |
| ComunicacaoEmail (SMTP) | MailHog no desenvolvimento (o diagrama indica Gmail SMTP; ver D6) | 1025 / 8025 |

O RabbitMQ não aparece no diagrama de componentes: ele é o meio pelo qual o ServiceOcorrencias usa o
`IServiceNotificacoes` (ver D2).

As interfaces do modelo correspondem a:

- `IGateway`: as rotas `/api/*` do gateway, chamadas pelo Nginx. É nelas que o token JWT é validado.
- `IServiceUsuarios`: `/api/auth`, `/api/usuarios`, e as chamadas do ocorrencias ao usuarios (permissão e destinatários).
- `IServiceOcorrencias`: `/api/ocorrencias`.
- `IServiceGeocodificacao`: `/geocoding/*`. É requerida pelo **gateway** (rota `/api/geocoding/**`, usada pelo
  frontend; ver D4) e pelo **ocorrencias** (validação do campus). Nas duas, o endereço vem do Eureka (`lb://`).
- `IServiceNotificacoes`: o pedido de e-mail publicado no RabbitMQ (exchange `safer.ocorrencias.exchange`, chave `notificacao.email.enviar`).
- `IDiscovery`: o registro de todos os serviços e do gateway no Eureka. Nenhum serviço guarda o endereço de outro.
- `IComunicacaoEmail` e `IAPIGeocodificacao`: `EmailSender` e `ProvedorGeocodificacao`.

## 3. Diagrama de classes (caso de uso Registrar Ocorrência)

### Camada GUI

| Modelo | Código |
|---|---|
| `TelaCadastroOcorrencia` (exibir mapa, preencher endereço, enviar ocorrência, mensagem com o protocolo) | `frontend/public/index.html` e `app.js` |

### Camada de Negócio

| Modelo | Código |
|---|---|
| `Fachada` e `ControladorCadastroOcorrencia` | `OcorrenciasController` (REST) e `OcorrenciasService` |
| `cadastrarOcorrencia`, `verificarDadosFormulario` | `registrarOcorrencia` e a validação (`@Valid` nos DTOs, regras de espécie, imagens e campus) |
| `getInformacoesUsuario(tokenSessao)` e `CadastroLogin.getUsuarioViaToken` | `GET /auth/me`. O token é emitido pelo `JwtService` do usuarios e validado pelo `AutenticacaoJwtFilter` do gateway; os serviços leem a identidade verificada pelo `IdentidadeGatewayFilter` (ver o desvio D8) |
| `obterConfiguracaoMapa`, `obterEnderecoPorCoordenadas` | `GET /geocoding/map/config` e `/address`, chamados direto pelo frontend (ver o desvio D4) |
| `CadastroOcorrencia` | `OcorrenciasService` (`registrarOcorrencia`, `buscarDetalhesPorId`, `buscarDetalhesPorProtocolo`, `listarEmAberto`, `listarPorUsuario`, `vincularAnalista`, `atualizarStatus`, `cancelarOcorrencia`) |
| `CadastroHistoricoOcorrencia` | gravação do histórico dentro de `OcorrenciasService` e `HistoricoOcorrenciaRepository` |
| `Ocorrencia`, `DadosComunicante`, `DadosLocalizacao`, `Coordenadas`, `HistoricoOcorrencia` | as mesmas classes em `ocorrencias-service/.../model` (`numeroProtocolo` é `protocolo`; `dataHora` é `criadoEm`) |
| `Foto[*]` | lista de imagens em Base64, tabela `ocorrencia_imagens` (até 5 por ocorrência) |
| `StatusOcorrencia` (6 valores) | `StatusOcorrencia`, gravado como código: 1 `EM_ABERTO`, 2 `EM_ATENDIMENTO`, 3 `NECESSITA_ATENDIMENTO_MEDICO`, 4 `FINALIZADO_COM_SUCESSO`, 5 `FINALIZADO_COM_COMPLICACOES`, 6 `CANCELADO` |
| `RelacaoUfrpe` | `RelacaoUfrpe` (Discente, Docente, Técnico, Terceirizado, Visitante) |
| `Usuario`, `Login` | `Usuario` com `Login` embutido (e-mail e hash da senha), em `usuarios-service/.../model` |
| `CargoUsuario`, `CargoComum`, `CargoAnalista`, `CargoAdministrador` | as mesmas quatro, em `usuarios-service/.../model/cargo` |
| `Usuario.mudarCargo`, `podeCancelar/Assumir/AtualizarOcorrencia`, `notificarNovaOcorrencia` | os mesmos métodos em `Usuario`, delegando ao cargo |

### Subsistemas

| Modelo | Código |
|---|---|
| `FachadaComunicacaoAPIGeocodificacao` | `GeocodificacaoService` (consulta o cache e depois o provedor); do lado do ocorrencias, `GeocodificacaoClient` |
| `IAdaptadorServicoGeocodificacao` e `AdaptadorServicoGeocodificacao` | `ProvedorGeocodificacao` e `NominatimAdapter` |
| `GeoConfigsDTO` (urlTemplate, atribuição, maxZoom) | `MapConfig` e `GET /geocoding/map/config` (o endpoint existe, mas o frontend não o consome; ver D4) |
| `FachadaComunicacaoEmail` | `NotificacaoService` e `EnviarEmailListener` |
| `IAdaptadorComunicacaoEmail` e `AdaptadorComunicacaoEmail` | `EmailSender` e `SmtpEmailSender` |
| `ServicoEmailSDK` | `JavaMailSender` (Spring Mail) |
| `ISubsistemaComunicacaoNotificacao` (`notificaAnalistas`, `notificarUsuario`) | `EmailNotificacaoPublisher` e `EmailOcorrenciaBuilder` no ocorrencias (ver o desvio D2) |

### Camada de Dados

Os `IRepositorio...` e as classes `...BDR` correspondem aos repositórios do Spring Data JPA
(`OcorrenciaRepository`, `HistoricoOcorrenciaRepository`, `UsuarioRepository`) sobre PostgreSQL, um banco por serviço.

## 4. Padrões de projeto

| Padrão | No modelo | No código |
|---|---|---|
| **State** | `CargoUsuario` e os três cargos | igual: cada cargo responde `podeAssumir`, `podeAtualizar`, `podeCancelar`, `notificarNovaOcorrencia` e `podeMudarPara` |
| **Adapter** | geocodificação e e-mail | `NominatimAdapter` e `SmtpEmailSender`, atrás de interfaces |
| **Repository** | `IRepositorio...` + `BDR` | interfaces do Spring Data JPA |
| **Facade e Singleton** | `Fachada`, controladores e `Cadastro...` singleton | a Fachada passou a ser o **API Gateway**; controladores, serviços e repositórios são beans do Spring, singletons por padrão |

## Desvios em relação à modelagem

**D1. Divisão em microsserviços.** As camadas GUI, Negócio e Dados do diagrama de classes se distribuem em quatro
serviços (usuarios, ocorrencias, geocodificacao, notificacoes), com Eureka e gateway. Consequência: classes que
no modelo se referenciam diretamente passaram a se comunicar por HTTP ou mensagem.

**D2. Notificações.** No modelo, o subsistema de notificação tem `notificaAnalistas(...)` e `notificarUsuario(...)`
e decide o conteúdo. No código, o **ocorrencias monta o e-mail pronto** (e consulta o usuarios para saber os
destinatários), e o notificacoes **só envia**. Motivo: manter o notificacoes sem regra de negócio, como um adaptador
de saída. O canal é uma fila (RabbitMQ), com idempotência por chave, três tentativas e fila de mensagens mortas.
O diagrama de componentes mostra a dependência lógica `ServiceOcorrencias → IServiceNotificacoes`, que continua
válida; a fila é a forma assíncrona de implementá-la. Não há `verificaDisponibilidade()`.

**D3. State com ids e autorização remota.** No modelo, `podeAssumirOcorrencia(ocorrencia)` recebe o objeto. Como
`Ocorrencia` e `Usuario` estão em serviços diferentes, os métodos recebem ids
(`podeAtualizarOcorrencia(analistaResponsavelId)`, `podeCancelarOcorrencia(criadorOcorrenciaId)`) e o ocorrencias
pergunta ao usuarios por HTTP. Se o usuarios não responde, a ação é abortada (503). Pelo mesmo motivo, a associação
`usuarioComunicante` da ocorrência é guardada como `usuarioId`.

**D4. Geocodificação exposta pelo gateway.** No modelo, quem fornecia a configuração do mapa e o endereço por
coordenadas era o controlador do cadastro. No código, o frontend chama a geocodificação **diretamente, pelo
gateway** (`/api/geocoding/address` e `/campus/validate`, ao clicar no mapa), e o ocorrencias só a usa para validar o
campus ao registrar. O endpoint `/geocoding/map/config` existe, mas o frontend **não o consome**: a URL dos tiles do
OpenStreetMap está fixa no `app.js`.

**D5. `Login` e repositório.** O `Login` foi absorvido por `Usuario` (mesma tabela) e não tem repositório próprio.
O histórico é gravado dentro do `OcorrenciasService`, sem classe `CadastroHistoricoOcorrencia`.

**D6. SMTP.** A nota do componente `ComunicacaoEmail` no diagrama aponta para o Gmail. No desenvolvimento, o
destino é o **MailHog** (`MAIL_HOST` no `compose.yaml`). O envio por um SMTP real é questão de configuração do
`spring.mail` (usuário, senha e TLS), que não foi configurada nem testada. O relatório cita o MailHog; a nota do
diagrama ainda deve ser trocada no Astah para os dois ficarem iguais.

**D7. Visualizar sem geocodificar.** O modelo faz *Visualizar e Acompanhar Ocorrências* incluir *Geocodificar
Endereço*. No código, ao visualizar uma ocorrência a tela exibe o endereço e as coordenadas que foram gravados no
registro, com um mapa Leaflet, e **não consulta a geocodificação de novo**. A geocodificação só participa ao
registrar. Para o diagrama ficar fiel ao código, o `<<include>>` de *Visualizar* para *Geocodificar* pode ser
removido; a relação de *Registrar Ocorrência* continua correta.

**D8. JWT validado no gateway.** O modelo não diz onde o token é verificado. No código, o **gateway** é o único
que o valida (`AutenticacaoJwtFilter`: assinatura e validade). Rotas fora da lista pública recebem 401 ali mesmo. O
gateway descarta os cabeçalhos `X-User-*` enviados pelo cliente e repassa só a identidade verificada
(`X-User-Id`, `X-User-Email`, `X-User-Nome`, `X-User-Cargo`), sem o token. O usuarios apenas emite o token, e o
ocorrencias e o usuarios montam a autenticação a partir desses cabeçalhos (`IdentidadeGatewayFilter`). O ocorrencias
não conhece mais o `JWT_SECRET`. Isso só é seguro porque nenhum serviço de negócio publica porta no host.

**D9. Endereços pelo Eureka.** O modelo mostra todos os serviços ligados ao `IDiscovery`. No código, o gateway
(`lb://...`) e o `UsuariosClient` e o `GeocodificacaoClient` do ocorrencias resolvem o destino pelo nome lógico do
serviço no Eureka, com balanceamento do Spring Cloud LoadBalancer. Os bancos e o RabbitMQ continuam por nome de
host do compose.

**D10. RabbitMQ fora do diagrama.** O diagrama liga ServiceOcorrencias a `IServiceNotificacoes` diretamente. No
código, a ligação é uma fila no RabbitMQ (ver D2), que não está desenhada como componente.

## Itens além do modelo

- **Segurança:** JWT emitido pelo usuarios e validado só no gateway (D8); `/internal` só acessível pela rede e
  não roteado pelo gateway.
- **Administração de usuários:** listar e excluir usuários e `GET /auth/me`. O primeiro administrador é criado a
  partir de `ADMIN_EMAIL` e `ADMIN_SENHA`.
- **Infraestrutura:** Eureka, gateway, RabbitMQ, MailHog e Docker Compose. Só o Nginx, o RabbitMQ, o MailHog e os
  bancos (para uso pela IDE, só em `localhost`) publicam porta no host.
- **Banco:** migrações Flyway em cada serviço, com `ddl-auto: validate`.

## Como conferir

- Casos de uso e rotas: seguir o roteiro de "Primeiro uso" do `README.md`.
- State: um usuário `COMUM` recebe 403 ao tentar assumir uma ocorrência, e um `ANALISTA` só atualiza o status da
  que assumiu.
- Inclusões: parar o `usuarios-service` (`docker compose stop usuarios-service`) e tentar cancelar uma ocorrência
  (a ação é abortada com 503), e registrar uma (continua sendo registrada, sem o aviso aos analistas).
- Autenticação no gateway: `GET /api/ocorrencias/em-aberto` sem token devolve `401`; com um cabeçalho `X-User-Cargo:
  ADMINISTRADOR` forjado e sem token, também `401`.
- Descoberta de serviços: o Eureka não publica porta no host; confira com `docker compose logs discovery-server` que
  todos os serviços se registraram, e que `compose.yaml` não define endereços de serviço (só `EUREKA_URL`).
- Testes automatizados: `./gradlew test`.
