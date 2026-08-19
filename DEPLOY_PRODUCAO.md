# Deploy em producao

Este guia descreve o passo a passo para publicar a aplicacao em producao usando:

- Banco de dados: Neon PostgreSQL
- Back-end: Render Web Service
- Front-end: Vercel
- Pagamento: Mercado Pago Checkout Pro em producao

Nao commite tokens, senhas, hashes reais, URLs privadas ou qualquer outro segredo. Todas as credenciais devem ficar apenas nos paineis dos provedores.

## 1. Preparacao

1. Atualize a branch base e crie uma branch propria para ajustes de deploy:

```bash
git switch dev
git pull
git switch -c chore/production-deploy
```

2. Rode as validacoes locais antes de publicar:

```bash
cd backend
mvn clean test
```

```bash
cd frontend
npm test -- --run
npm run build
```

3. Confirme que nenhum arquivo de ambiente real sera commitado:

```bash
git status --short
```

Arquivos como `.env`, tokens do Mercado Pago, senhas de banco, senha SMTP e `JWT_SECRET` nao devem aparecer como arquivos versionados.

## 2. Banco de dados no Neon

1. Crie uma conta ou acesse o Neon.
2. Crie um projeto PostgreSQL para producao.
3. Crie ou use um database dedicado, por exemplo:

```text
wedding_raffle
```

4. Copie os dados de conexao:

```text
host
database
user
password
```

5. Monte a URL JDBC para o Spring Boot:

```text
jdbc:postgresql://<neon-host>/<database>?sslmode=require
```

Exemplo de formato:

```text
DATABASE_URL=jdbc:postgresql://ep-example.us-east-1.aws.neon.tech/wedding_raffle?sslmode=require
DATABASE_USERNAME=<neon_user>
DATABASE_PASSWORD=<neon_password>
```

As migrations do Flyway serao executadas automaticamente quando o back-end subir pela primeira vez.

Atencao: configure o usuario admin e o hash da senha antes da primeira execucao em producao. Se a primeira migration rodar com um hash incorreto, sera necessario corrigir manualmente no banco ou recriar o banco de producao antes de liberar o sistema.

## 3. Segredos e valores de producao

Antes de criar o servico do back-end, separe estes valores:

```text
ADMIN_USERNAME=<usuario_admin>
ADMIN_PASSWORD_HASH=<hash_bcrypt_da_senha_admin>
JWT_SECRET=<segredo_longo_e_aleatorio>
MERCADO_PAGO_ACCESS_TOKEN=<access_token_de_producao>
MERCADO_PAGO_WEBHOOK_SECRET=<secret_do_webhook>
SMTP_HOST=<host_smtp>
SMTP_PORT=<porta_smtp>
SMTP_USERNAME=<usuario_smtp>
SMTP_PASSWORD=<senha_smtp>
SMTP_FROM=<email_remetente>
```

Recomendacoes:

- Gere `JWT_SECRET` com pelo menos 32 bytes aleatorios.
- Gere `ADMIN_PASSWORD_HASH` com BCrypt custo 12.
- Nao use geradores online para senhas, hashes ou secrets.
- Use credenciais de producao do Mercado Pago apenas no ambiente de producao.

## 4. Back-end no Render

1. Crie um novo Web Service no Render conectado ao repositorio GitHub.
2. Configure:

```text
Root Directory: backend
Runtime: Java
Build Command: mvn clean package -DskipTests
Start Command: java -jar target/raffle-api-0.0.1-SNAPSHOT.jar
```

3. Configure Java 21 no Render. Se o painel permitir variavel de runtime, use:

```text
JAVA_VERSION=21
```

4. Configure as variaveis de ambiente:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=10000

DATABASE_URL=jdbc:postgresql://<neon-host>/<database>?sslmode=require
DATABASE_USERNAME=<neon_user>
DATABASE_PASSWORD=<neon_password>

SPRING_FLYWAY_PLACEHOLDERS_ADMIN_USERNAME=<admin_user>
SPRING_FLYWAY_PLACEHOLDERS_ADMIN_PASSWORD_HASH=<bcrypt_hash>

FRONTEND_ORIGIN=https://<vercel-domain>

JWT_SECRET=<strong_random_secret>
JWT_EXPIRATION_SECONDS=3600
JWT_ISSUER=raffle-api

RAFFLE_UNIT_PRICE=10.00
RAFFLE_NUMBER_MIN=00000
RAFFLE_NUMBER_MAX=99999

MERCADO_PAGO_ACCESS_TOKEN=<production_access_token>
MERCADO_PAGO_WEBHOOK_URL=https://<render-domain>/payments/webhook
MERCADO_PAGO_WEBHOOK_SECRET=<webhook_secret>
MERCADO_PAGO_SUCCESS_URL=https://<vercel-domain>/payment-return/success
MERCADO_PAGO_FAILURE_URL=https://<vercel-domain>/payment-return/failure
MERCADO_PAGO_PENDING_URL=https://<vercel-domain>/payment-return/pending
MERCADO_PAGO_RETRY_MAX_ATTEMPTS=3
MERCADO_PAGO_RETRY_DELAY_MILLIS=500
MERCADO_PAGO_RETRY_MULTIPLIER=2

SMTP_HOST=<smtp_host>
SMTP_PORT=<smtp_port>
SMTP_USERNAME=<smtp_username>
SMTP_PASSWORD=<smtp_password>
SMTP_FROM=<smtp_from>
```

5. Faca o primeiro deploy.
6. Confira os logs do Render e valide que:

- A aplicacao iniciou com profile `prod`.
- O servidor esta escutando na porta configurada.
- O Flyway executou as migrations com sucesso.
- Nao ha erro de conexao com o Neon.

7. Anote a URL publica do Render:

```text
https://<render-domain>
```

Essa URL sera usada no front-end e no webhook do Mercado Pago.

## 5. Front-end na Vercel

1. Crie um novo projeto na Vercel conectado ao repositorio GitHub.
2. Configure:

```text
Root Directory: frontend
Framework Preset: Vite
Build Command: npm run build
Output Directory: dist
```

3. Configure a variavel de ambiente:

```text
VITE_API_BASE_URL=https://<render-domain>
```

Nao adicione barra no final da URL.

4. Faca o deploy.
5. Anote a URL publica da Vercel:

```text
https://<vercel-domain>
```

6. Volte ao Render e atualize as variaveis que dependem da URL final do front-end:

```text
FRONTEND_ORIGIN=https://<vercel-domain>
MERCADO_PAGO_SUCCESS_URL=https://<vercel-domain>/payment-return/success
MERCADO_PAGO_FAILURE_URL=https://<vercel-domain>/payment-return/failure
MERCADO_PAGO_PENDING_URL=https://<vercel-domain>/payment-return/pending
```

7. Faca redeploy do back-end no Render.

## 6. Mercado Pago em producao

1. Acesse o painel de desenvolvedores do Mercado Pago.
2. Abra a aplicacao usada pelo projeto.
3. Ative as credenciais de producao.
4. Copie o `access_token` de producao.
5. No Render, configure:

```text
MERCADO_PAGO_ACCESS_TOKEN=<production_access_token>
```

6. Configure o webhook da aplicacao no Mercado Pago:

```text
URL: https://<render-domain>/payments/webhook
Evento/topico: payment
```

7. Se o Mercado Pago fornecer ou permitir configurar um segredo do webhook, salve o mesmo valor no Render:

```text
MERCADO_PAGO_WEBHOOK_SECRET=<webhook_secret>
```

8. Configure as URLs de retorno:

```text
Success URL: https://<vercel-domain>/payment-return/success
Failure URL: https://<vercel-domain>/payment-return/failure
Pending URL: https://<vercel-domain>/payment-return/pending
```

9. Confirme no Render:

```text
MERCADO_PAGO_WEBHOOK_URL=https://<render-domain>/payments/webhook
MERCADO_PAGO_SUCCESS_URL=https://<vercel-domain>/payment-return/success
MERCADO_PAGO_FAILURE_URL=https://<vercel-domain>/payment-return/failure
MERCADO_PAGO_PENDING_URL=https://<vercel-domain>/payment-return/pending
```

10. Faca novo deploy do back-end se qualquer variavel tiver sido alterada.

## 7. Ordem recomendada de publicacao

Use esta ordem para evitar ciclos entre URL do front, URL do back-end e Mercado Pago:

1. Criar banco no Neon.
2. Criar Render com as variaveis do banco e placeholders de URL.
3. Fazer deploy do back-end.
4. Copiar URL publica do Render.
5. Criar Vercel com `VITE_API_BASE_URL` apontando para o Render.
6. Fazer deploy do front-end.
7. Copiar URL publica da Vercel.
8. Atualizar `FRONTEND_ORIGIN` e URLs de retorno no Render.
9. Ativar credenciais de producao do Mercado Pago.
10. Atualizar `MERCADO_PAGO_ACCESS_TOKEN` no Render.
11. Configurar webhook e URLs de retorno no Mercado Pago.
12. Fazer redeploy final do back-end.
13. Executar testes de fumaca em producao.

## 8. Testes de fumaca em producao

Execute estes testes antes de divulgar o link aos convidados:

1. Acesse:

```text
https://<vercel-domain>
```

2. Confirme que a home carrega sem erro.
3. Confirme que o contador do sorteio aparece corretamente.
4. Confirme que, se o sorteio estiver encerrado, a compra de novos numeros nao fica disponivel.
5. Confirme que o card do numero sorteado aparece apenas quando o sorteio estiver encerrado e houver resultado.
6. Inicie uma compra real com valor baixo ou valor configurado para validacao.
7. Confirme que o checkout abre no Mercado Pago.
8. Finalize o pagamento em producao.
9. Confirme o retorno para uma das rotas:

```text
/payment-return/success
/payment-return/failure
/payment-return/pending
```

10. No Render, confirme nos logs que o webhook chegou em:

```text
/payments/webhook
```

11. Confirme no banco que a transacao foi atualizada.
12. Confirme que os numeros da sorte foram gerados apos pagamento aprovado.
13. Confirme que o e-mail foi enviado para o comprador.
14. Acesse a area admin.
15. Confirme que o sorteio pode ser executado apenas pelo admin.
16. Confirme que o resultado exibe numero, nome e bandeira quando houver ganhador.

## 9. Checklist antes de divulgar

- [ ] Neon criado e acessivel.
- [ ] Render publicado com profile `prod`.
- [ ] Flyway executado sem erro.
- [ ] Admin criado com usuario e senha corretos.
- [ ] Vercel publicado apontando para o Render.
- [ ] CORS funcionando com `FRONTEND_ORIGIN`.
- [ ] Credenciais de producao do Mercado Pago ativadas.
- [ ] Webhook `payment` configurado no Mercado Pago.
- [ ] URLs de retorno configuradas no Mercado Pago e no Render.
- [ ] SMTP validado.
- [ ] Compra real testada.
- [ ] Webhook recebido.
- [ ] Numeros gerados apos pagamento aprovado.
- [ ] E-mail recebido.
- [ ] Logs sem erro critico.

## 10. Operacao e manutencao

- Monitore os logs do Render durante as primeiras compras.
- Monitore o painel do Mercado Pago para pagamentos pendentes, recusados e aprovados.
- Monitore o Neon para conexoes, uso de armazenamento e erros.
- Antes de alterar regra de sorteio, preco ou intervalo de numeros, valide a regra em `docs/regras-de-negocio.md`.
- Para alterar secrets, use somente os paineis dos provedores.
- Para rollback do front-end, use a tela de deployments da Vercel.
- Para rollback do back-end, use o historico de deploys do Render.
- Nao faca rollback manual do banco sem backup.

## 11. Problemas comuns

### Front-end nao acessa o back-end

Verifique:

```text
VITE_API_BASE_URL=https://<render-domain>
FRONTEND_ORIGIN=https://<vercel-domain>
```

Depois de mudar `VITE_API_BASE_URL`, faca novo deploy do front-end.
Depois de mudar `FRONTEND_ORIGIN`, faca novo deploy do back-end.

### Webhook nao chega

Verifique:

```text
MERCADO_PAGO_WEBHOOK_URL=https://<render-domain>/payments/webhook
```

Confirme tambem que:

- A URL e HTTPS publica.
- O Render esta acordado.
- O evento `payment` esta habilitado no Mercado Pago.
- O token configurado no Render e de producao.

### Compra aprovada mas numeros nao aparecem

Verifique:

- Logs do webhook no Render.
- Status da transacao no banco.
- Se o pagamento foi aprovado no Mercado Pago.
- Se houve erro de envio de e-mail.

### Erro na primeira migration

Verifique:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `SPRING_FLYWAY_PLACEHOLDERS_ADMIN_USERNAME`
- `SPRING_FLYWAY_PLACEHOLDERS_ADMIN_PASSWORD_HASH`

Se o usuario admin foi criado com hash incorreto, corrija o registro no banco ou recrie o banco antes de liberar o ambiente.

