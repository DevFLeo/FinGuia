# FinGuia Web

Visualizador do banco de dados do FinGuia no navegador. Abre o mesmo arquivo
SQLite que o app Android grava (`finguia_database`) e mostra extrato, fluxo
mensal, totais por banco, lançamentos agendados e investimentos.

O arquivo é lido **dentro do navegador**, via SQLite compilado para WebAssembly
([sql.js](https://sql.js.org), incluído em `vendor/`). Nada é enviado para
nenhum servidor, e o app funciona sem internet.

## Como rodar

```bat
web\finguia-web.bat
```

No Linux/macOS:

```bash
web/finguia-web.sh
```

O script sobe um servidor local em `http://127.0.0.1:8080` e abre o navegador.
Para usar um banco de outro lugar:

```bat
web\finguia-web.bat C:\caminho\finguia_database
```

Opções do servidor: `node servidor.js --porta 9000 --banco <arquivo> --sem-navegador`.

## Login simulado

A página abre numa tela de login. Para apresentar sem cadastro, use a conta de
demonstração **`demo` / `finguia`**, criada automaticamente. "Criar conta"
cadastra um usuário novo (3 a 30 caracteres; senha com pelo menos 4).

É **simulação**: não há servidor de contas. Usuários ficam no `localStorage`
deste navegador, com a senha guardada como hash SHA-256 de sal aleatório +
senha, nunca em texto. A sessão vale para a aba (`sessionStorage`) e o botão
**Sair** a encerra. Isso demonstra o fluxo de login e separa os dashboards de
cada usuário, mas **não protege os dados** de quem usa o mesmo computador.

A criptografia do navegador só funciona em contexto seguro: abra pelo
`finguia-web.bat` (http://127.0.0.1), não com duplo clique no `index.html`.

## Dashboards e templates

Cada usuário pode ter vários dashboards, cada um criado a partir de um template:

| Template | O que mostra |
|---|---|
| **Completo** | Totais e abas: extrato, fluxo mensal, por banco, agendados, investimentos |
| **Resumo** | Totais, fluxo mensal e os 5 maiores gastos |
| **Gastos** | Total gasto, nº de saídas, média e maior saída; saídas por tipo; maiores gastos; por banco |
| **Investimentos** | Aplicado, valor atual, resultado e nº de ativos; lista da carteira |

No topo: o seletor troca de dashboard, **+ Novo dashboard** pede nome e
template, e **Excluir dashboard** remove o atual (o último não pode ser
excluído). A escolha fica salva por usuário. O primeiro dashboard de cada
usuário é o "Principal", com o template Completo.

Para criar um template novo: acrescente uma entrada em `TEMPLATES`
(`dashboards.js`) com a lista de blocos. Os blocos disponíveis estão em
`BLOCOS` (`widgets.js`); para um bloco novo, escreva uma função que recebe o
contexto e devolve um elemento com `data-bloco`.

## Testes

```bash
cd web/testes
npm install
npm test                  # interface no jsdom (57 verificações)
npm run test:navegador    # mesmo fluxo num Firefox real (11 verificações)
```

Os testes usam `dados-teste/banco-ficticio.db`, que só tem dados fictícios, e
conferem os números da página contra `dados-teste/esperado.json`, calculado
direto no SQLite. O teste no navegador usa o Firefox instalado (caminho
alternativo pela variável `FIREFOX`), numa instância separada que não mexe no
Firefox que estiver aberto, e salva capturas de tela em `resultados/`.

## Como obter o banco do aparelho

```bat
web\exportar-banco.bat
```

Copia o banco do aparelho conectado para `web/dados/` via `adb` e consolida o
WAL. Requer depuração USB ativa e a **build debug** instalada (`run-as` não
funciona em build de release).

Se você já tem um backup em `.tar` (como `backup_finguia/finguia_backup.tar`):

```bat
tar -xf backup_finguia\finguia_backup.tar -C web\dados --strip-components=1 databases/
node web\consolidar-wal.js web\dados\finguia_database
```

### Por que consolidar o WAL

O Room grava em modo WAL: o `finguia_database` puro costuma ter só alguns KB, e
as transações recentes ficam no `finguia_database-wal`. O sql.js não lê WAL, então
`consolidar-wal.js` roda um `PRAGMA wal_checkpoint(TRUNCATE)` e dobra o WAL para
dentro do `.db` antes de abrir. Sem esse passo a página abre vazia.

## Estrutura

| Arquivo | Papel |
|---|---|
| `index.html` | Marcação das telas |
| `estilos.css` | Tema claro/escuro, espelhando as cores do app |
| `esquema.js` | Espelho do esquema Room: enums, rótulos e classificação entrada/saída |
| `banco.js` | Abre o SQLite e roda as consultas equivalentes ao `TransacaoDao` |
| `app.js` | Fluxo entre as telas (login → banco → dashboard), seletor e exportação CSV |
| `auth.js` | Login simulado: contas com senha em hash, sessão e Sair |
| `dashboards.js` | Templates e os dashboards salvos de cada usuário |
| `widgets.js` | Blocos visuais que os templates combinam |
| `servidor.js` | Servidor local sem dependências (só o Node padrão) |
| `consolidar-wal.js` | Checkpoint do WAL antes de abrir no navegador |
| `vendor/` | sql.js (MIT), versionado para funcionar offline |
| `testes/` | Testes automatizados da página (jsdom e Firefox) |

## Manutenção

`esquema.js` e `banco.js` reproduzem o que está em
`app/src/main/java/com/finguia/dados/`. Ao mudar uma entidade ou adicionar uma
migração no `FinGuiaDatabase`, atualize os dois — o visualizador avisa quando
encontra um banco sem as tabelas esperadas, mas não detecta colunas novas.
