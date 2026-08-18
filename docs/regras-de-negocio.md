# Regras de Negócio — "Presente Premiado" (Rifa de Casamento)

> Nome do projeto: **Presente Premiado**
> Stack: Back-end Java/Spring Boot (API REST) + Banco SQL | Front-end React
> Integração de pagamento: Mercado Pago (Checkout Pro) + registro manual de pagamento em dinheiro (admin)

---

## 1. Visão Geral

O app permite que convidados contribuam para um sorteio realizado no dia do casamento, recebendo "números da sorte" por contribuição. Existem dois fluxos de contribuição:

**A) Fluxo online (auto-atendimento, pagamento via Mercado Pago)**
1. Usuário informa nome e telefone (obrigatórios) e, opcionalmente, e-mail.
2. Usuário escolhe a quantidade de números que deseja.
3. Sistema calcula o valor total (quantidade × valor unitário configurável).
4. Usuário é redirecionado ao checkout do Mercado Pago.
5. Após pagamento aprovado, o sistema gera os números aleatórios (únicos, dentro de um intervalo fixo), salva no banco vinculados à transação, e:
   - Se e-mail foi informado: envia e-mail de confirmação com os números.
   - Se e-mail não foi informado: exibe destaque na tela para o usuário baixar um **PDF** com os números (única forma de guardá-los).
6. Caso o pagamento falhe, é exibida mensagem de erro e nenhum número é gerado.

**B) Fluxo em dinheiro (somente admin, presencial no dia da festa)**
1. O admin, logado no painel administrativo, acessa a tela "Registrar pagamento em dinheiro".
2. Preenche nome e telefone (obrigatórios) e, opcionalmente, e-mail do convidado que está pagando em espécie.
3. Informa a quantidade de números.
4. O pagamento é considerado confirmado imediatamente (sem Mercado Pago) e os números são gerados na hora.
5. O admin pode baixar/imprimir o PDF com os números para entregar ao convidado.

Existe também uma área administrativa (login restrito) para realizar o **sorteio**, sorteando apenas entre os números efetivamente gerados/pagos (online ou em dinheiro). O sorteio possui uma **tela dedicada, separada do painel administrativo**, pensada para ser projetada em telão no dia do evento — essa tela nunca exibe informações de arrecadação/vendas.

Não existe cadastro de usuário comum. O único login do sistema é o do **admin**, usado para acessar o registro de pagamentos em dinheiro, a listagem de transações e a funcionalidade de sorteio.

---

## 2. Decisões já definidas

| Tema | Decisão |
|---|---|
| Geração dos números | Aleatórios, dentro de um intervalo fixo (ex: `00000` a `99999`), **sem repetição entre todos os compradores** (unicidade global no banco) |
| Compra repetida pelo mesmo e-mail | Permitida — cada nova compra soma novos números ao mesmo e-mail (várias transações independentes) |
| Login do admin | Usuário e senha armazenados no banco (hash de senha), criados manualmente via seed/script — sem tela de cadastro |
| Valor unitário do número | Definido via variável de ambiente (não fixo no código, não em tabela de configuração no banco) |
| Intervalo dos números | Definido via variável de ambiente (min/max) |
| Limite de números por compra | Não haverá limite máximo por transação |
| Vencedores do sorteio | Apenas 1 vencedor por sorteio |
| Exibição pública de vendas | Não haverá exibição pública de quantos números foram vendidos/quanto falta arrecadar |
| Campos obrigatórios do contribuinte | Nome e telefone são obrigatórios; e-mail passa a ser **opcional** |
| Guarda dos números sem e-mail | Quando o e-mail não é informado, o usuário deve baixar um **PDF** com os números — não há outra forma de recuperá-los depois |
| Pagamento em dinheiro | Só pode ser registrado pelo **admin**, via tela própria no painel administrativo; ao ser registrado, é considerado aprovado imediatamente (não passa pelo Mercado Pago) |
| Tela de sorteio | Tela separada do painel administrativo, acessada a partir dele, própria para exibição em telão — nunca mostra dados de arrecadação/vendas |
| Referência da transação | Toda transação, incluindo pagamento em dinheiro, deve ter um `external_reference` UUID não sequencial, usado para consulta e download de PDF |
| Telefone | O telefone aceita formatos comuns brasileiros, mas deve ser normalizado para 10 ou 11 dígitos antes de ser salvo |
| Valor em dinheiro | Transações em dinheiro registram `valor_total = quantidade × valor_unitário` vigente |
| Valor unitário do número (revisado) | Deixa de ser fixo por variável de ambiente. Passa a ser configurável pelo admin em tempo de execução (tabela `raffle_config` no banco). `RAFFLE_UNIT_PRICE` (env) passa a ser usado apenas como **seed inicial** (primeira carga, via migration) — depois disso, o valor vigente é sempre lido do banco |
| Preço gravado na transação | Toda transação (`MERCADO_PAGO` ou `CASH`) grava o `unit_price` vigente no momento em que foi criada. Alterações futuras de preço não afetam transações já existentes — cada uma mantém o preço com que nasceu, inclusive para fins de auditoria em transações aprovadas. Não há histórico de quem/quando alterou o preço |
| Data/hora do sorteio | Configurável pelo admin (data + hora, fuso `America/Sao_Paulo`). Usada exclusivamente para exibir contagem regressiva pública na tela inicial — não bloqueia compras nem dispara o sorteio automaticamente. O sorteio continua 100% manual, disparado pelo admin |
| Rank público de maiores compradores | Exibido na tela inicial, **top 5**, agrupado por telefone único, ordenado pela soma de `quantity` das transações `APPROVED` daquele telefone. Nunca exibe nome, telefone, e-mail ou valor — apenas um avatar anônimo (emoji + cor) e a quantidade total de números. Sem paginação/"ver mais" |
| Bandeira por telefone | Na primeira compra de um telefone, o sistema atribui automaticamente uma bandeira de país ainda não usada por outro telefone. Novas compras com o mesmo telefone reutilizam a mesma bandeira. |
| Ranking público de bandeiras | Exibido na tela inicial ao lado do formulário de compra. Soma a quantidade de números de transações `APPROVED` por bandeira. A bandeira em primeiro lugar também ganha um prêmio especial no dia do sorteio. |
| Estorno/chargeback/mediação após aprovação | Se uma transação online aprovada for depois estornada, sofrer chargeback ou entrar em mediação antes do sorteio, os números já gerados permanecem no histórico, mas deixam de ser elegíveis enquanto o status não voltar a `APPROVED`. |

---

## 3. Regras de Negócio — Front-end (React)

### 3.1 Fluxo de compra
- RN-F01: A primeira tela deve solicitar **nome** (obrigatório) e **telefone** (obrigatório), além de **e-mail** (opcional, claramente identificado como opcional). Validar formato de telefone e, se preenchido, formato de e-mail, antes de permitir avançar.
- RN-F01.1: Deve haver um texto de apoio próximo ao campo de e-mail explicando a consequência de não preenchê-lo (ex: "Informe seu e-mail para receber os números automaticamente, ou deixe em branco e baixe um PDF ao final").
- RN-F02: Após os campos obrigatórios válidos, exibir seletor de quantidade de números desejados (input numérico, com valor mínimo de 1, sem limite máximo).
- RN-F03: O valor total deve ser calculado e exibido em tempo real: `quantidade × valor_unitário`. O valor unitário deve vir de uma configuração consultada via API (não fixo no front-end).
- RN-F04: Abaixo do valor total, exibir botão "Pagar" (ou similar) que, ao ser clicado, aciona o back-end para criar a preferência de pagamento no Mercado Pago e redireciona o usuário para a URL de checkout retornada.
- RN-F05: O botão de pagamento deve ficar desabilitado enquanto a requisição ao back-end está em andamento (evitar duplo clique / dupla criação de preferência).

### 3.2 Retorno do pagamento
- RN-F06: Após o pagamento, o Mercado Pago deve redirecionar de volta para o app (rotas de retorno: sucesso, falha, pendente).
- RN-F07: Na tela de retorno, o front-end deve consultar o back-end (via ID da transação/preferência recebido por query param) para confirmar o status real do pagamento — **nunca confiar apenas no status vindo por parâmetro de URL**.
- RN-F08: Se o pagamento foi aprovado e os números foram gerados:
  - Exibir mensagem de sucesso, agradecendo o presente e desejando boa sorte no sorteio.
  - Exibir os números da sorte gerados na tela.
  - **Se e-mail foi informado**: informar que os números também foram enviados por e-mail.
  - **Se e-mail não foi informado**: exibir um aviso em destaque explicando que essa é a única forma de guardar os números, com um botão bem visível **"Baixar PDF"**, que gera/baixa um PDF com os números.
- RN-F09: Se o pagamento falhou, foi cancelado ou está pendente:
  - Exibir mensagem de erro/pendência clara, sem gerar números.
  - Oferecer opção de tentar novamente.
- RN-F10: Se o pagamento estiver "pendente" (ex: boleto), o front deve informar que os números serão gerados assim que a confirmação for recebida, e que o e-mail será enviado quando isso ocorrer (não travar o usuário esperando).

### 3.3 Área administrativa
- RN-F11: Deve existir uma rota de login exclusiva para o admin (não visível/linkada na navegação pública do site).
- RN-F12: Usuários que não sejam admin nunca devem ver opção de login ou qualquer link para a área administrativa.
- RN-F13: Após login, o admin acessa um painel com navegação para quatro áreas: **Listagem de transações**, **Registrar pagamento em dinheiro**, **Sorteio** e **Configurações da rifa** (preço unitário e data/hora do sorteio).
  - Listagem/consulta dos números gerados (com filtro por e-mail ou nome, opcional), exibindo também a **data/hora da compra** (`createdAt`) de cada transação.
- RN-F14: Rotas administrativas devem ser protegidas no front-end (redirecionar para login caso não autenticado) — mas a proteção real deve estar no back-end (o front-end é só UX).
- RN-F17: **Tela "Registrar pagamento em dinheiro"** (admin): formulário com nome (obrigatório), telefone (obrigatório), e-mail (opcional) e quantidade de números. Ao confirmar, os números são gerados imediatamente (sem Mercado Pago) e exibidos na tela, com botão para baixar/imprimir o PDF na hora, para entrega ao convidado.
- RN-F18: **Tela de Sorteio** (admin): tela separada/independente do restante do painel (rota própria), pensada para ser projetada em telão durante a festa.
  - Não deve exibir nenhuma informação de arrecadação, quantidade de números vendidos, valores ou listagem de transações.
  - Deve conter apenas: um botão para realizar o sorteio (com confirmação, já que a ação não pode ser refeita) e, após a execução, a exibição do número e do nome do vencedor em destaque visual grande, adequado para leitura à distância.

### 3.4 Tela inicial pública (contador e rank)
- RN-F19: A tela inicial deve exibir um **contador regressivo** até a data/hora do sorteio, consultando um endpoint público. Se a data do sorteio ainda não tiver sido configurada pelo admin, o contador simplesmente não é exibido (sem mensagem de erro visível ao usuário).
- RN-F20: A tela inicial deve exibir um **rank público dos 5 maiores compradores** (por quantidade de números, ver RN-B33), mostrando apenas avatar anônimo (emoji + cor) e a quantidade — nunca nome, telefone, e-mail ou valor.
- RN-F20.1: A tela inicial deve exibir, ao lado do formulário de compra em telas maiores, um **ranking público das bandeiras**, somando a quantidade de números de transações `APPROVED` por bandeira.
- RN-F20.2: A tabela do ranking de bandeiras deve conter uma descrição explicando que cada telefone recebe uma bandeira exclusiva automaticamente na primeira compra e que novas compras do mesmo telefone acumulam para a mesma bandeira.
- RN-F20.3: A tela deve exibir o aviso: "A bandeira em primeiro lugar também ganhará um prêmio especial no dia do sorteio."
- RN-F21: O painel admin deve ter uma tela de **Configurações da rifa**, permitindo editar o preço unitário vigente (RN-B02.1) e a data/hora do sorteio (RN-B32).

### 3.5 Geral
- RN-F15: Nenhuma chave, token, credencial ou URL sensível do Mercado Pago deve estar no código do front-end. Apenas a `public key` (se necessária) pode ser exposta, e mesmo essa deve vir de variável de ambiente de build.
- RN-F16: Mensagens de erro devem ser amigáveis e não expor detalhes técnicos/stack trace ao usuário final.

---

## 4. Regras de Negócio — Back-end (Java / Spring Boot)

### 4.1 Configuração e segurança
- RN-B01: Nenhuma informação sensível (credenciais do Mercado Pago, credenciais de banco, JWT secret, SMTP, etc.) pode estar hardcoded no código-fonte. Tudo deve vir de variáveis de ambiente (`application.yml` referenciando `${VAR}` + `.env`/secrets do ambiente de deploy).
- RN-B02 (revisado): O valor unitário de cada número (ex: R$ 10,00) deixa de ser fixo por variável de ambiente. Passa a ser lido de uma tabela `raffle_config` no banco (linha única), editável pelo admin em tempo de execução. A variável de ambiente `RAFFLE_UNIT_PRICE` é usada **apenas como seed inicial**, aplicado uma única vez via migration — depois da primeira carga, ela deixa de ser consultada em runtime.
- RN-B02.1: Endpoint protegido (somente admin) `PUT /admin/raffle-config/unit-price` para atualizar o preço unitário vigente. A alteração tem efeito imediato sobre novas cotações/transações; não há histórico de quem alterou ou quando.
- RN-B02.2: Toda transação (`MERCADO_PAGO` ou `CASH`) deve gravar o campo `unit_price` com o valor vigente no momento em que foi criada. `total_amount` continua sendo `quantity × unit_price` **da própria transação**, nunca recalculado a partir do preço vigente no momento da consulta. Transações `PENDING` mantêm o preço com que nasceram mesmo que o admin altere o preço depois; transações `APPROVED` preservam esse valor para fins de auditoria/histórico no admin.
- RN-B03: O intervalo de números possíveis (ex: `00000` a `99999`) também deve ser configurável via variável de ambiente (min/max) — isso não muda.
- RN-B03.1: Não há limite máximo de números por compra — o usuário pode adquirir quantos quiser em uma mesma transação.

### 4.2 Autenticação (somente admin)
- RN-B04: Deve existir apenas um tipo de usuário: **admin**. Não há endpoint de cadastro público (`/register` não deve existir ou deve estar bloqueado).
- RN-B05: Usuários admin são criados via seed/script de banco (ex: migration do Flyway/Liquibase), nunca via endpoint exposto.
- RN-B06: Senha do admin deve ser armazenada com hash (ex: BCrypt), nunca em texto plano.
- RN-B07: Autenticação deve gerar um token (ex: JWT) com expiração configurável. Endpoints administrativos (sorteio, listagem de números) devem exigir esse token válido.
- RN-B08: Tentativas de acesso a endpoints administrativos sem token válido devem retornar `401/403`.

### 4.3 Fluxo de compra de números
- RN-B09: Endpoint para validar nome, telefone (obrigatórios), e-mail (opcional) e quantidade desejada, retornando o valor total calculado (`quantidade × valor_unitário`), sem ainda gerar números nem cobrar.
- RN-B10: Endpoint para criar a **preferência de pagamento** no Mercado Pago (Checkout Pro), enviando:
  - Descrição do item ("Número(s) da sorte — Presente Premiado").
  - Quantidade e valor unitário.
  - Dados do pagador (nome e, se disponível, e-mail), conforme suportado pela API.
  - URLs de retorno (`success`, `failure`, `pending`) apontando para o front-end.
  - URL de notificação (webhook) para o back-end receber atualizações assíncronas de status.
  - Um identificador externo (`external_reference`) UUID não sequencial representando a transação interna, para casar o retorno do Mercado Pago com o registro criado no banco.
- RN-B11: Ao criar a preferência, o back-end deve salvar no banco um registro de **transação/pedido** com status inicial `PENDENTE`, contendo: nome, telefone, e-mail (pode ser nulo), quantidade solicitada, valor total, `payment_method = MERCADO_PAGO`, `external_reference`, id da preferência do Mercado Pago, timestamps.
- RN-B11.1: Toda transação deve registrar a bandeira atribuída ao telefone. Se já existir transação anterior para o mesmo telefone, reutilizar a mesma bandeira; se for a primeira compra daquele telefone, sortear automaticamente uma bandeira de país ainda não usada por outro telefone.
- RN-B11.2: A lista de bandeiras disponíveis deve ficar centralizada em arquivo de recurso/configuração da aplicação, não instanciada manualmente uma a uma no serviço de atribuição.
- RN-B12: O back-end deve possuir um endpoint de **webhook** (notificação do Mercado Pago) que:
  - Recebe a notificação de mudança de status de pagamento.
  - Consulta a API do Mercado Pago para confirmar o status real do pagamento (nunca confiar apenas no payload recebido, por segurança — sempre validar buscando o pagamento pelo ID informado).
  - Atualiza o status da transação no banco (`APROVADO`, `RECUSADO`, `PENDENTE`, `CANCELADO`, etc., mapeando os status do Mercado Pago).
- RN-B13: Quando (e somente quando) o status do pagamento for confirmado como **aprovado**:
  - Gerar a quantidade de números contratados, de forma aleatória, dentro do intervalo configurado.
  - Garantir unicidade: nenhum número já existente no banco (de qualquer outra transação aprovada) pode se repetir. Se houver colisão, gerar novamente até obter um número livre.
  - Persistir cada número gerado vinculado à transação e ao e-mail do comprador.
  - Atualizar status da transação para `CONCLUÍDA`/`APROVADA`.
  - Disparar o envio de e-mail para o comprador com a lista de números gerados.
- RN-B14: Se o pagamento for recusado/cancelado, nenhum número deve ser gerado; a transação deve ficar marcada como `RECUSADA`/`CANCELADA`.
- RN-B14.1: Se um pagamento já aprovado for posteriormente estornado, sofrer chargeback ou entrar em mediação, a transação deve refletir esse novo status e seus números não devem participar do sorteio enquanto a transação não estiver `APPROVED`. Os números já gerados não devem ser apagados nem regerados.
- RN-B15: O endpoint que o front-end consulta na tela de retorno (RN-F07) deve buscar o status atual da transação pelo `external_reference` (ou id da transação) e retornar: status do pagamento, números gerados (se houver) e mensagem apropriada. Esse endpoint não deve, ele mesmo, decidir aprovação — apenas refletir o que o webhook (fonte da verdade) já processou. Caso o webhook ainda não tenha chegado, pode fazer uma consulta ativa ao Mercado Pago como fallback.
- RN-B16: Compras repetidas do mesmo e-mail devem ser tratadas como transações independentes; todos os números de todas as transações aprovadas daquele e-mail concorrem ao sorteio.

### 4.4 Pagamento em dinheiro (registro manual pelo admin)
- RN-B26: Endpoint protegido (somente admin) para registrar uma doação em dinheiro: recebe nome e telefone (obrigatórios) e e-mail (opcional), além da quantidade de números.
- RN-B27: Ao registrar, a transação deve ser criada diretamente com `payment_method = CASH`, `external_reference` UUID não sequencial e status já `APROVADA` — não há integração com o Mercado Pago nesse fluxo, e nenhum webhook é esperado.
- RN-B27.1: Transações em dinheiro devem registrar `valor_total` como `quantidade × valor_unitário` vigente no momento do registro.
- RN-B28: A geração dos números deve seguir exatamente a mesma lógica de unicidade usada no fluxo online (RN-B13), dentro de uma transação atômica.
- RN-B29: O e-mail só é disparado se o campo e-mail estiver preenchido (ver RN-B21.1). O PDF (RN-B30) deve estar disponível de qualquer forma, para o admin entregar ao convidado presencialmente.

### 4.5 Geração de PDF dos números
- RN-B30: Endpoint para gerar/baixar um PDF contendo os números da sorte de uma transação específica (usado tanto pelo fluxo online sem e-mail quanto pelo fluxo de pagamento em dinheiro do admin).
- RN-B31: O PDF deve conter, no mínimo: nome do contribuinte, os números gerados, e uma mensagem de agradecimento/boa sorte, mantendo consistência com o conteúdo do e-mail (RN-B21).

### 4.4 Sorteio
- RN-B17: Endpoint (protegido, somente admin) para realizar o sorteio:
  - Considera **apenas** números vinculados a transações com status `APROVADA/CONCLUÍDA`.
  - Sorteia **um único número vencedor** aleatoriamente dentre os números elegíveis persistidos no banco (não gera um número novo — sorteia entre os existentes). Haverá apenas 1 vencedor por sorteio.
  - Retorna o número sorteado e o nome do vencedor associado a ele.
  - Persiste o resultado do sorteio (para evitar sorteios duplicados/perdidos e permitir consulta posterior).
- RN-B18: Deve ser possível consultar o resultado do sorteio já realizado sem sortear novamente (idempotência — um sorteio já realizado não deve ser refeito, a menos que exista uma ação explícita de "resetar sorteio", se isso vier a ser necessário).
- RN-B19: Endpoint (admin) para listar todas as transações/números gerados, com filtro opcional por e-mail ou nome — usado para conferência.
- RN-B19.2: A listagem de transações do admin (RN-B19) deve incluir a data/hora de criação (`createdAt`) de cada transação.
- RN-B19.1: O endpoint de sorteio (RN-B17) e o de consulta de resultado (RN-B18) devem retornar **apenas** número e nome do vencedor — nunca dados de arrecadação, listagem de transações ou quantidade vendida — já que alimentam a tela dedicada de sorteio, projetada publicamente (ver RN-F18).

### 4.5 E-mails
- RN-B20: Envio de e-mail deve ocorrer de forma assíncrona (não bloquear a resposta HTTP da confirmação de pagamento).
- RN-B21: O e-mail deve conter: agradecimento pela contribuição/presente, lista dos números da sorte gerados, e mensagem de boa sorte no sorteio.
- RN-B21.1: O envio de e-mail só deve ser disparado se o campo e-mail estiver preenchido na transação. Ausência de e-mail não é um erro — o fluxo deve seguir normalmente sem tentativa de envio.
- RN-B22: Configuração de SMTP (host, porta, usuário, senha, remetente) deve vir de variáveis de ambiente.
- RN-B23: Falha no envio de e-mail não deve reverter a geração dos números (o número já foi gerado e pago); deve haver log de erro para reenvio manual, se necessário.

### 4.6 Configuração da rifa, contador e rank público
- RN-B32: Endpoint protegido (somente admin) `PUT /admin/raffle-config/scheduled-at` para definir/editar a data/hora do sorteio (`scheduled_draw_at`), armazenada em UTC mas interpretada/exibida no fuso `America/Sao_Paulo`. Enquanto não configurada, o campo fica nulo.
- RN-B33: Endpoint público único `GET /public/home-summary` retornando, em uma única resposta:
  - `scheduledDrawAt` (nulo se ainda não configurado) — usado para o contador regressivo (RN-F19).
  - `flagRanking`: top 5 bandeiras, ordenadas pela soma de `quantity` das transações `APPROVED` daquela bandeira, retornando código, nome, símbolo da bandeira e total de números.
  - `topBuyers`: lista com os 5 telefones com maior soma de `quantity` em transações `APPROVED`, cada um representado por:
    - um **avatar anônimo determinístico**, derivado de hash do telefone, combinando um emoji de um conjunto fixo (~40 opções) e uma cor de fundo de um conjunto fixo (~8 opções) — resultando em até ~320 combinações possíveis, para reduzir colisões visuais mesmo com centenas de compradores distintos;
    - a quantidade total de números comprados (soma de `quantity`).
  - Este endpoint **nunca** deve retornar nome, telefone, e-mail ou valores monetários — apenas os dados acima.
- RN-B33.1: O valor unitário vigente (`unit_price` de `raffle_config`) **não** é exposto neste endpoint público; ele já é retornado pelo endpoint de cotação (RN-B09), que é o consultado pelo front na tela de compra.

### 4.7 Tratamento de erros
- RN-B24: Erros de comunicação com a API do Mercado Pago devem ser tratados e logados, retornando mensagem clara ao front-end sem expor detalhes internos/stack trace.
- RN-B25: Toda ação sensível (criação de preferência, geração de números, sorteio) deve ser logada (auditoria mínima: quem, quando, o quê).

---

## 5. Modelo de dados (sugestão inicial)

**Transacao (Pedido)**
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID/Long | PK |
| name | String | nome do contribuinte (obrigatório) |
| phone | String | telefone do contribuinte (obrigatório) |
| email | String | **opcional** (nullable) |
| quantidade | Integer | quantidade de números comprados |
| valor_total | Decimal | calculado no momento da compra ou do registro em dinheiro (`quantidade × unit_price` desta própria transação) |
| unit_price | Decimal | valor unitário vigente no momento da criação desta transação (imutável depois de criada) |
| payment_method | Enum | `MERCADO_PAGO`, `CASH` |
| status | Enum | PENDENTE, APROVADA, RECUSADA, CANCELADA |
| external_reference | String | referência UUID única da transação, também usada para pagamento em dinheiro |
| mp_payment_id | String | id do pagamento no Mercado Pago (nulo quando `payment_method = CASH`) |
| criado_em / atualizado_em | Timestamp | |

**NumeroSorte**
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID/Long | PK |
| numero | String/Integer | número gerado (ex: "04231") |
| transacao_id | FK | referência à transação (nome/telefone/e-mail do contribuinte vêm da transação, não duplicados aqui) |
| criado_em | Timestamp | |

*(Constraint de unicidade em `numero` para garantir não-repetição global.)*

**Sorteio**
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID/Long | PK |
| numero_vencedor | String/Integer | |
| transacao_vencedora_id | FK | referência à transação vencedora (nome/telefone/e-mail vêm de lá) |
| realizado_em | Timestamp | |

**AdminUser**
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID/Long | PK |
| usuario | String | |
| senha_hash | String | BCrypt |

**RaffleConfig** *(linha única — configuração vigente da rifa)*
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | PK, sempre 1 linha |
| unit_price | Decimal | valor unitário vigente do número; seed inicial vem de `RAFFLE_UNIT_PRICE` (env), depois editável via admin (RN-B02.1) |
| scheduled_draw_at | Timestamp (nullable) | data/hora do sorteio, editável via admin (RN-B32); nulo até ser configurada |
| updated_at | Timestamp | |

> O intervalo de números (`numero_min`/`numero_max`) continua vindo de variáveis de ambiente (RN-B03) — não mudou. Apenas o valor unitário migrou de env var para tabela de configuração no banco (RN-B02).

---

## 6. Sugestão de Infraestrutura / Deploy (gratuito)

> Sugestão inicial, validada para o volume de tráfego esperado (rifa pontual, poucos acessos concentrados). Pode ser revista se o projeto crescer.

### 6.1 Componentes sugeridos

| Camada | Serviço sugerido | Observação |
|---|---|---|
| Back-end (Spring Boot) | **Render** (free tier) | Web service com deploy via Docker ou GitHub; expõe URL pública HTTPS (necessária para o webhook do Mercado Pago) |
| Banco de dados (PostgreSQL) | **Neon** ou **Supabase** (free tier) | Preferido separado do Postgres free do próprio Render, que costuma ter limites/expiração mais agressivos |
| Front-end (React) | **Vercel** ou **Netlify** (free tier) | Deploy estático, integração com GitHub, HTTPS e CDN inclusos |

### 6.2 Diagrama simplificado

```
[React - Vercel/Netlify] → chama API → [Spring Boot - Render] → [PostgreSQL - Neon/Supabase]
                                              ↑
                                    webhook Mercado Pago
```

### 6.3 Pontos de atenção do free tier

- RN-D01: Serviços gratuitos de back-end (ex: Render) hibernam após um período de inatividade e podem levar alguns segundos para "acordar" na primeira requisição após esse período (cold start). Isso reforça a importância da RN-B15 (o front-end deve consultar ativamente o status da transação, e não depender apenas do recebimento do webhook).
- RN-D02: O endpoint de webhook do Mercado Pago exige uma URL pública acessível via HTTPS — todos os serviços sugeridos atendem esse requisito por padrão.
- RN-D03: Bancos de dados free tier costumam ter limite de armazenamento e, em alguns casos, expiração por inatividade. Como o volume de dados da rifa é pequeno, isso não deve ser um problema, mas recomenda-se monitorar o painel do provedor escolhido, principalmente se houver um intervalo longo entre a criação do projeto e a data do casamento.
- RN-D04: Todas as variáveis de ambiente sensíveis (credenciais do Mercado Pago, banco, SMTP, JWT secret, valor unitário do número, intervalo min/max) devem ser configuradas diretamente nos painéis dos provedores (Render/Vercel/Netlify), nunca commitadas no repositório.

---

## 7. Pontos em aberto (a confirmar depois)

- Valor inicial (seed) do preço unitário e intervalo min/max dos números — a definir mais próximo do desenvolvimento/deploy.
- Layout/textos exatos do e-mail, do PDF e da tela de sucesso.
- Confirmação final dos provedores de deploy (Render/Neon/Vercel foram sugeridos, mas podem ser trocados por equivalentes se preferir); ambiente de homologação usará contas próprias nesses mesmos provedores, apontando para o Mercado Pago em modo sandbox.
- Horário exato do sorteio no dia 05/09/2026 (data já definida; horário a confirmar e configurar via RN-B32).
- Lista definitiva dos ~40 emojis e ~8 cores usados nos avatares do rank público (RN-B33) — pode ser definida na implementação, desde que determinística por telefone.
