# frontend (página de teste)

Tela de cadastro de ocorrência em HTML, CSS e JavaScript simples, só para testar o serviço de
geocodificação com o Leaflet. Não fala com nenhum outro serviço e não envia nada: ao final, só mostra
os dados preenchidos. O formulário em 3 etapas, o centro do mapa e as cores vêm do protótipo anterior
(`Bits-Busters/SAFER`).

A página não conhece o endereço de nenhum serviço: ela chama caminhos relativos (`/api/geocoding/...`)
e um Nginx (serviço `frontend` do `compose.yaml`, configurado em `nginx.conf`) repassa a chamada ao
`geocodificacao-service`. Por isso não precisa de CORS, e a página não passa pelo gateway nem pelo
Eureka; mais adiante o Nginx passa a apontar para o gateway.

| Ação na tela | Chamada |
|---|---|
| Abrir a página (tiles do mapa) | `GET /api/geocoding/map/config` |
| Buscar um local pelo nome | `GET /api/geocoding/coordinates?address=` |
| Clicar no mapa | `GET /api/geocoding/address?latitude=&longitude=` |
| Marcar um ponto | `GET /api/geocoding/campus/validate?latitude=&longitude=` |

## Como rodar

```bash
docker compose up --build frontend
```

Isso sobe o banco, o `geocodificacao-service` e o Nginx. Abra `http://localhost:5500`.

Estrutura: `public/` (a página, servida pelo Nginx) e `nginx.conf`.
