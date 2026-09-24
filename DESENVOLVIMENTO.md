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
7. [Importação de extratos e Open Finance](#7-importação-de-extratos-e-open-finance)
8. [Aparência, formato de números e padrões](#8-aparência-formato-de-números-e-padrões)
9. [FinGuia Web](#9-finguia-web)
10. [Tutorial: como consertar](#10-tutorial-como-consertar)
11. [Tutorial: como apresentar](#11-tutorial-como-apresentar)
12. [Limitações conhecidas](#12-limitações-conhecidas)

---

## 1. O app em uma frase

O FinGuia **lê as notificações dos apps de banco**, entende se foi entrada ou
saída e de quanto, e **grava a transação sozinho** — o usuário não precisa
digitar nada para ter o extrato em dia. Para o histórico antigo, ou bancos que
não notificam, dá para **importar o extrato OFX** do banco.

Todo o resto (painel, calculadoras, investimentos, notícias) gira em torno
desse banco de transações.

---

## 2. Mapa das pastas

```
app/src/main/java/com/finguia/
├── MainActivity.kt            Ponto de entrada: aplica o tema escolhido e abre o FinGuiaApp
├── SplashActivity.kt          Tela de abertura
├── FinGuiaApplication.kt      Configura o carregador de imagens (Coil + SVG)
│
├── service/                   ★ CAPTURA DE NOTIFICAÇÕES (o coração do app)
│   ├── NotificationReaderService.kt   Escuta o sistema, recupera perdidas, grava
│   ├── AnalisadorNotificacao.kt       Decide: transação (tipo, valor, quem) ou descarte
│   ├── BancoConfig.kt                 Lista de pacotes de banco aceitos
│   └── NotificationListenerHelper.kt  Checa/reativa a permissão de notificações
│
├── dados/                     BANCO LOCAL (Room) E APIs
│   ├── FinGuiaDatabase.kt     Banco Room, versão e migrações
│   ├── TransacaoBancaria.kt   Tabela de transações + enum TipoTransacao
│   ├── TransacaoDao.kt        Consultas SQL de transações
│   ├── TransacaoRepository.kt Camada entre DAO e ViewModel
│   ├── ImportadorExtrato.kt   Importa OFX/Open Finance para o banco, sem duplicar
│   ├── CategoriaCustom.kt / CategoriaDao.kt / CategoriaRepository.kt
│   ├── Investimento.kt        Tabela, DAO e repositório de investimentos
│   ├── MercadoApi.kt          Retrofit: Yahoo Finance, Banco Central, GNews
│   └── MercadoRepository.kt   Cotações e taxas (Selic, CDI, IPCA)
│
├── motor/                     ★ REGRAS PURAS EM JAVA (sem Android, com testes)
│   ├── DinheiroBR.java        Dinheiro em centavos, soma exata
│   ├── NumeroBR.java          Lê e exibe números no padrão 0.000,00
│   ├── MascaraMoeda.java      Digitação estilo app de banco
│   ├── SentidoTransacao.java  Entrada, saída ou neutro, por tipo
│   └── importacao/
│       ├── LeitorOfx.java             Extrato OFX dos bancos
│       ├── LeitorOpenFinance.java     Resposta da API de Contas do Open Finance
│       ├── ClassificadorImportacao.java  Tipo de cada lançamento importado
│       ├── LancamentoExterno.java     Lançamento lido de fora, antes de gravar
│       └── InstituicoesBR.java        Nome do banco pelo código (260 = Nubank)
│
└── ui/                        TELAS (Jetpack Compose)
    ├── FinGuiaApp.kt          Navegação entre telas (enum DestinosApp)
    ├── formato/               Atalhos Kotlin para o motor: emReais(), corDoSentido()...
    ├── home/  dashboard/  transacoes/  busca/  gerenciamento/
    ├── calculadora/  investimentos/  noticias/  notificacoes/
    ├── perfil/  configuracoes/   (tema, padrões, importação)
    └── theme/                 Paleta clara/escura, tipografia e tema

app/src/test/java/com/finguia/  Testes unitários (rodam no PC, sem celular)
web/                             FinGuia Web: visualizador do banco no navegador
```

---

## 3. Como o dado anda pelo app

```
 ┌──────────────┐   notificação   ┌──────────────────────────┐
 │ App do banco │ ──────────────▶ │ NotificationReaderService │
 └──────────────┘                 └────────────┬─────────────┘
                                               │ título + texto + hora
 ┌──────────────┐                              ▼
 │ Arquivo OFX  │                 ┌──────────────────────────┐
 │ Open Finance │──┐              │  AnalisadorNotificacao   │  transação ou descarte?
 └──────────────┘  │              └────────────┬─────────────┘
                   ▼                           │ TransacaoBancaria
        ┌────────────────────┐                 ▼
        │ ImportadorExtrato  │──▶ ┌──────────────────────────┐
        └────────────────────┘    │ TransacaoRepository      │
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
notificação que chega com o app aberto — ou um extrato importado — aparece no
extrato na hora.

Isso é o padrão **MVVM**: *Model* (Room + Repository), *ViewModel* (estado da
tela) e *View* (Composables).

---

## 4. A lógica da captura de notificações

Tudo começa em `NotificationReaderService`. Cada notificação passa por uma
sequência de filtros; se cair em qualquer um, é descartada — e o motivo vai para
o log (seção 10.2).

### Passo a passo

| # | O que acontece | Por quê |
|---|---|---|
| 1 | Checa se o pacote está em `BancoConfig.BANCOS_SUPORTADOS` | Não ler WhatsApp, Instagram etc. Só bancos (e SMS). |
| 2 | Descarta **resumo de grupo** e notificação **contínua** | O resumo diz só "3 novas movimentações"; contínua é sincronização/progresso. |
| 3 | Junta **todos** os campos de texto: `TEXT`, `BIG_TEXT`, `SUB_TEXT`, `SUMMARY_TEXT`, `INFO_TEXT`, `TEXT_LINES` | Muitos bancos põem o valor só no `BIG_TEXT` (o texto expandido). |
| 4 | **Anti-duplicata na memória**: mesmo `pacote + título + texto` em 60 s | Bancos re-postam a mesma notificação várias vezes. |
| 5 | **Anti-duplicata no banco**: mesmo texto **e** mesmo horário de postagem já gravado | Re-entrega da mesma notificação (ex.: ao reconectar). Não junta dois Pix iguais em horários diferentes. |
| 6 | `AnalisadorNotificacao.analisar()` | Decide: transação (tipo, valor, contraparte) ou descarte, com motivo. |
| 7 | Grava com o **horário de postagem** (`sbn.postTime`) e manda o broadcast `NOVA_TRANSACAO_BANCARIA` | A hora é a do banco, não a do processamento. |

### Notificações perdidas com o serviço parado

Se o Android matar o app ou o celular reiniciar, as notificações continuam na
bandeja. Ao reconectar (`onListenerConnected`), o serviço processa as que foram
**postadas depois da última que ele viu** — uma *marca d'água* guardada em
`SharedPreferences`. Isso evita dois problemas de reprocessar a bandeja inteira:
uma transação que o usuário **apagou** voltaria, e quem atualiza o app teria
**duplicatas**. Na primeira execução a marca começa em "agora".

### O que o `analisar()` decide, em ordem

1. **Propaganda** → descarta. Frases que só aparecem em anúncio (`"quite sua
   dívida"`, `"de desconto"`, `"sua oferta"`, `"saiu pra entrega"`...) descartam
   sempre. Palavras mais ambíguas (`"aproveite"`, `"oferta"`) só descartam quando
   o texto não tem uma frase específica de transação — alguns bancos colam
   propaganda no fim de uma notificação real.
2. **Tipo** pela regra da palavra mais longa (abaixo).
3. **Valor** (abaixo). **Sem valor → descarta**: "Você recebeu um Pix, abra o
   app" não tem o que registrar.
4. **Contraparte**: quem pagou ou recebeu, se o texto disser — "Pix recebido de
   Fulano", "Compra no débito em PADARIA CENTRAL", "Boleto de R$ 81,81 para
   Empresa". Nomes precisam começar com maiúscula, para não capturar "para pagar".

Toda comparação é feita no texto **sem acentos e em minúsculas**: SMS costuma
vir como "voce recebeu", e com acento não casaria.

> **Resultado medido:** das 30 notificações que o app tinha capturado em uso
> real antes dessas regras, só 6 eram transações; 18 eram propaganda e 6 eram
> lembretes de boleto. Com o analisador atual, as 6 transações continuam iguais,
> os 6 lembretes viram avisos (neutros) e as 18 propagandas são descartadas.

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
frase genérica. Quando uma frase genérica rouba um caso, a correção é
acrescentar a frase específica ao tipo certo — foi assim com *"compra aprovada
no débito"*, que caía em `"compra aprovada"` (crédito).

**Se nada casar**, entra o *fallback direcional*: se o texto só tem palavras de
entrada ("recebido", "creditado"...) vira TRANSFERENCIA_RECEBIDA; se só tem de
saída ("debitado", "compra"...) vira TRANSFERENCIA_ENVIADA; se tem as duas ou
nenhuma, fica DESCONHECIDO.

### Como o valor é extraído

Uma regex procura valores em formato brasileiro:

1. `R$` + valor com centavos → `R$ 1.500,00`, `R$250,00`
2. `R$` + valor sem centavos → `R$ 1.500`, `R$ 50`
3. valor solto **com** centavos → `1.234,56` (exigir centavos evita pegar datas como `12/05`)

Fica com o **primeiro que não seja saldo, limite ou desconto**: valor logo
depois de "saldo"/"limite", ou seguido de "de desconto", é pulado. Em *"Saldo
disponível R$ 1.200,00. Pix recebido de R$ 50,00"*, o valor gravado é 50.

### Entrada, saída ou aviso?

Quem decide o sinal é o **tipo**, não o valor (o valor é sempre positivo no
banco). A regra mora em **`SentidoTransacao`** (Java, pacote `motor`) — as telas
não repetem mais a lista. A mesma divisão está no `TransacaoDao` (SQL) e no
`web/esquema.js`:

| Entradas (somam) | Saídas (subtraem) | Neutros / avisos (não entram nos totais) |
|---|---|---|
| PIX_RECEBIDO, TRANSFERENCIA_RECEBIDA, DEPOSITO, ESTORNO | PIX_ENVIADO, COMPRA_DEBITO, COMPRA_CREDITO, BOLETO_PAGO, TRANSFERENCIA_ENVIADA, SAQUE | COBRANCA, DESCONHECIDO |

`COBRANCA` é neutra de propósito: "seu boleto vence amanhã" é um **aviso**, não
dinheiro saindo. Nas telas aparece em **cinza, sem sinal**.

---

## 5. O banco de dados

Arquivo no celular: `/data/data/com.finguia/databases/finguia_database`
(Room sobre SQLite). Versão atual: **6**.

### Tabelas

**`transacoes_bancarias`** — cada movimentação

| Coluna | Tipo | Significado |
|---|---|---|
| `id` | inteiro | Chave, gerada sozinha |
| `banco` | texto | Nome amigável ("Nubank") |
| `pacoteApp` | texto | Origem: pacote Android ("com.nu.production"), `manual`, `importado.ofx` ou `importado.openfinance` |
| `tipo` | texto | Nome do enum `TipoTransacao` |
| `valor` | real | Sempre positivo; o sinal vem do tipo |
| `descricao` | texto | Frase gerada ou digitada |
| `tituloNotificacao` / `textoNotificacao` | texto | Texto original, para auditoria e busca |
| `timestampMs` | inteiro | Data/hora em milissegundos (horário de postagem da notificação) |
| `recorrente` | 0/1 | Marcado pelo usuário como fixo do mês |
| `dataAgendada` | inteiro ou nulo | Data futura de um lançamento agendado |
| `efetivado` | 0/1 | **0 = agendado, ainda não conta nos totais** |
| `idExterno` | texto ou nulo | Id na origem de lançamentos importados (`ofx:...`, `openfinance:...`), para não importar duas vezes |

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
| 5 → 6 | coluna `idExterno` (importação de extratos) |

As migrações preservam os dados de quem já tinha o app instalado. **Nunca**
altere uma entidade sem criar a migração correspondente (receita na seção 10.5).

---

## 6. O pacote `motor` (Java)

Regras de negócio que **não dependem de Android** ficam em
`com.finguia.motor`, escritas em Java. Kotlin chama Java sem nenhuma
configuração extra, e por serem puras dá para testar tudo no PC em segundos.
Nas telas, use os atalhos de `ui/formato` (seção 8), que chamam estas classes.

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
| `moeda(valor, sigla)` | `moeda(1234.5, "US$")` → `"US$ 1.234,50"` |
| `moedaComSinal(valor)` | `10` → `"+R$ 10,00"` |
| `percentual(valor, casas)` | `12.5` → `"12,50%"` |
| `formatarFlexivel(v, min, max)` | cripto: `0.00001234` → `"0,00001234"` |
| `paraCampo(valor, casas)` | preenche campo editável sem pontos: `"1234,50"` |

> **Bugs que o `NumeroBR` corrigiu:**
> - Os campos faziam `texto.replace(",", ".").toDoubleOrNull()`. Com `"1.234,56"`
>   isso vira `"1.234.56"`, que não é número → o campo valia **0 sem avisar**.
> - Seis telas formatavam com `"%,.2f"` e trocavam vírgula e ponto. Num celular
>   em português o `format` já devolve `1.234,56`, e a troca invertia para
>   **`1,234.56`** — errado justamente para o usuário brasileiro.

### `MascaraMoeda` — digitação de app de banco

O usuário digita só números e eles entram pela direita como centavos:
`1` → `0,01` → `0,12` → `1,23` → `12,34` → `1.234,56`. Não existe dúvida entre
ponto e vírgula porque o usuário nunca digita nenhum dos dois. Usada nos campos
de valor do Lançar e da edição de transação.

### `SentidoTransacao` — entrada, saída ou neutro

`SentidoTransacao.de(tipo)` devolve `ENTRADA`, `SAIDA` ou `NEUTRO`, e
`aplicar(valor)` dá o efeito no saldo (+valor, −valor ou 0). Antes, seis telas
repetiam a lista de tipos de entrada e tratavam todo o resto como saída — e o
gráfico do Painel subtraía lembretes de boleto do saldo.

### `importacao/` — ver seção 7.

### Testes

```bash
./gradlew testDebugUnitTest --tests "com.finguia.motor.*"
```

Os testes estão em `app/src/test/java/com/finguia/`. Cada caso de uso tem um
teste com nome descritivo — ler os testes é o jeito mais rápido de entender o
que cada função garante. O projeto tem **105 testes**.

---

## 7. Importação de extratos e Open Finance

Em **Configurações → Dados → Importar extrato**, o usuário escolhe um arquivo e
o app importa os lançamentos, mostrando um resumo. Reimportar o mesmo arquivo
não duplica nada (coluna `idExterno`).

```
arquivo ──▶ ImportadorExtrato.ler()          detecta o formato pelo conteúdo
              ├─ LeitorOfx          (OFX)
              └─ LeitorOpenFinance  (JSON)
                    │ List<LancamentoExterno>   valor com sinal, id externo
                    ▼
            ClassificadorImportacao           TipoTransacao de cada um
                    ▼
            TransacaoRepository.salvarTodas   só os que ainda não existem
```

### OFX — o que funciona hoje

OFX é o formato de "exportar extrato" de praticamente todo banco brasileiro,
para conta corrente e cartão. O leitor aceita as duas variantes (1.x SGML e
2.x XML) e cuida do que os bancos daqui realmente geram:

- **Codificação** pelo cabeçalho, com Windows-1252 como padrão: lido como UTF-8,
  "débito" vira lixo.
- **Vírgula decimal** (`-1500,00`) além do ponto do padrão.
- **Data sem fuso** = horário de Brasília; **data sem hora** = meio-dia (meia-noite
  UTC apareceria como 21h do dia anterior).
- **Sem FITID**, gera um id estável a partir do conteúdo.
- **Fatura de cartão**: o lançamento positivo quase sempre é o *pagamento da
  fatura*, que vira neutro — senão o app contaria como receita um dinheiro que
  só saiu da conta corrente.

### Open Finance — o que existe e o que falta

**O app não chama as APIs do Open Finance Brasil.** Elas só atendem
participantes autorizados pelo Banco Central, com certificado ICP-Brasil, mTLS e
registro no diretório do ecossistema. Um app sem esse credenciamento não
consegue se conectar.

O que está pronto: `LeitorOpenFinance` converte a resposta da **API de Contas**
(`GET /accounts/v2/accounts/{accountId}/transactions`) em lançamentos —
`creditDebitType` dá o sinal, `LANCAMENTO_FUTURO` vira agendado, `type` (PIX,
TED, BOLETO...) ajuda a classificar. A mesma tela de importação aceita esse JSON.

Para ligar uma fonte real, o caminho é um **agregador credenciado** (Pluggy,
Belvo...), que é pago e exige contrato. Com as credenciais, falta só a chamada
de rede que entrega esse JSON; o mapeamento, a classificação e a deduplicação já
estão prontos e testados.

### Como o tipo de um lançamento importado é decidido

`ClassificadorImportacao`: o **sinal manda na direção**; a categoria da origem
(`TRNTYPE` do OFX: ATM, POS, PAYMENT...; `type` do Open Finance) e o histórico
do extrato ("PIX", "PAGTO", "SAQUE"...) refinam o tipo. Uma pista que contradiz
o sinal perde para ele — um teste percorre todas as combinações para garantir.

---

## 8. Aparência, formato de números e padrões

### Tema claro e escuro

Configurações → Aparência: **Escuro** (padrão), **Claro** ou **Seguir o sistema**.

As telas pintam com tokens (`DarkBg`, `CardBg`, `GrayText`, `TextoForte`,
`MoneyGreen`, `DebtRed`...) definidos em `ui/theme/Color.kt`. Eles **não são
cores fixas**: leem a paleta ativa (`ui/theme/Paleta.kt`), publicada pelo
`FinGuiaTheme`. Por isso trocar o tema redesenha tudo sem mexer em cada tela.

Regras para quem escreve tela nova:

- Texto sobre o fundo ou cartão: `TextoForte` (nunca `Color.White` fixo — some no tema claro).
- Texto sobre botão sólido roxo/verde: aí sim `Color.White`.
- Esses tokens só podem ser lidos dentro de `@Composable`. Dentro de um
  `Canvas { }` o bloco de desenho não é composable: leia a cor numa `val` antes.

### Formato de números

Nas telas, use os atalhos de `ui/formato/FormatoBR.kt` e `Sentido.kt`:

```kotlin
total.emReais()                  // "R$ 1.234,56"
taxa.emPercentual(2)             // "10,75%"
campo.lerNumeroBR() ?: 0.0       // aceita 1.234,56
corDoSentido(tipo.sentido)       // verde, vermelho ou cinza
prefixoDoSentido(tipo.sentido)   // "+", "-" ou ""
```

Campo de valor em dinheiro: `visualTransformation = MascaraMoedaBR`, estado com
`digitosMoeda(it)` e leitura com `MascaraMoeda.reais(estado)`.

### Padrões configuráveis

Configurações → Padrões: tela ao abrir o app, aba inicial do Lançar, direção do
lançamento avulso, calculadora que abre primeiro e nome da conta dos
lançamentos manuais. Ficam em `PadroesApp` (`ConfiguracoesViewModel.kt`); os
valores iniciais reproduzem o comportamento antigo.

---

## 9. FinGuia Web

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
- **Login simulado:** a página abre pedindo usuário e senha. Conta de
  demonstração: `demo` / `finguia`. As contas ficam só no navegador (senha em
  hash SHA-256 com sal); é demonstração, não segurança. O botão **Sair** volta
  ao login.
- **Dashboards com templates:** cada usuário cria quantos quiser, escolhendo
  entre Completo, Resumo, Gastos e Investimentos. Como criar um template novo
  está no [`web/README.md`](web/README.md#dashboards-e-templates).
- **Testes:** dentro da pasta `FinGuia`: `cd web/testes`, `npm install`,
  `npm test` (jsdom) e `npm run test:navegador` (Firefox real).

---

## 10. Tutorial: como consertar

### 10.1 Antes de tudo

```bash
git switch desenvolvimento && git pull     # trabalhe sempre em desenvolvimento
./gradlew testDebugUnitTest                # testes (1–2 min, sem celular)
./gradlew assembleDebug                    # gera o APK (ou use build_apk.bat)
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

### 10.2 "O app não pegou a notificação do meu banco"

Com o celular no cabo e depuração USB ligada:

```bash
adb logcat -s FinGuia-Notif
```

Faça uma transação e leia o log. Cada situação tem uma causa:

| O log mostra | Causa | Conserto |
|---|---|---|
| **Nada** | Permissão desligada ou pacote não cadastrado | No Android: Configurações → Apps → Acesso especial → Acesso a notificações → FinGuia ligado. Se estiver ok, siga 10.3 |
| `Ignorada — resumo de grupo` / `notificação contínua` | Normal: não é a notificação da transação | Nada a fazer |
| `Ignorada — duplicata recente` / `já gravada` | A mesma notificação chegou de novo | Nada a fazer |
| `Descartada — propaganda ("...")` | Uma frase de anúncio casou | Se era transação de verdade, a frase entre aspas é genérica demais: ajuste as listas `PROMOCIONAL_*` |
| `Descartada — sem valor` | Tipo reconhecido, mas o texto não tem `R$` | O banco não manda o valor na notificação; use a importação de OFX |
| `Descartada — sem tipo nem valor` | Frase do banco não reconhecida | Siga 10.4 com o texto do log |
| `Transação salva` com tipo errado | Palavra-chave de outro tipo venceu | Siga 10.4 |

**Testar sem esperar uma transação real:** no emulador, o app de SMS está na
lista de bancos. Mande um SMS simulado:

```bash
adb emu sms send 11999990000 "BANCO: Voce recebeu um Pix de R$ 12,34 de Fulano"
```

### 10.3 Adicionar um banco novo

1. Descubra o pacote: `adb shell pm list packages | findstr -i nomedobanco`
2. Adicione uma linha em `service/BancoConfig.kt`:
   ```kotlin
   "br.com.novobanco.app"          to "Novo Banco",
   ```
3. Recompile e teste com uma transação real.

### 10.4 Ensinar uma frase nova ao analisador

1. Copie o texto exato do log (seção 10.2).
2. **Escreva o teste primeiro** em `app/src/test/java/com/finguia/service/AnalisadorNotificacaoTest.kt`,
   com um texto no mesmo padrão (troque nomes e valores reais por fictícios).
   Rode e veja falhar.
3. Em `service/AnalisadorNotificacao.kt`, ache o tipo certo em `PALAVRAS_TIPO`
   e acrescente a frase, o mais específica possível:
   ```kotlin
   TipoTransacao.PIX_RECEBIDO to listOf(
       ...,
       "pix caiu na sua conta"      // ← nova
   ),
   ```
4. Lembre da regra: **a mais longa vence**. Frase curta e genérica ("pix")
   rouba casos de outros tipos. Prefira frases de 3 ou mais palavras.
5. Rode todos os testes: a frase nova não pode quebrar os casos antigos.

### 10.5 Adicionar uma coluna no banco (migração)

Exemplo: adicionar `observacao` em transações.

1. **Entidade** (`TransacaoBancaria.kt`) — campo com valor padrão:
   ```kotlin
   val observacao: String = ""
   ```
2. **Versão** (`FinGuiaDatabase.kt`) — `version = 6` vira `version = 7`.
3. **Migração** no `companion object`:
   ```kotlin
   private val MIGRACAO_6_7 = object : Migration(6, 7) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL(
               "ALTER TABLE transacoes_bancarias ADD COLUMN observacao TEXT NOT NULL DEFAULT ''"
           )
       }
   }
   ```
4. **Registrar**: `.addMigrations(MIGRACAO_1_2, ..., MIGRACAO_5_6, MIGRACAO_6_7)`
5. **Web**: se a coluna for exibida no navegador, atualize `web/esquema.js` e `web/banco.js`.
6. Teste instalando **por cima** da versão anterior (não desinstale) — é assim
   que se descobre migração quebrada antes do usuário. Para conferir a versão
   do banco no aparelho:
   ```bash
   adb exec-out run-as com.finguia cat databases/finguia_database > banco.db
   sqlite3 banco.db "PRAGMA user_version;"
   ```

> Esqueceu a migração? O app fecha ao abrir com
> `IllegalStateException: A migration from 6 to 7 was required but not found`.

### 10.6 Uma importação classificou errado

1. Abra o arquivo OFX num editor de texto e ache o bloco `<STMTTRN>` do
   lançamento: veja `TRNTYPE`, `TRNAMT` e `MEMO`.
2. Escreva o caso em `ClassificadorImportacaoTest.java` e veja falhar.
3. Ajuste `pelaDescricao()` (palavras do histórico) ou `pelaCategoria()`
   (`TRNTYPE`/`type`) em `ClassificadorImportacao.java`.

### 10.7 Adicionar um padrão nas Configurações

1. Novo campo em `PadroesApp` (`ConfiguracoesViewModel.kt`) com o valor que
   reproduz o comportamento atual.
2. Leia e grave em `lerPadroes()` / `salvar()`, com uma chave nova.
3. Um `definirX()` no ViewModel e uma linha `ItemLista(...)` em `TelaConfiguracoes.kt`.
4. Na tela que usa: `val padroes by configuracoes.padroes.collectAsState()`.

### 10.8 Quando um teste falhar

O relatório abre no navegador:
`app/build/reports/tests/testDebugUnitTest/index.html`.
Ele mostra qual caso falhou, o valor esperado e o obtido.

---

## 11. Tutorial: como apresentar

### 11.1 Checklist do dia anterior

- [ ] APK da branch **`producao`** instalado no celular de demonstração
- [ ] Permissão de acesso a notificações **ligada**
- [ ] Algumas transações reais no extrato (faça 2 ou 3 Pix pequenos antes)
- [ ] Um segundo celular ou pessoa para mandar um Pix **ao vivo**
- [ ] Um extrato OFX de exemplo na pasta Downloads do celular (para a demo de importação)
- [ ] FinGuia Web com o banco já exportado (`web\exportar-banco.bat`), como plano B se o celular falhar
- [ ] Tela do celular espelhada no projetor (ex.: `scrcpy`)
- [ ] Modo não perturbe ligado **para outros apps** — só o banco pode notificar

### 11.2 Roteiro de 10 minutos

| Tempo | O que mostrar | O que falar |
|---|---|---|
| 0–1 min | Tela inicial | **O problema:** ninguém anota cada gasto. A planilha morre na segunda semana. |
| 1–4 min | **Pix ao vivo** → notificação → extrato atualiza sozinho | **A solução:** o app lê a notificação do banco e registra sem o usuário digitar nada. *Este é o momento mais forte: deixe a plateia ver acontecer.* |
| 4–5 min | Extrato: um lembrete de boleto em cinza | O app separa dinheiro que mexeu de aviso. Propaganda nem entra. |
| 5–6 min | Importar um OFX | Histórico antigo em segundos, sem duplicar se importar de novo. |
| 6–7 min | Calculadoras e Investimentos | Taxas reais do Banco Central (Selic, CDI, IPCA) e cotações do Yahoo Finance. |
| 7–8 min | Configurações: trocar para o tema claro | Aparência e padrões do usuário, na hora. |
| 8–10 min | Slide de arquitetura (diagrama da seção 3) | MVVM, Room, Flow reativo, regras em Java puro, 105 testes. |

### 11.3 Frases que explicam bem

- *"O app não pede senha de banco nenhum. Ele só lê o que já aparece na sua tela de notificações."*
- *"Os dados nunca saem do celular. Não temos servidor."*
- *"A tela não é mandada atualizar: ela observa o banco de dados e redesenha sozinha."*
- *"Dinheiro é guardado em centavos inteiros, porque somar números com vírgula no computador acumula erro."*
- *"Das 30 notificações que o protótipo capturava, só 6 eram transações. Medimos, corrigimos e hoje as 24 de ruído ficam de fora."*

### 11.4 Perguntas prováveis da banca

**"E se o banco mudar o texto da notificação?"**
O analisador tem fallback direcional (seção 4): mesmo sem reconhecer a frase,
palavras como "recebido" ou "debitado" ainda classificam a direção. E incluir
uma frase nova é uma linha de código com um teste (seção 10.4).

**"E se o app estiver fechado quando o Pix chegar?"**
O Android reinicia o serviço de notificações; ao reconectar, ele recupera as
notificações que ficaram na bandeja e grava com o horário original.

**"Não é inseguro ler notificações?"**
É uma permissão do próprio Android, que o usuário concede explicitamente e pode
revogar. O app só processa pacotes da lista de bancos (`BancoConfig`) e não
envia nada para a internet.

**"Por que não usam Open Finance?"**
O Open Finance Brasil exige que a empresa seja participante autorizada pelo
Banco Central, com certificados ICP-Brasil e conexão mTLS. Um app sem esse
credenciamento não pode chamar as APIs dos bancos. O FinGuia já tem o conversor
do formato oficial da API de Contas pronto e testado (seção 7); ligar a um
agregador credenciado, como Pluggy ou Belvo, exige só o contrato e a chamada de
rede. Enquanto isso, a leitura de notificações e a importação de OFX entregam o
resultado prático.

**"Por que parte do código está em Java?"**
As regras puras (dinheiro, números, sentido das transações, leitura de OFX e
Open Finance) não dependem do Android. Isoladas assim, rodam em testes no PC em
segundos. Kotlin e Java convivem no mesmo projeto sem configuração extra.

**"Como garantem que o cálculo está certo?"**
Testes unitários em `app/src/test` (105). Mostre o relatório verde de
`./gradlew testDebugUnitTest`.

### 11.5 Se algo der errado ao vivo

| Problema | Saída |
|---|---|
| Pix não chegou | Mostre uma transação que já estava capturada e o texto original dela no detalhe |
| App travou | Abra o FinGuia Web com o banco exportado e entre com `demo` / `finguia` |
| Sem internet | Captura, extrato e importação funcionam offline; só cotações e notícias dependem de rede |

---

## 12. Limitações conhecidas

Coisas que funcionam, mas têm espaço para melhorar. Bom saber antes que alguém pergunte.

- **Capturas antigas continuam no banco.** As regras novas de descarte valem
  para notificações daqui para frente. Propagandas e lançamentos de R$ 0,00
  gravados por versões anteriores precisam ser apagados à mão no Extrato.
- **Dois Pix idênticos em menos de 60 s** (mesmo valor, mesma pessoa, mesmo
  texto): o segundo é tomado como re-postagem e ignorado.
- **Recuperação de perdidas** só alcança notificações que ainda estão na
  bandeja; se o usuário as dispensou com o serviço parado, não há como recuperar.
- **Importar conta e cartão do mesmo período** conta os gastos duas vezes: as
  compras aparecem no OFX do cartão e o pagamento da fatura aparece no OFX da
  conta. Importe um dos dois, ou apague o pagamento da fatura.
- **Open Finance** sem conexão direta (seção 7).
- **Contraparte** depende do texto do banco seguir um dos padrões conhecidos;
  quando não segue, a descrição fica sem o nome.
- **`FUNCOES.md`** tem trechos de versões antigas (ver aviso no topo).
