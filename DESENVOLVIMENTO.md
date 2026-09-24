# FinGuia — Guia de Desenvolvimento

> Leitura para quem vai mexer no código ou apresentar o projeto. Explica **como o
> app pensa**, onde cada coisa mora e traz dois tutoriais curtos: um para
> consertar problemas comuns e outro para apresentar.
>
> Para a lista função por função, veja também o [`FUNCOES.md`](FUNCOES.md).
> Algumas partes dele descrevem versões antigas (por exemplo `CriptoApi.kt`,
> que virou `MercadoApi.kt`). Quando os dois discordarem, vale o código e este guia.

---

## Sumário

1. [O app em uma frase](#1-o-app-em-uma-frase)
2. [Mapa das pastas](#2-mapa-das-pastas)
3. [Como o dado anda pelo app](#3-como-o-dado-anda-pelo-app)
4. [A lógica da captura de notificações](#4-a-lógica-da-captura-de-notificações)
5. [O banco de dados](#5-o-banco-de-dados)
6. [O pacote `motor` (Java)](#6-o-pacote-motor-java)
7. [FinGuia Web](#7-finguia-web)
8. [Tutorial: como consertar](#8-tutorial-como-consertar)
9. [Tutorial: como apresentar](#9-tutorial-como-apresentar)
10. [Limitações conhecidas](#10-limitações-conhecidas)

---

## 1. O app em uma frase

O FinGuia **lê as notificações dos apps de banco**, entende se foi entrada ou
saída e de quanto, e **grava a transação sozinho** — o usuário não precisa
digitar nada para ter o extrato em dia.

Todo o resto (painel, calculadoras, investimentos, notícias) gira em torno
desse banco de transações.

---

## 2. Mapa das pastas

```
app/src/main/java/com/finguia/
├── MainActivity.kt            Ponto de entrada: aplica o tema e abre o FinGuiaApp
├── SplashActivity.kt          Tela de abertura
├── FinGuiaApplication.kt      Configura o carregador de imagens (Coil + SVG)
│
├── service/                   ★ CAPTURA DE NOTIFICAÇÕES (o coração do app)
│   ├── NotificationReaderService.kt   Escuta o sistema e dispara o processamento
│   ├── AnalisadorNotificacao.kt       Descobre tipo e valor a partir do texto
│   ├── BancoConfig.kt                 Lista de pacotes de banco aceitos
│   └── NotificationListenerHelper.kt  Checa/reativa a permissão de notificações
│
├── dados/                     BANCO LOCAL (Room) E APIs
│   ├── FinGuiaDatabase.kt     Banco Room, versão e migrações
│   ├── TransacaoBancaria.kt   Tabela de transações + enum TipoTransacao
│   ├── TransacaoDao.kt        Consultas SQL de transações
│   ├── TransacaoRepository.kt Camada entre DAO e ViewModel
│   ├── CategoriaCustom.kt / CategoriaDao.kt / CategoriaRepository.kt
│   ├── Investimento.kt        Tabela, DAO e repositório de investimentos
│   ├── MercadoApi.kt          Retrofit: Yahoo Finance, Banco Central, GNews
│   └── MercadoRepository.kt   Cotações e taxas (Selic, CDI, IPCA)
│
├── motor/                     ★ REGRAS PURAS EM JAVA (sem Android, com testes)
│   ├── DinheiroBR.java        Dinheiro em centavos, soma exata
│   ├── NumeroBR.java          Lê e exibe números no padrão 0.000,00
│   └── MascaraMoeda.java      Digitação estilo app de banco
│
└── ui/                        TELAS (Jetpack Compose)
    ├── FinGuiaApp.kt          Navegação entre telas (enum DestinosApp)
    ├── home/  dashboard/  transacoes/  busca/  gerenciamento/
    ├── calculadora/  investimentos/  noticias/  notificacoes/
    ├── perfil/  configuracoes/
    └── theme/                 Cores, tipografia e tema

app/src/test/java/com/finguia/  Testes unitários (rodam no PC, sem celular)
web/                             FinGuia Web: visualizador do banco no navegador
```

---

## 3. Como o dado anda pelo app

```
 ┌──────────────┐   notificação   ┌──────────────────────────┐
 │ App do banco │ ──────────────▶ │ NotificationReaderService │
 └──────────────┘                 └────────────┬─────────────┘
                                               │ título + texto
                                               ▼
                                  ┌──────────────────────────┐
                                  │  AnalisadorNotificacao   │  tipo? valor?
                                  └────────────┬─────────────┘
                                               │ TransacaoBancaria
                                               ▼
                                  ┌──────────────────────────┐
                                  │ TransacaoRepository      │
                                  │   └─ TransacaoDao (Room) │ ──▶ SQLite no celular
                                  └────────────┬─────────────┘
                                               │ Flow (avisa sozinho quando muda)
                                               ▼
                                  ┌──────────────────────────┐
                                  │   TransacaoViewModel     │  StateFlow
                                  └────────────┬─────────────┘
                                               │ collectAsState()
                                               ▼
                                  ┌──────────────────────────┐
                                  │ Telas: Extrato, Home...  │  redesenham sozinhas
                                  └──────────────────────────┘
```

**O ponto-chave para explicar:** ninguém "manda a tela atualizar". O DAO devolve
um `Flow`, que emite de novo sempre que a tabela muda. O ViewModel transforma
em `StateFlow` e a tela em Compose redesenha quando o estado muda. Por isso uma
notificação que chega com o app aberto aparece no extrato na hora.

Isso é o padrão **MVVM**: *Model* (Room + Repository), *ViewModel* (estado da
tela) e *View* (Composables).

---

## 4. A lógica da captura de notificações

Tudo começa em `NotificationReaderService.onNotificationPosted()`. Cada
notificação passa por uma sequência de filtros; se cair em qualquer um, é
descartada.

### Passo a passo

| # | O que acontece | Por quê |
|---|---|---|
| 1 | Checa se o pacote está em `BancoConfig.BANCOS_SUPORTADOS` | Não ler WhatsApp, Instagram etc. Só bancos. |
| 2 | Descarta se for **resumo de grupo** (`FLAG_GROUP_SUMMARY`) | O resumo diz só "3 novas movimentações", sem valor. |
| 3 | Junta **todos** os campos de texto: `TEXT`, `BIG_TEXT`, `SUB_TEXT`, `SUMMARY_TEXT`, `INFO_TEXT`, `TEXT_LINES` | Muitos bancos põem o valor só no `BIG_TEXT` (o texto expandido). |
| 4 | **Anti-duplicata**: hash de `pacote + título + texto`, lembrado por 60 s (até 50 hashes) | Bancos re-postam a mesma notificação várias vezes. |
| 5 | `AnalisadorNotificacao.identificarTipo()` | Decide PIX_RECEBIDO, COMPRA_CREDITO etc. |
| 6 | `AnalisadorNotificacao.extrairValor()` | Tira o `R$ 1.234,56` do texto. |
| 7 | Se o tipo é `DESCONHECIDO` **e** o valor é 0, descarta | Nada útil para registrar. |
| 8 | Grava no Room e manda um broadcast `NOVA_TRANSACAO_BANCARIA` | O broadcast permite avisar outras partes do app. |

### Como o tipo é decidido (a regra da palavra mais longa)

Cada tipo tem uma lista de frases-chave em `PALAVRAS_TIPO`. O analisador
procura **todas** no texto e fica com a **mais longa** que aparecer.

Exemplo: *"Débito automático: pagamento efetuado de R$ 89,90"*

- `"débito automático"` casa com COBRANCA (17 letras)
- `"pagamento efetuado"` casa com BOLETO_PAGO (18 letras)
- Ganha a mais longa → **BOLETO_PAGO** ✔

E faz diferença: COBRANCA é neutra (não entra no saldo), BOLETO_PAGO é saída.
Como o dinheiro de fato saiu, o certo é BOLETO_PAGO.

Por isso **a ordem da lista não importa**: frase mais específica sempre vence
frase genérica.

**Se nada casar**, entra o *fallback direcional*: se o texto só tem palavras de
entrada ("recebido", "creditado"...) vira TRANSFERENCIA_RECEBIDA; se só tem de
saída ("debitado", "compra"...) vira TRANSFERENCIA_ENVIADA; se tem as duas ou
nenhuma, fica DESCONHECIDO.

### Como o valor é extraído

Uma regex procura, em ordem de preferência:

1. `R$` + valor com centavos → `R$ 1.500,00`, `R$250,00`
2. `R$` + valor sem centavos → `R$ 1.500`, `R$ 50`
3. valor solto **com** centavos → `1.234,56` (exigir centavos evita pegar datas como `12/05`)

Pega a **primeira** ocorrência. No padrão BR o ponto é milhar e a vírgula é
decimal.

### Entrada ou saída?

Quem decide o sinal é o **tipo**, não o valor (o valor é sempre positivo no
banco). A mesma regra está no `TransacaoDao` (SQL) e no `web/esquema.js`:

| Entradas (somam) | Saídas (subtraem) | Neutros (não entram nos totais) |
|---|---|---|
| PIX_RECEBIDO, TRANSFERENCIA_RECEBIDA, DEPOSITO, ESTORNO | PIX_ENVIADO, COMPRA_DEBITO, COMPRA_CREDITO, BOLETO_PAGO, TRANSFERENCIA_ENVIADA, SAQUE | COBRANCA, DESCONHECIDO |

`COBRANCA` é neutra de propósito: "sua fatura vence amanhã" é um **aviso**, não
dinheiro saindo.

---

## 5. O banco de dados

Arquivo no celular: `/data/data/com.finguia/databases/finguia_database`
(Room sobre SQLite). Versão atual: **5**.

### Tabelas

**`transacoes_bancarias`** — cada movimentação

| Coluna | Tipo | Significado |
|---|---|---|
| `id` | inteiro | Chave, gerada sozinha |
| `banco` | texto | Nome amigável ("Nubank") |
| `pacoteApp` | texto | Pacote Android de origem ("com.nu.production") |
| `tipo` | texto | Nome do enum `TipoTransacao` |
| `valor` | real | Sempre positivo; o sinal vem do tipo |
| `descricao` | texto | Frase gerada ou digitada |
| `tituloNotificacao` / `textoNotificacao` | texto | Texto original, para auditoria e busca |
| `timestampMs` | inteiro | Data/hora em milissegundos |
| `recorrente` | 0/1 | Marcado pelo usuário como fixo do mês |
| `dataAgendada` | inteiro ou nulo | Data futura de um lançamento agendado |
| `efetivado` | 0/1 | **0 = agendado, ainda não conta nos totais** |

**`categorias_custom`** — categorias criadas pelo usuário (`label`, `sublabel`, `tipo`, `ehEntrada`).

**`investimentos`** — carteira (`nome`, `categoria`, `valorInvestido`,
`rentabilidadePct`, `ticker`, `precoEntrada`, `quantidade`...). `valorAtual` e
`lucro` **não** são colunas: são calculados em `Investimento.kt`.

### Histórico de versões (migrações)

| Versão | Mudança |
|---|---|
| 1 → 2 | coluna `recorrente` |
| 2 → 3 | tabela `categorias_custom` |
| 3 → 4 | colunas `dataAgendada`, `efetivado` + tabela `investimentos` |
| 4 → 5 | colunas `ticker`, `precoEntrada`, `quantidade` em investimentos |

As migrações preservam os dados de quem já tinha o app instalado. **Nunca**
altere uma entidade sem criar a migração correspondente (receita na seção 8).

---

## 6. O pacote `motor` (Java)

Regras de negócio que **não dependem de Android** ficam em
`com.finguia.motor`, escritas em Java. Kotlin chama Java sem nenhuma
configuração extra, e por serem puras dá para testar tudo no PC em segundos.

### `DinheiroBR` — dinheiro em centavos

**Problema:** em `double`, `0.1 + 0.2` dá `0.30000000000000004`. Somando
centenas de transações o erro aparece no centavo.
**Solução:** guardar e somar em **centavos inteiros** (`long`); converter para
reais só na hora de mostrar.

| Função | Exemplo |
|---|---|
| `paraCentavos(double)` | `12.34` → `1234` |
| `lerCentavos(texto)` | `"R$ 1.234,56"` → `123456` |
| `formatar(centavos)` | `123456` → `"R$ 1.234,56"` |
| `formatarCompacto(centavos)` | `123400` → `"R$ 1,2 mil"` |
| `somar(...)` | soma exata, lança erro se estourar |
| `percentual(parte, total)` | não quebra com total zero |

### `NumeroBR` — padrão 0.000,00

| Função | Exemplo |
|---|---|
| `ler(texto)` | `"1.234,56"` → `1234.56`; `"1.500"` → `1500`; `"12.5"` → `12.5` |
| `formatar(valor)` | `1234.5` → `"1.234,50"` |
| `moeda(valor)` | `1234.5` → `"R$ 1.234,50"` |
| `moedaComSinal(valor)` | `10` → `"+R$ 10,00"` |
| `percentual(valor, casas)` | `12.5` → `"12,50%"` |
| `formatarFlexivel(v, min, max)` | cripto: `0.00001234` → `"0,00001234"` |
| `paraCampo(valor, casas)` | preenche campo editável sem pontos: `"1234,50"` |

> **Bug que o `ler()` corrige:** o código antigo fazia
> `texto.replace(",", ".").toDoubleOrNull()`. Com `"1.234,56"` isso vira
> `"1.234.56"`, que não é número → o campo valia **0 sem avisar ninguém**.

### `MascaraMoeda` — digitação de app de banco

O usuário digita só números e eles entram pela direita como centavos:
`1` → `0,01` → `0,12` → `1,23` → `12,34` → `1.234,56`. Não existe dúvida entre
ponto e vírgula porque o usuário nunca digita nenhum dos dois.

### Testes

```bash
./gradlew testDebugUnitTest --tests "com.finguia.motor.*"
```

Os testes estão em `app/src/test/java/com/finguia/motor/`. Cada caso de uso
tem um teste com nome descritivo — ler os testes é o jeito mais rápido de
entender o que cada função garante.

---

## 7. FinGuia Web

Visualizador do mesmo banco SQLite no navegador. Detalhes em
[`web/README.md`](web/README.md); o essencial:

```bat
web\exportar-banco.bat    :: puxa o banco do celular via adb
web\finguia-web.bat       :: abre http://127.0.0.1:8080
```

- O SQLite roda **dentro do navegador** (sql.js / WebAssembly). Nada sai do computador.
- `web/banco.js` repete as consultas do `TransacaoDao`, então os totais batem com o app.
- **Pegadinha:** o Room grava em modo WAL. O arquivo `.db` sozinho vem quase
  vazio; os dados recentes estão no `-wal`. O `consolidar-wal.js` junta os dois.
  Sem isso a página abre vazia.

---

## 8. Tutorial: como consertar

### 8.1 Antes de tudo

```bash
git switch desenvolvimento && git pull     # trabalhe sempre em desenvolvimento
./gradlew testDebugUnitTest                # testes (1–2 min, sem celular)
./gradlew assembleDebug                    # gera o APK (ou use build_apk.bat)
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

### 8.2 "O app não pegou a notificação do meu banco"

Com o celular no cabo e depuração USB ligada:

```bash
adb logcat -s FinGuia-Notif
```

Faça uma transação e leia o log. Cada situação tem uma causa:

| O log mostra | Causa | Conserto |
|---|---|---|
| **Nada** | Permissão desligada ou pacote não cadastrado | No Android: Configurações → Apps → Acesso especial → Acesso a notificações → FinGuia ligado. Se estiver ok, siga 8.3 |
| `Ignorada — resumo de grupo` | Normal: a notificação individual vem logo em seguida | Nada a fazer |
| `Ignorada — duplicata recente` | Mesma notificação repetida em menos de 60 s | Nada a fazer |
| `Descartada — sem tipo/valor. Título='...' Texto='...'` | Frase do banco não reconhecida | Siga 8.4 com o texto do log |
| `Transação salva` com tipo errado | Palavra-chave de outro tipo venceu | Siga 8.4 |

### 8.3 Adicionar um banco novo

1. Descubra o pacote: `adb shell pm list packages | findstr -i nomedobanco`
2. Adicione uma linha em `service/BancoConfig.kt`:
   ```kotlin
   "br.com.novobanco.app"          to "Novo Banco",
   ```
3. Recompile e teste com uma transação real.

### 8.4 Ensinar uma frase nova ao analisador

1. Copie o texto exato do log (seção 8.2).
2. Em `service/AnalisadorNotificacao.kt`, ache o tipo certo em `PALAVRAS_TIPO`
   e acrescente a frase **em minúsculas**, o mais específica possível:
   ```kotlin
   TipoTransacao.PIX_RECEBIDO to listOf(
       ...,
       "pix caiu na sua conta"      // ← nova
   ),
   ```
3. Lembre da regra: **a mais longa vence**. Frase curta e genérica ("pix")
   rouba casos de outros tipos. Prefira frases de 3 ou mais palavras.

### 8.5 Adicionar uma coluna no banco (migração)

Exemplo: adicionar `observacao` em transações.

1. **Entidade** (`TransacaoBancaria.kt`) — campo com valor padrão:
   ```kotlin
   val observacao: String = ""
   ```
2. **Versão** (`FinGuiaDatabase.kt`) — `version = 5` vira `version = 6`.
3. **Migração** no `companion object`:
   ```kotlin
   private val MIGRACAO_5_6 = object : Migration(5, 6) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL(
               "ALTER TABLE transacoes_bancarias ADD COLUMN observacao TEXT NOT NULL DEFAULT ''"
           )
       }
   }
   ```
4. **Registrar**: `.addMigrations(MIGRACAO_1_2, ..., MIGRACAO_4_5, MIGRACAO_5_6)`
5. **Web**: se a coluna for exibida no navegador, atualize `web/esquema.js` e `web/banco.js`.
6. Teste instalando **por cima** da versão anterior (não desinstale) — é assim
   que se descobre migração quebrada antes do usuário.

> Esqueceu a migração? O app fecha ao abrir com
> `IllegalStateException: A migration from 5 to 6 was required but not found`.

### 8.6 Mostrar ou ler um número na tela

Sempre pelo `NumeroBR`, nunca `"%.2f".format(...)` nem `toDoubleOrNull()`:

```kotlin
import com.finguia.motor.NumeroBR

Text(NumeroBR.moeda(total))                     // "R$ 1.234,56"
Text(NumeroBR.percentual(taxa, 2))              // "10,75%"
val valor = NumeroBR.ler(textoDoCampo) ?: 0.0   // aceita 1.234,56
```

### 8.7 Quando um teste falhar

O relatório abre no navegador:
`app/build/reports/tests/testDebugUnitTest/index.html`.
Ele mostra qual caso falhou, o valor esperado e o obtido.

---

## 9. Tutorial: como apresentar

### 9.1 Checklist do dia anterior

- [ ] APK da branch **`producao`** instalado no celular de demonstração
- [ ] Permissão de acesso a notificações **ligada**
- [ ] Algumas transações reais no extrato (faça 2 ou 3 Pix pequenos antes)
- [ ] Um segundo celular ou pessoa para mandar um Pix **ao vivo**
- [ ] FinGuia Web com o banco já exportado (`web\exportar-banco.bat`), como plano B se o celular falhar
- [ ] Tela do celular espelhada no projetor (ex.: `scrcpy`)
- [ ] Modo não perturbe ligado **para outros apps** — só o banco pode notificar

### 9.2 Roteiro de 10 minutos

| Tempo | O que mostrar | O que falar |
|---|---|---|
| 0–1 min | Tela inicial | **O problema:** ninguém anota cada gasto. A planilha morre na segunda semana. |
| 1–4 min | **Pix ao vivo** → notificação → extrato atualiza sozinho | **A solução:** o app lê a notificação do banco e registra sem o usuário digitar nada. *Este é o momento mais forte: deixe a plateia ver acontecer.* |
| 4–5 min | Extrato e Painel | Entradas, saídas e saldo calculados das transações capturadas. |
| 5–7 min | Calculadoras e Investimentos | Taxas reais do Banco Central (Selic, CDI, IPCA) e cotações do Yahoo Finance. |
| 7–8 min | FinGuia Web no navegador | O mesmo banco lido no computador, sem servidor — tudo local. |
| 8–10 min | Slide de arquitetura (diagrama da seção 3) | MVVM, Room, Flow reativo, testes unitários. |

### 9.3 Frases que explicam bem

- *"O app não pede senha de banco nenhum. Ele só lê o que já aparece na sua tela de notificações."*
- *"Os dados nunca saem do celular. Não temos servidor."*
- *"A tela não é mandada atualizar: ela observa o banco de dados e redesenha sozinha."*
- *"Dinheiro é guardado em centavos inteiros, porque somar números com vírgula no computador acumula erro."*

### 9.4 Perguntas prováveis da banca

**"E se o banco mudar o texto da notificação?"**
O analisador tem fallback direcional (seção 4): mesmo sem reconhecer a frase,
palavras como "recebido" ou "debitado" ainda classificam a direção. E incluir
uma frase nova é uma linha de código (seção 8.4).

**"Não é inseguro ler notificações?"**
É uma permissão do próprio Android, que o usuário concede explicitamente e pode
revogar. O app só processa pacotes da lista de bancos (`BancoConfig`) e não
envia nada para a internet.

**"Por que não usam Open Finance?"**
O Open Finance Brasil exige que a empresa seja participante autorizada pelo
Banco Central, com certificados ICP-Brasil e conexão mTLS. Um app sem esse
credenciamento não pode chamar as APIs dos bancos. O caminho viável para um
projeto como este seria um agregador credenciado (como Pluggy ou Belvo), que é
pago e exige contrato. A leitura de notificações entrega o resultado prático
sem esse requisito.

**"Por que parte do código está em Java?"**
As regras puras de dinheiro e número (pacote `motor`) não dependem do Android.
Isoladas assim, rodam em testes no PC em segundos. Kotlin e Java convivem no
mesmo projeto sem configuração extra.

**"Como garantem que o cálculo está certo?"**
Testes unitários em `app/src/test`. Mostre o relatório verde de
`./gradlew testDebugUnitTest`.

### 9.5 Se algo der errado ao vivo

| Problema | Saída |
|---|---|
| Pix não chegou | Mostre uma transação que já estava capturada e o texto original dela no detalhe |
| App travou | Abra o FinGuia Web com o banco exportado |
| Sem internet | Captura e extrato funcionam offline; só cotações e notícias dependem de rede |

---

## 10. Limitações conhecidas

Coisas que funcionam, mas têm espaço para melhorar. Bom saber antes que alguém pergunte.

- **Horário da transação** é o da captura, não o de quando o banco postou a
  notificação (`timestampMs` usa o padrão `System.currentTimeMillis()`, e não
  `sbn.postTime`). Normalmente a diferença é de milissegundos, mas notificações
  atrasadas ficam com a hora errada.
- **Anti-duplicata fica só na memória.** Se o Android reiniciar o serviço dentro
  da janela de 60 s, uma re-postagem pode ser gravada de novo.
- **Notificações recebidas com o serviço parado não são recuperadas** quando ele volta.
- **Valor:** pega a primeira ocorrência de dinheiro no texto. Uma notificação
  como "Pix de R$ 50,00 — saldo R$ 1.200,00" funciona, mas se o banco escrever
  o saldo antes do valor, o saldo é registrado.
- **Estorno pode virar Pix recebido.** Em *"Você recebeu um estorno"*, a frase
  `"você recebeu"` (PIX_RECEBIDO, 12 letras) vence `"estorno"` (7 letras). Os
  dois são entrada, então **o saldo fica certo** — só o rótulo sai errado.
- **`FUNCOES.md`** tem trechos de versões antigas (ver aviso no topo).
