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
| `app.js` | Monta as abas, filtros, gráfico e exportação CSV |
| `servidor.js` | Servidor local sem dependências (só o Node padrão) |
| `consolidar-wal.js` | Checkpoint do WAL antes de abrir no navegador |
| `vendor/` | sql.js (MIT), versionado para funcionar offline |

## Manutenção

`esquema.js` e `banco.js` reproduzem o que está em
`app/src/main/java/com/finguia/dados/`. Ao mudar uma entidade ou adicionar uma
migração no `FinGuiaDatabase`, atualize os dois — o visualizador avisa quando
encontra um banco sem as tabelas esperadas, mas não detecta colunas novas.
