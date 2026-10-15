# Horror Movie Hub

Plataforma para fãs de terror: lista pessoal de **filmes** de terror, alertas de estreia/streaming e gerador de maratona que cabe no tempo que você tem.

## Stack
- Java 21 + Spring Boot 3 (Web, Data JPA, Security, Validation, Scheduler)
- PostgreSQL + Flyway
- Redis para cachear as respostas da TMDB
- JWT para autenticação
- **TMDB API v3** (filmes, gênero Horror = `27`; provedores de streaming via `/movie/{id}/watch/providers`)
- JUnit 5, Mockito e Testcontainers
- Docker Compose, GitHub Actions e Swagger/OpenAPI

## Entidades
| Entidade | Campos-chave |
|---|---|
| Usuario | id, nome, email, senha (hash), papel (USER/ADMIN), chatIdTelegram |
| Filme | id, tmdbId, titulo, tituloOriginal, sinopse, duracaoMin, dataLancamento, posterUrl, notaTmdb, subgenero |
| ItemLista | usuario, filme, status (QUERO_VER, ASSISTIDO, ABANDONADO), nota, assistidoEm |
| Review | usuario, filme, texto, nota, medometro (1–5), data |
| Alerta | usuario, filme, tipo (ESTREIA, STREAMING), ativo, notificadoEm |
| Maratona | usuario, tempoDisponivelMin, nivelMedoMin, itens, duracaoTotal |

## Endpoints
**Auth** — `POST /auth/registro`, `POST /auth/login`

**Catálogo**
- `GET /filmes/busca?q=` — busca na TMDB (só gênero Horror), com cache, e salva localmente
- `GET /filmes/{id}` — detalhes + média de nota e medômetro da comunidade
- `GET /filmes/em-alta` — mais bem avaliados no hub
- `GET /filmes/em-breve` — próximas estreias de terror (`/discover/movie` com `primary_release_date.gte=hoje`)

**Minha lista** — `GET /minha-lista?status=`, `POST /minha-lista`, `PATCH /minha-lista/{id}`, `DELETE /minha-lista/{id}`

**Reviews** — `POST /filmes/{id}/reviews`, `GET /filmes/{id}/reviews?page=`

**Alertas** — `GET /alertas`, `POST /alertas/{filmeId}?tipo=ESTREIA|STREAMING`, `DELETE /alertas/{filmeId}?tipo=` (sem tipo remove os dois)

**Usuário** — `PUT /usuarios/me/telegram` com `{ "chatId": "123" }`

**Maratona** — `POST /maratonas/gerar` com `{ "tempoMin": 300, "nivelMedoMin": 3, "apenasQueroVer": true }`, `GET /maratonas`

## Regras de negócio
- Marcar como ASSISTIDO preenche `assistidoEm`; nota só é aceita em filme ASSISTIDO.
- Uma review por usuário e filme (constraint única no banco), nota 0–10 e medômetro 1–5 validados.
- **Job de alertas** (`@Scheduled`, a cada 6h):
  - ESTREIA: dispara no Telegram quando `dataLancamento <= hoje`.
  - STREAMING: consulta watch providers (região BR) e dispara quando o filme aparece em algum serviço.
  - Respeita o rate limit da TMDB (~40 req/10s) com intervalo entre chamadas; marca `notificadoEm` para não repetir.
- **Gerador de maratona = mochila 0/1**: capacidade = tempo disponível, peso = duração do filme, valor = nota da comunidade combinada com o medômetro. Programação dinâmica, testes cobrindo casos-limite.

## Pacotes (por feature)
```
com.douglas.horrorhub
 ├─ auth/      (controller, service, JwtFilter)
 ├─ filme/     (controller, service, repository, TmdbClient)
 ├─ lista/
 ├─ review/
 ├─ alerta/    (AlertaJob, TelegramNotifier)
 ├─ maratona/  (MaratonaService, MochilaSolver)
 └─ shared/    (exceptions, GlobalExceptionHandler, config)
```

## Fases
1. Base: Docker Compose, Flyway, auth JWT, CRUD de usuário. ✅
2. Catálogo: TmdbClient filtrando Horror, cache Redis, persistência dos filmes. ✅
3. Lista e reviews: regras de status, paginação, medômetro. ✅
4. Maratona: MochilaSolver + MaratonaService, POST /maratonas/gerar e GET /maratonas. ✅
5. Alertas: job agendado + bot do Telegram. ✅
6. Acabamento: Swagger, CI, Testcontainers, README com diagrama. ✅ (falta deploy no Render/Railway)
7. Extra: front simples (React ou Thymeleaf).

## Para o README / LinkedIn
Destaque: integração resiliente com API externa (cache + rate limit), o algoritmo de otimização da maratona e o job de notificações de estreia/streaming.
