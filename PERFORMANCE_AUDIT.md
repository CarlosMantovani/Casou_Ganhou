# Auditoria de performance e escalabilidade

**Projeto:** Presente Premiado<br>
**Data:** 21/08/2026<br>
**Commit auditado:** `a643187`<br>
**Escopo:** front-end React/Vite, API Spring Boot, PostgreSQL/Flyway, Mercado Pago, pools, concorrência, infraestrutura e escala horizontal.

## 1. Resumo executivo

Foram identificados **16 achados**: 0 CRITICAL, 9 HIGH, 5 MEDIUM, 1 LOW e 1 INFO.

Os riscos mais relevantes estão no fluxo que converte pagamento em números da sorte. A geração atual faz uma consulta de existência por candidato, procura duplicatas em uma lista com busca linear e só persiste ao final. Como a quantidade por compra não tem limite de negócio, o custo cresce de forma não limitada, piora à medida que o intervalo se ocupa e mantém uma transação longa. Em paralelo, a idempotência é apenas um `SELECT -> verificar -> gerar`, sem lock de linha ou marcador atômico no banco; webhooks concorrentes podem repetir todo o trabalho.

Também há amplificação entre front-end, API e Mercado Pago: uma página pendente consulta o status a cada 5 segundos sem limite de duração; o back-end pode transformar cada consulta em uma chamada síncrona ao provedor dentro de uma transação. O sorteio, por sua vez, materializa todos os candidatos no back-end, transfere todos ao navegador para uma animação e depois materializa tudo novamente para escolher o vencedor.

Não é possível afirmar quantos usuários simultâneos a aplicação suporta apenas pela inspeção. Não há resultados de carga, métricas de pool, perfis de CPU/heap, latência de queries ou `EXPLAIN ANALYZE` com volume representativo. Todo achado cujo impacto depende desses dados foi classificado como `DEPENDE DE MEDIÇÃO` ou `POTENCIAL`.

## 2. Método e limitações

A auditoria foi executada em duas passadas completas:

1. Mapeamento ponta a ponta dos fluxos `Frontend -> Controller -> Service -> Repository -> PostgreSQL -> Mercado Pago`, incluindo migrations, transações, coleções, serialização, Docker e Vercel.
2. Revalidação dos achados contra constraints do banco, índices, testes, configurações de pool, políticas do React Query e build de produção.

Validações executadas, sem alterar código:

- Back-end: `mvn test` — **87 testes aprovados**.
- Front-end: `npm test -- --run` — **32 testes aprovados**.
- Front-end: `npm run build` — build aprovado, 1.910 módulos transformados.
- Bundle medido: JavaScript público **469,08 kB / 143,64 kB gzip**; chunk admin **38,77 kB / 9,99 kB gzip**; CSS **21,71 kB / 5,30 kB gzip**.
- O build confirmou **173 SVGs inline** no JavaScript público e 22 SVGs emitidos separadamente.

Limitações:

- Não houve acesso a dados ou métricas de produção/homologação.
- Não foi executado teste de carga.
- Não foi executado `EXPLAIN (ANALYZE, BUFFERS)` com cardinalidade representativa.
- Não houve simulação de latência/falha real do Mercado Pago.
- A integração SMTP não existe no código atual; há apenas variáveis no `.env.example`, portanto não há cliente/pool/fila de e-mail implementado para medir.

### Significado dos status

- `CONFIRMADO`: o comportamento ou custo estrutural está diretamente demonstrado pelo código/build.
- `POTENCIAL`: existe uma condição de corrida, configuração ausente ou cenário plausível que precisa ocorrer para produzir o impacto.
- `DEPENDE DE MEDIÇÃO`: o código merece validação, mas somente dados de runtime podem demonstrar se é gargalo no volume real.

## 3. Controles positivos observados

- `spring.jpa.open-in-view=false` evita manter o contexto JPA aberto durante a serialização HTTP.
- `LuckyNumber.transaction` usa `FetchType.LAZY`.
- A listagem administrativa usa `Page` com tamanho padrão 20 e busca os números da página em uma única query, evitando N+1 por transação nessa tela.
- Resumo administrativo e ranking usam projections/agregações no banco, sem carregar entidades completas para somar em Java.
- `lucky_number.number` possui constraint única global e `transaction.external_reference` possui índice único.
- Há índices para `lucky_number.transaction_id`, `transaction.status` e `(phone, recovery_code)`.
- JWT é stateless; não há sessão HTTP local, scheduler, lock local ou arquivo local persistente que exija sticky session.
- A lista de bandeiras é imutável e carregada uma vez no startup.
- A área administrativa usa lazy loading e fica fora do chunk público inicial.
- O front-end é estático e pode ser servido por CDN/Vercel independentemente das instâncias da API.

## 4. Achados

### [PERF-01] Geração de números tem consultas O(q), busca em memória O(q²) e persistência sem lote efetivo

**Severidade:** HIGH<br>
**Status:** CONFIRMADO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/LuckyNumberServiceImpl.java`; `backend/src/main/java/com/weddingraffle/rifa/entity/LuckyNumber.java`; `backend/src/main/resources/application.yml`<br>
**Método / trecho:** `generateFor`, `nextAvailableNumber` e `contains` (linhas 31-42 e 61-85); `LuckyNumber.id` (linhas 18-23)

**Problema:** para cada número solicitado, o serviço percorre os números ainda não salvos com `stream().anyMatch` e executa `existsByNumber` no PostgreSQL. O custo de CPU da lista cresce como O(q²); as consultas crescem no mínimo como O(q), além das colisões. `saveAll` não elimina o problema: não há `hibernate.jdbc.batch_size` configurado e a entidade usa `GenerationType.IDENTITY`, estratégia que normalmente impede batching de inserts pelo Hibernate.

**Evidência:** o laço de quantidade chama `nextAvailableNumber`; cada tentativa chama `contains(pendingNumbers, candidate)` e `luckyNumberRepository.existsByNumber(candidate)`. O intervalo padrão possui 100.000 posições e os DTOs aceitam qualquer `Integer >= 1`. A persistência só ocorre depois de todos os candidatos terem sido acumulados em memória.

**Impacto:** alta latência, muitas viagens ao banco, crescimento de heap e transações longas durante webhook ou pagamento em dinheiro. Perto da ocupação total, colisões aumentam; se a quantidade exceder a capacidade restante, o método pode fazer trabalho extremo e falhar somente no final.

**Cenário:** compras grandes; intervalo com 50%, 90% ou 99% de ocupação; 10 a 1.000 aprovações simultâneas; quantidade maior que a capacidade disponível.

**Correção recomendada:** manter a constraint única, mas substituir o algoritmo por alocação set-based/atômica no banco. Opções a avaliar: pré-gerar o universo de números e reivindicar linhas livres com `FOR UPDATE SKIP LOCKED`; gerar candidatos em blocos, consultar existentes em lote e inserir em batch com retry de conflito; ou outra estratégia que preserve aleatoriedade e unicidade sem uma query por candidato. Validar atomicamente `quantity <= capacidade restante` antes de criar/cobrar. A regra “sem limite máximo por transação” deve ser preservada; não introduzir limite de negócio sem aprovação.

**Como medir:** k6/JMeter com quantidades 1, 10, 100, 1.000 e 10.000 e ocupação 0%, 50%, 90% e 99%; contar statements no PostgreSQL; medir p50/p95/p99, CPU, alocações/GC via JFR, tempo de transação, locks e rollbacks.

**Complexidade da correção:** HIGH

### [PERF-02] Chamadas externas e loops de reconciliação ficam dentro de transações de banco

**Severidade:** HIGH<br>
**Status:** CONFIRMADO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/TransactionServiceImpl.java`<br>
**Método / trecho:** `create`, `getStatus`, `recover` e `refreshPendingTransaction` (linhas 75-105 e 140-185)

**Problema:** `create` abre uma transação, consulta configuração e depois chama o Mercado Pago antes de persistir. `getStatus` consulta a transação e pode chamar o provedor ainda dentro de `@Transactional`. `recover` carrega todas as transações correspondentes e chama o Mercado Pago sequencialmente para cada uma que estiver pendente.

**Evidência:** `paymentProviderClient.createPreference` está dentro de `create @Transactional`; `paymentProviderClient.getPayment` é alcançado por `getStatus @Transactional` e pelo laço de `recover @Transactional`. Nos dois últimos casos a aplicação já consultou o banco antes do I/O externo.

**Impacto:** a janela transacional acompanha a latência externa; conexões/EntityManagers, threads HTTP e snapshots podem permanecer ocupados por muito mais tempo que a operação SQL. O laço de recuperação multiplica esse tempo pelo número de transações pendentes do telefone.

**Cenário:** Mercado Pago lento/indisponível; telefone com várias compras pendentes; rajada de retornos de pagamento; pool pequeno ou banco remoto.

**Correção recomendada:** separar I/O externo de transações curtas. Ler apenas os identificadores necessários, encerrar a transação, consultar o provedor e abrir uma nova transação curta para lock/validação/atualização. Na criação, evitar manter a transação durante `createPreference`; definir estratégia de compensação/idempotência para a preferência criada. Limitar e paginar a reconciliação por telefone em vez de chamadas externas sequenciais não limitadas.

**Como medir:** temporizar duração de `@Transactional`, chamada externa e aquisição/uso de conexão separadamente; acompanhar Hikari active/pending, threads Tomcat, p95/p99 e latência simulada do provedor em 100 ms, 1 s, 10 s e timeout.

**Complexidade da correção:** HIGH

### [PERF-03] Timeout do Mercado Pago não é configurado e o retry não diferencia falhas transitórias de 4xx

**Severidade:** HIGH<br>
**Status:** DEPENDE DE MEDIÇÃO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/integration/MercadoPagoClient.java`; `backend/src/main/resources/application.yml`<br>
**Método / trecho:** construtor e métodos `createPreference`/`getPayment` (linhas 35-80); propriedades de retry (linhas 42-45)

**Problema:** a aplicação só configura o access token do SDK. Não há configuração explícita de connection timeout/read timeout de 1 minuto. Qualquer `MPApiException` ou `MPException` vira o mesmo `ExternalPaymentException`, e `@Retryable` repete todas essas exceções sem inspecionar status HTTP. Não há bulkhead nem circuit breaker.

**Evidência:** não existe propriedade de timeout em `AppProperties`/YAML nem configuração do cliente HTTP do SDK. Os dois catches agrupam exceções de API e transporte; o retry usa apenas a classe wrapper.

**Impacto:** falhas permanentes podem ser repetidas inutilmente; chamadas lentas podem ocupar threads por tempo definido pelo default do SDK; uma indisponibilidade do provedor pode propagar fila e exaustão para API e banco.

**Cenário:** 401/403/422, 429, 5xx, conexão lenta, DNS/TLS indisponível ou timeout do provedor sob 50 a 1.000 requisições concorrentes.

**Correção recomendada:** configurar connection/read timeout explicitamente; classificar retry somente para timeout, 429 quando seguro e 5xx transitório; respeitar `Retry-After`; aplicar jitter e limites; adicionar bulkhead/circuit breaker com fallback adequado. Não repetir erros de validação/autorização.

**Como medir:** fault injection com respostas 400, 401, 422, 429, 500 e conexões que não respondem; registrar número de tentativas, tempo total, threads ocupadas e taxa de erro, sem logar tokens/dados sensíveis.

**Complexidade da correção:** MEDIUM

### [PERF-04] Polling pendente pode gerar fan-out indefinido até o Mercado Pago

**Severidade:** HIGH<br>
**Status:** DEPENDE DE MEDIÇÃO<br>
**Arquivo:** `frontend/src/features/payment-return/PaymentReturnPage.tsx`; `backend/src/main/java/com/weddingraffle/rifa/service/impl/TransactionServiceImpl.java`<br>
**Método / trecho:** `useQuery` (linhas 20-25); `getStatus`/`refreshPendingTransaction` (linhas 140-185)

**Problema:** enquanto o status for `PENDENTE`, cada navegador consulta a API a cada 5 segundos sem duração máxima ou backoff. Se já houver `mpPaymentId`, cada consulta pode gerar outra chamada síncrona ao Mercado Pago e nova tentativa de atualização.

**Evidência:** `refetchInterval` permanece em 5.000 ms enquanto os dados forem pendentes; não há contador, timeout total ou aumento de intervalo. O back-end chama `getPayment` para transações pendentes com `mpPaymentId`.

**Impacto:** muitos navegadores abertos podem multiplicar tráfego API, conexões, threads, consultas e cota do Mercado Pago. A origem da carga é o número de abas pendentes, não apenas o número de pagamentos novos.

**Cenário:** se todos estiverem em página pendente, 10/50/100/200/500/1.000 clientes produzem teoricamente até 2/10/20/40/100/200 consultas de status por segundo, antes de retries/refetches adicionais.

**Correção recomendada:** usar backoff progressivo com teto, parar após janela definida e pausar quando a página estiver oculta/offline; manter webhook como caminho principal; coalescer/cachear reconciliações por pagamento no servidor com `lastCheckedAt`; impedir que várias consultas concorrentes consultem o mesmo pagamento externamente. Qualquer prazo exibido ao usuário é decisão de negócio e deve ser confirmado antes de implementação.

**Como medir:** k6 com 10 a 1.000 VUs pendentes por 15-30 minutos, provedor mockado; medir RPS interno/externo, p95/p99, conexões, threads, taxa de retry e custo por pagamento.

**Complexidade da correção:** MEDIUM

### [PERF-05] Idempotência do webhook é read-check-write e não resiste a concorrência

**Severidade:** HIGH<br>
**Status:** POTENCIAL<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/TransactionServiceImpl.java`; `backend/src/main/java/com/weddingraffle/rifa/service/impl/LuckyNumberServiceImpl.java`; `backend/src/main/resources/db/migration/V1__create_raffle_schema.sql`<br>
**Método / trecho:** `processPaymentNotification` (linhas 109-132); `generateFor` (linhas 31-42); constraints de `lucky_number` (linhas 44-60)

**Problema:** duas notificações concorrentes podem ler a mesma transação como não aprovada e ambas chamar `generateFor`. Dentro do gerador, ambas podem observar `existsByTransaction=false`. Não há `@Version`, lock pessimista, claim atômico ou tabela de eventos idempotentes. A unicidade global de `number` evita apenas o mesmo número; não impede dois conjuntos diferentes para a mesma transação.

**Evidência:** a decisão ocorre após um `findByExternalReference` normal; `existsByTransaction` também é um SELECT normal. O schema não contém constraint/estado que marque atomicamente “geração iniciada/concluída”. Os testes cobrem duplicata sequencial, não duas threads/transações.

**Impacto:** geração duplicada, quantidade maior que a paga, inserts extras, colisões/rollbacks e tempestade de retries. Em múltiplas instâncias, nenhum estado em memória coordena o fluxo.

**Cenário:** webhooks duplicados entregues ao mesmo tempo, polling e webhook aprovando simultaneamente, retry do Mercado Pago ou duas instâncias processando o mesmo pagamento.

**Correção recomendada:** serializar a transição no banco: lock de linha da transação ou update compare-and-set de estado; persistir/validar evento idempotente por payment/event id; marcar geração de forma atômica; garantir invariantes no schema e repetir conflitos de unicidade com transação nova e limitada. A chamada externa deve ocorrer antes da seção crítica curta.

**Como medir:** teste de integração PostgreSQL real com 2, 10, 50 e 100 workers processando o mesmo pagamento; verificar contagem final igual a `quantity`, tempo de lock, deadlocks, rollbacks e statements.

**Complexidade da correção:** HIGH

### [PERF-06] Sorteio materializa e transfere todos os candidatos duas vezes

**Severidade:** HIGH<br>
**Status:** CONFIRMADO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/repository/LuckyNumberRepository.java`; `backend/src/main/java/com/weddingraffle/rifa/service/impl/RaffleServiceImpl.java`; `frontend/src/features/admin/AdminDrawPage.tsx`<br>
**Método / trecho:** `findEligibleForDraw` (linhas 26-34); `listEligibleNumbers`/`drawNewWinner` (linhas 51-69); mutation do sorteio (linhas 24-29)

**Problema:** antes do sorteio, o navegador baixa todos os números elegíveis com nome e bandeira para a animação. Depois de 5,5 segundos, chama `/raffle/draw`, que executa novamente a mesma query, cria outra lista completa de entidades com `join fetch` e só então seleciona um índice.

**Evidência:** `/raffle/eligible-numbers` retorna `List`; o front mantém a lista inteira; `draw()` chama `getEligibleLuckyNumbers()` novamente. Não há paginação, projection limitada ou seleção no banco.

**Impacto:** duas leituras completas, heap e GC no back-end, serialização JSON, transferência e memória no navegador. A transação de sorteio fica aberta durante o carregamento de toda a coleção.

**Cenário:** 10.000 a 100.000 números elegíveis; rede móvel/lenta; instância com heap limitado no dia do evento; duas requisições simultâneas.

**Correção recomendada:** a animação deve receber apenas uma amostra limitada de candidatos ou dados sintéticos; a escolha real deve permanecer no servidor e retornar uma projection de um vencedor. Avaliar estratégia uniforme no PostgreSQL sem transferir a população inteira; comparar `ORDER BY random() LIMIT 1`, count + offset e estratégias com chave aleatória usando `EXPLAIN ANALYZE` no volume máximo.

**Como medir:** datasets de 1.000, 10.000 e 100.000 números; medir bytes JSON, heap/alocação, GC, query time, tempo de serialização e tempo total da tela.

**Complexidade da correção:** MEDIUM

### [PERF-07] Sorteios concorrentes não são serializados no banco

**Severidade:** MEDIUM<br>
**Status:** POTENCIAL<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/RaffleServiceImpl.java`; `backend/src/main/resources/db/migration/V1__create_raffle_schema.sql`; `backend/src/test/java/com/weddingraffle/rifa/service/impl/RaffleServiceImplTests.java`<br>
**Método / trecho:** `draw`/`drawNewWinner` (linhas 35-61); tabela `raffle_draw` (linhas 62-67); teste `drawCanCreateANewResultWhenAResultAlreadyExists` (linhas 67-92)

**Problema:** cada chamada carrega todos os candidatos e insere um novo resultado, sem lock, versionamento ou constraint que serialize duas chamadas. Duas instâncias podem executar todo o custo simultaneamente.

**Evidência:** `draw()` não consulta/fecha um estado de sorteio antes de salvar. A tabela aceita múltiplas linhas. O teste confirma que novo resultado é intencionalmente permitido, enquanto `docs/regras-de-negocio.md` ainda descreve o sorteio como idempotente sem uma ação explícita de reset.

**Impacto:** full scans duplicados, resultados concorrentes e comportamento não determinístico na tela pública. O risco de performance é amplificado exatamente no momento do evento.

**Cenário:** duplo clique, retry HTTP, duas abas administrativas ou duas instâncias processando simultaneamente.

**Correção recomendada:** primeiro confirmar a regra de negócio de ressorteio. Independentemente da decisão, serializar cada execução com estado/lock no banco e garantir que uma única ação lógica produza um resultado. Se ressorteio for permitido, usar comando/idempotency key explícito e versionar a rodada.

**Como medir:** teste concorrente com 2-50 chamadas de draw e PostgreSQL real; verificar número de resultados por comando, locks, deadlocks, query count e latência.

**Complexidade da correção:** MEDIUM

### [PERF-08] Respostas de números e PDFs são não paginadas e totalmente bufferizadas

**Severidade:** HIGH<br>
**Status:** CONFIRMADO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/LuckyNumberPdfServiceImpl.java`; `backend/src/main/java/com/weddingraffle/rifa/service/impl/TransactionServiceImpl.java`; `backend/src/main/java/com/weddingraffle/rifa/controller/TransactionController.java`; `backend/src/main/java/com/weddingraffle/rifa/controller/AdminTransactionController.java`<br>
**Método / trecho:** geração de PDF (linhas 56-159 e 269-305); status/recovery (linhas 187-218); respostas PDF (linhas 62-74 e 65-77)

**Problema:** status e recuperação retornam todos os números atuais/anteriores em listas. O PDF carrega todos os números, mantém todas as páginas no `PDDocument`, grava em `ByteArrayOutputStream`, copia com `toByteArray` e retorna outro `byte[]` pela camada HTTP. Tudo ocorre dentro de transação read-only. A listagem admin é paginada por transação, mas cada linha inclui todos os números daquela transação.

**Evidência:** repositories retornam `List<String>` sem paginação; controllers usam `ResponseEntity<byte[]>`; `PDDocument` e `ByteArrayOutputStream` vivem até o arquivo completo estar pronto. A quantidade por compra é não limitada por regra de negócio.

**Impacto:** múltiplas cópias em heap, GC, respostas grandes, conexão de banco retida durante CPU de PDF e risco de OOM/timeout para participantes com muitos números.

**Cenário:** uma compra ou telefone com 1.000-100.000 números; vários downloads/status simultâneos; instância com memória limitada.

**Correção recomendada:** separar resumo de listas paginadas no JSON; evitar enviar números completos na listagem admin até expansão; gerar PDF fora da transação e com estratégia de memória limitada/streaming ou geração assíncrona em storage compartilhado. Como não há limite máximo de compra, a solução deve lidar com volume alto sem truncar números.

**Como medir:** JFR/heap profiler e k6 para 1.000, 10.000 e 100.000 números; medir pico de heap, GC pause, bytes de resposta, duração da transação e tempo de download.

**Complexidade da correção:** HIGH

### [PERF-09] Agregações públicas são recalculadas por requisição e duplicadas na tela de ranking

**Severidade:** MEDIUM<br>
**Status:** DEPENDE DE MEDIÇÃO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/PublicHomeServiceImpl.java`; `backend/src/main/java/com/weddingraffle/rifa/repository/TransactionRepository.java`; `frontend/src/features/flag-ranking/FlagRankingPage.tsx`<br>
**Método / trecho:** `getSummary`/`findFlagRanking` (linhas 46-83); queries de soma/ranking (linhas 48-72); queries do front (linhas 13-20)

**Problema:** cada resumo executa ranking agrupado, soma global, leitura de configuração, leitura do último sorteio e eventualmente lookup do número vencedor. A tela top 30 dispara simultaneamente `home-summary` — que calcula top 5 — e `flag-ranking` — que calcula top 30 — duplicando ranking e soma na carga inicial. Não há cache HTTP/aplicacional.

**Evidência:** `findFlagRanking` sempre chama duas queries; `FlagRankingPage` usa dois `useQuery` independentes. Os endpoints não definem `Cache-Control` e não existe `@Cacheable`/cache distribuído.

**Impacto:** rajadas de acesso público podem repetir agregações e sorts sobre a mesma tabela mesmo quando nenhum pagamento mudou.

**Cenário:** divulgação do link, convidados atualizando ranking, 100-1.000 acessos em rajada, tabela com muitas transações.

**Correção recomendada:** medir primeiro. Se necessário, unificar o contrato da tela para evitar top 5 + top 30 duplicados; usar cache HTTP curto com invalidação/tolerância definida ou cache compartilhado/preagregação atualizado após mudança de status. Evitar cache apenas local se múltiplas instâncias precisarem de consistência.

**Como medir:** `EXPLAIN (ANALYZE, BUFFERS)` para ranking/soma com 1k/10k/100k transações; k6 em home/ranking; medir query latency, buffers, CPU do banco, cache hit e RPS.

**Complexidade da correção:** MEDIUM

### [PERF-10] Índices atuais não atendem diretamente busca substring e principais ordenações administrativas

**Severidade:** MEDIUM<br>
**Status:** DEPENDE DE MEDIÇÃO<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/repository/TransactionRepository.java`; `backend/src/main/resources/db/migration/V1__create_raffle_schema.sql`; `backend/src/main/resources/db/migration/V5__add_participant_and_payment_method_to_transaction.sql`; `backend/src/main/resources/db/migration/V13__reuse_recovery_code_by_phone.sql`<br>
**Método / trecho:** `findByNameOrPhone` (linhas 39-46); índices das migrations

**Problema:** a busca usa `lower(name) LIKE '%termo%'` e `phone LIKE '%dígitos%'`; os B-tree simples de `name` e os compostos de telefone não atendem diretamente substring com wildcard inicial nem a expressão `lower(name)`. A listagem padrão ordena por `createdAt`, mas não há índice em `created_at`. O índice `(phone, recovery_code)` filtra recuperação, mas não cobre o `ORDER BY created_at DESC`.

**Evidência:** migrations criam índices de `email`, `status`, `name`, `participant_flag_code`, `(phone, participant_flag_code)` e `(phone, recovery_code)`, mas nenhum índice funcional/trigram ou de `created_at`.

**Impacto:** possíveis sequential scans e sorts à medida que a tabela cresce; paginação com offsets altos também pode ficar mais cara.

**Cenário:** buscas administrativas por fragmento, ordenação por data/valor/nome/status, muitos registros ou páginas profundas.

**Correção recomendada:** não criar índice por suposição. Executar `EXPLAIN (ANALYZE, BUFFERS)` nas consultas reais. Se confirmado: considerar `pg_trgm` + GIN para busca substring normalizada; índice `created_at DESC` para a listagem predominante; incluir `created_at` após `(phone, recovery_code)` se a cardinalidade justificar; avaliar cursor/keyset pagination para páginas profundas. Cada índice deve ser justificado pela consulta e pelo custo de escrita.

**Como medir:** planos com dados representativos e valores seletivos/não seletivos; acompanhar scan type, rows removed, sort method/memory, buffers, p95 e impacto dos índices nos inserts.

**Complexidade da correção:** LOW a MEDIUM

### [PERF-11] Pool de conexões e pool HTTP não têm orçamento explícito por ambiente/instância

**Severidade:** HIGH<br>
**Status:** POTENCIAL<br>
**Arquivo:** `backend/src/main/resources/application.yml`; `backend/src/main/resources/application-prod.yml`; `backend/Dockerfile`<br>
**Método / trecho:** configuração de datasource/JPA (linhas 1-16); imagem/runtime (linhas 10-18)

**Problema:** não há configuração de `spring.datasource.hikari` (máximo, mínimo, acquisition timeout, max lifetime, leak detection) nem de threads/conexões/filas do Tomcat. Em escala horizontal, cada instância cria seu próprio pool e a soma pode exceder o limite do PostgreSQL/provedor. Chamadas externas bloqueantes agravam a diferença entre muitas threads HTTP e poucas conexões.

**Evidência:** os YAMLs contêm somente URL/credenciais e JPA; não há propriedades Hikari/Tomcat nem parâmetros por ambiente. Também não há health/readiness para impedir tráfego antes de banco/migrations estarem prontos.

**Impacto:** espera por conexão, rejeição pelo banco, filas longas, timeout em cascata e comportamento imprevisível ao aumentar instâncias.

**Cenário:** 2-10 instâncias; banco free tier com limite baixo; 200-1.000 requests concorrentes; Mercado Pago lento.

**Correção recomendada:** definir um orçamento: `instâncias máximas × maximumPoolSize <= conexões disponíveis - reserva operacional`; configurar acquisition timeout, max lifetime abaixo do limite do provedor, idle/minimum conforme cold start e métricas de leak; alinhar Tomcat max threads/queue ao throughput bloqueante. Avaliar pooler gerenciado/PgBouncer conforme provedor.

**Como medir:** Hikari active/idle/pending/timeout, threads busy, fila, conexões em `pg_stat_activity`, throughput e p95/p99 por número de instâncias.

**Complexidade da correção:** LOW a MEDIUM

### [PERF-12] Atribuição de bandeira/código repete queries e depende de read-check sem coordenação global

**Severidade:** MEDIUM<br>
**Status:** POTENCIAL<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/service/impl/RandomParticipantFlagService.java`; `backend/src/main/java/com/weddingraffle/rifa/service/impl/RandomRecoveryCodeService.java`; `backend/src/main/resources/db/migration/V9__add_participant_flag_to_transaction.sql`; `backend/src/main/resources/db/migration/V13__reuse_recovery_code_by_phone.sql`<br>
**Método / trecho:** `resolveForPhone`/`randomUnusedFlag` (linhas 28-48); `resolveForPhone`/`generateUniqueCode` (linhas 23-37)

**Problema:** cada criação consulta a primeira transação do telefone duas vezes, uma para bandeira e outra para recovery code. Para telefone novo, a bandeira faz `SELECT DISTINCT` de todos os códigos usados e o recovery code pode fazer várias queries de existência. Não há tabela de participante com unicidade `phone -> flag/code`; V13 remove a unicidade global do recovery code.

**Evidência:** os dois services chamam `findFirstByPhoneOrderByCreatedAtAsc` independentemente. A migration de bandeira cria apenas índices não únicos; a de recovery mantém índice `(phone, recovery_code)`, sem garantir que telefones diferentes não compartilhem o mesmo código ou que compras concorrentes do mesmo telefone recebam a mesma atribuição.

**Impacto:** leituras repetidas no checkout e inconsistência entre instâncias. Corridas podem atribuir a mesma bandeira a telefones diferentes ou atribuições diferentes ao mesmo telefone, além de multiplicar recomputações.

**Cenário:** primeiros checkouts de telefones novos concorrendo em 2+ instâncias; 50-1.000 criações simultâneas.

**Correção recomendada:** modelar atribuição por participante/telefone em tabela própria, com constraints únicas adequadas e insert/claim atômico; buscar bandeira e código uma vez; selecionar bandeira livre no banco com lock curto. A migração exige cuidado para consolidar dados existentes.

**Como medir:** teste concorrente com telefones iguais e distintos; verificar invariantes finais, quantidade de queries por checkout, lock waits e throughput.

**Complexidade da correção:** HIGH

### [PERF-13] Bundle público incorpora 173 SVGs e carrega páginas secundárias de forma eager

**Severidade:** MEDIUM<br>
**Status:** DEPENDE DE MEDIÇÃO<br>
**Arquivo:** `frontend/src/components/ui/FlagEmoji.tsx`; `frontend/src/App.tsx`<br>
**Método / trecho:** `import.meta.glob(..., { eager: true })` (linhas 3-202); imports de rotas (linhas 1-8)

**Problema:** todas as 195 bandeiras são registradas eager. O build embute 173 SVGs como data URI no JavaScript público e emite 22 assets. Apenas a área admin é lazy; pagamento, recuperação e top 30 entram no chunk inicial mesmo quando a home é acessada.

**Evidência:** build local: `index` 469,08 kB (143,64 kB gzip), admin 38,77 kB (9,99 kB gzip), 173 ocorrências `data:image/svg` no chunk público.

**Impacto:** download, descompressão, parse e compilação de JavaScript desnecessários, especialmente em celular. O tamanho isolado não prova degradação perceptível; precisa de medição de campo/lab.

**Cenário:** primeira visita em rede móvel, aparelho de CPU lenta, cache frio.

**Correção recomendada:** evitar glob eager/inlining de todas as bandeiras; carregar asset por código sob demanda, usar assets externos com cache ou emoji nativo quando aceitável. Aplicar lazy loading a rotas públicas secundárias. Comparar bundle e UX antes/depois.

**Como medir:** Lighthouse/WebPageTest em Slow 4G e CPU throttling; medir transfer size, parse/eval, LCP, INP, TTI e code coverage; usar bundle analyzer.

**Complexidade da correção:** MEDIUM

### [PERF-14] Home mantém dois timers de 1 segundo e cotação dispara por cada alteração de quantidade

**Severidade:** LOW<br>
**Status:** DEPENDE DE MEDIÇÃO<br>
**Arquivo:** `frontend/src/features/buy-numbers/BuyNumbersPage.tsx`; `frontend/src/features/buy-numbers/CountdownPanel.tsx`<br>
**Método / trecho:** effects (linhas 51-56 e 16-21); `quoteQuery` (linhas 45-49)

**Problema:** a home cria um timer no componente pai para recalcular fechamento e outro no contador. O timer do pai rerenderiza toda a página a cada segundo. Cada clique em aumentar/diminuir quantidade cria uma nova chave de cotação e nova chamada, sem debounce/cancelamento explícito.

**Evidência:** os dois `setInterval` são independentes. `queryKey` inclui `quantity` e o `queryFn` chama a API.

**Impacto:** renders e requests extras; provavelmente pequeno no volume atual, mas pode aparecer em celulares lentos ou cliques rápidos.

**Cenário:** contador ativo por horas, usuário mantendo a página aberta ou alterando quantidade rapidamente.

**Correção recomendada:** manter uma única fonte de relógio e isolar o rerender no menor componente; agendar apenas a transição de encerramento no pai. Debouncear a cotação ou cancelar requests obsoletos via `AbortSignal`, sem deixar o front ser fonte do valor final.

**Como medir:** React Profiler, contador de renders/requests e teste com clique rápido; comparar CPU/INP.

**Complexidade da correção:** LOW

### [PERF-15] Não há observabilidade nem testes de carga/concorrência para sustentar capacidade

**Severidade:** INFO<br>
**Status:** CONFIRMADO<br>
**Arquivo:** `backend/pom.xml`; `backend/src/main/resources/application*.yml`; `backend/src/test`; `frontend/src`<br>
**Método / trecho:** dependências/configuração e suítes de teste

**Problema:** não há Spring Boot Actuator, Micrometer/Prometheus, métricas Hikari/Tomcat, tracing, `pg_stat_statements` documentado, k6/JMeter, benchmark ou teste concorrente. Os testes de idempotência são sequenciais com mocks.

**Evidência:** busca no repositório não encontrou configuração/dependência correspondente. As suítes passam, mas não exercitam duas transações reais simultâneas.

**Impacto:** não é possível definir SLO, detectar saturação ou provar suporte a 10-1.000 usuários. Regressões de query count, heap e locks podem chegar à produção sem sinal.

**Cenário:** homologação, dia do evento, autoscaling ou incidente do provedor.

**Correção recomendada:** instrumentar timers por endpoint/provedor/query, Hikari, Tomcat, JVM/GC, erros e tamanho de resposta; habilitar health/readiness; criar scripts k6 e testes concorrentes PostgreSQL. Definir alertas sem registrar secrets/dados completos de pagamento.

**Como medir:** throughput, p50/p95/p99, error rate, CPU, heap, GC, threads, pool, query latency, locks/deadlocks, latência/retries do Mercado Pago e bytes de resposta.

**Complexidade da correção:** MEDIUM

### [PERF-16] Endpoints públicos caros não têm rate limit, quota ou backpressure

**Severidade:** HIGH<br>
**Status:** POTENCIAL<br>
**Arquivo:** `backend/src/main/java/com/weddingraffle/rifa/config/SecurityConfig.java`; `backend/pom.xml`; `backend/src/main/resources/application.yml`<br>
**Método / trecho:** endpoints públicos (linhas 49-68)

**Problema:** cotação, criação, recuperação, status, PDF, resumo, ranking e webhook são públicos conforme necessário, mas não há rate limiter, limite de concorrência, fila/bulkhead ou proteção distribuída. Alguns acionam Mercado Pago, agregações, PDF ou listas grandes.

**Evidência:** nenhuma dependência/configuração de rate limiting; SecurityConfig apenas permite/bloqueia. O botão do front evita duplo clique, mas não protege chamadas diretas, retries de rede ou múltiplas instâncias.

**Impacto:** tráfego legítimo em rajada ou abuso pode saturar threads, pool, banco, CPU/heap e cota externa; uma réplica adicional não resolve se todas atingirem o mesmo banco/provedor.

**Cenário:** 100-1.000 clientes, bots, refresh de PDFs/status, replay de webhook ou falha externa gerando retries.

**Correção recomendada:** aplicar quotas e limites por endpoint/custo no gateway/CDN ou mecanismo distribuído; bulkhead separado para Mercado Pago/PDF; `429` com `Retry-After`; limites de concorrência e fila curta. Webhook precisa política própria que não rejeite eventos válidos indiscriminadamente. Rate limit local isolado não é suficiente para escala horizontal.

**Como medir:** carga em rajada e sustentada, verificando fila, rejeições controladas, error rate, recuperação após sobrecarga e ausência de starvation entre endpoints.

**Complexidade da correção:** MEDIUM

## 5. Revisão de queries e índices

| Fluxo/query | Estrutura atual | Avaliação | Validação recomendada |
|---|---|---|---|
| `existsByNumber` por candidato | Unique index em `lucky_number(number)` | A lookup isolada é indexada, mas o problema é a quantidade de round trips | Contar statements e tempo acumulado por compra |
| Números elegíveis para sorteio | Índice em `transaction(status)`, FK indexada e unique de número | Retorna e ordena toda a população; plano exato depende da cardinalidade | `EXPLAIN (ANALYZE, BUFFERS)` com 1k/10k/100k elegíveis |
| Busca admin por nome/telefone | B-tree em `name`; índices compostos iniciando por `phone` | Wildcard inicial e `lower(name)` podem impedir uso direto dos índices | Planos com termos seletivos e genéricos; avaliar `pg_trgm` |
| Listagem `ORDER BY created_at` | Sem índice de `created_at` | Pode exigir sort/scan; impacto depende do tamanho | Plano das primeiras páginas e páginas profundas |
| Recuperação `(phone,recovery_code) ORDER BY created_at` | Índice `(phone,recovery_code)` | Filtro coberto; ordenação não coberta | Plano para telefones com muitas compras |
| Ranking por status/bandeira | Índices de status e bandeira separados | Aggregate/group/order pode varrer todo o subconjunto aprovado | Buffers, CPU e sort memory em volume representativo |
| Resumo admin | Aggregate de toda `transaction` | Full scan pode ser a escolha correta; não pressupor índice | Medir latência e frequência; cache/preagregação só se necessário |

Não foi confirmado nenhum índice adicional como obrigatório sem `EXPLAIN ANALYZE` e dados representativos.

## 6. Concorrência e escala horizontal

### Estado atual

A aplicação é majoritariamente stateless e **pode ser iniciada em múltiplas instâncias** sem sticky session: autenticação é JWT, dados ficam no PostgreSQL e não há arquivo/sessão/cache crítico local. Isso é uma base favorável, mas não comprova que os fluxos críticos sejam seguros ou que o banco suporte a soma de conexões.

Bloqueios para escala horizontal segura:

- geração após pagamento não possui claim/lock atômico;
- sorteio não é serializado por rodada/comando;
- atribuição de bandeira/recovery code depende de consultas e decisão na aplicação;
- cada instância multiplica pool de banco e threads;
- não há rate limit/cache distribuído nem métricas compartilhadas;
- PDFs e coleções completas aumentam heap de cada réplica e tráfego do banco.

### Cenários obrigatórios de carga

A tabela abaixo **não é uma estimativa de capacidade**. Ela mostra a pressão teórica do polling de 5 segundos caso todos estejam pendentes e os ensaios que devem ser feitos.

| Usuários simultâneos | Polls de status/s a cada 5 s | Foco do ensaio |
|---:|---:|---|
| 10 | 2 | baseline, query count e duração transacional |
| 50 | 10 | latência externa, pool e geração concorrente |
| 100 | 20 | p95/p99, GC e retries |
| 200 | 40 | filas Tomcat/Hikari e rate limit |
| 500 | 100 | saturação controlada e recuperação |
| 1.000 | 200 | limite real de API/banco/provedor; somente ambiente de carga |

Ensaios adicionais necessários:

1. Mesmo webhook: 2, 10, 50 e 100 entregas simultâneas.
2. Pagamentos distintos: 10-1.000 aprovações, com quantidade 1/10/100/1.000.
3. Ocupação do intervalo: 0%, 50%, 90% e 99%.
4. Mercado Pago: 100 ms/1 s/10 s/timeout; 4xx/429/5xx.
5. Home/ranking: rajada e carga sustentada.
6. Sorteio e PDF: 1k/10k/100k números.
7. Uma, duas e mais instâncias, mantendo orçamento total de conexões constante.

## 7. Plano de medição recomendado

### Ferramentas

- k6 ou JMeter para fluxos HTTP e cenários concorrentes.
- Spring Boot Actuator + Micrometer para JVM, HTTP, Tomcat e Hikari.
- Java Flight Recorder para CPU, alocação, bloqueios e GC.
- PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)`, `pg_stat_statements`, `pg_stat_activity` e locks.
- Lighthouse/WebPageTest e React Profiler para front-end.

### Métricas mínimas

- throughput, p50, p95, p99 e error rate por endpoint;
- CPU, heap, alocação, GC pause e threads;
- Hikari active/idle/pending, acquisition timeout e uso máximo;
- query count/request, query latency, buffers, locks, deadlocks e rollbacks;
- latência, timeout, retry e circuit state do Mercado Pago;
- bytes de request/response e tamanho/tempo de PDF;
- bundle transferido, parse/eval, LCP e INP.

Critérios de aceitação devem ser definidos antes do teste conforme infraestrutura e experiência desejada. O repositório, por si só, não demonstra suporte a qualquer nível de concorrência.

## 8. Priorização

Priorização considerando severidade × probabilidade × impacto × esforço:

| Prioridade | Achados | Justificativa |
|---|---|---|
| P0 | PERF-01, PERF-02, PERF-05 | Estão no caminho dinheiro -> números; combinam custo alto, transação longa e risco concorrente |
| P0 | PERF-04 | Pode multiplicar automaticamente carga interna e externa enquanto pagamentos permanecem pendentes |
| P1 | PERF-11 | Configuração/medição de pool tem esforço relativamente baixo e reduz risco antes de escalar instâncias |
| P1 | PERF-16 | Backpressure protege banco e provedor dos demais caminhos caros |
| P1 | PERF-06, PERF-08 | Coleções/PDF completos têm impacto alto no dia do evento ou em compras grandes |
| P2 | PERF-03, PERF-07, PERF-09, PERF-10, PERF-12 | Importantes, mas exigem fault injection, decisão de negócio ou plano de query para fechar a solução |
| P3 | PERF-13, PERF-14, PERF-15 | Otimização/observabilidade; PERF-15 deve anteceder afirmações de capacidade |

## 9. Contagem final

| Severidade | Quantidade |
|---|---:|
| CRITICAL | 0 |
| HIGH | 9 |
| MEDIUM | 5 |
| LOW | 1 |
| INFO | 1 |
| **Total** | **16** |

Distribuição por status:

| Status | Quantidade |
|---|---:|
| CONFIRMADO | 5 |
| POTENCIAL | 6 |
| DEPENDE DE MEDIÇÃO | 5 |

Nenhuma correção foi implementada. Cada correção deve ser tratada em branch/PR próprio, agrupando apenas achados tecnicamente relacionados e mantendo separadas mudanças de banco, integração, front-end e infraestrutura quando possível.
