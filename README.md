# FinGuia — Gestor Financeiro Inteligente

FinGuia é um aplicativo Android de gestão financeira pessoal que automatiza o acompanhamento da sua vida financeira por meio da **captura automática de notificações bancárias**. Sem digitar nada: o app lê as notificações dos seus bancos e registra cada transação automaticamente.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Stack Tecnológica](#stack-tecnológica)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Sistema de Captura de Notificações Bancárias](#sistema-de-captura-de-notificações-bancárias)
- [Bancos Suportados](#bancos-suportados)
- [Configuração e Instalação](#configuração-e-instalação)
- [Permissões Necessárias](#permissões-necessárias)
- [FinGuia Web](#finguia-web)
- [Branches e Fluxo de Trabalho](#branches-e-fluxo-de-trabalho)
- [Guia de Desenvolvimento](DESENVOLVIMENTO.md)
- [Roadmap](#roadmap)

---

## Visão Geral

O FinGuia resolve o principal problema da gestão financeira: **o esquecimento e a falta de registro**. A maioria das pessoas não anota cada gasto no momento em que acontece. O FinGuia elimina essa barreira ao capturar automaticamente as notificações dos aplicativos bancários instalados no dispositivo, classificar cada transação e exibi-la em um extrato consolidado e em tempo real.

```
Você recebe uma notificação do Nubank → FinGuia captura → Classifica como "Pix Recebido" → Salva no banco de dados → Atualiza o extrato automaticamente
```

---

## Funcionalidades

### Implementadas

| Funcionalidade | Descrição |
|---|---|
| **Captura Automática de Notificações** | Lê notificações de 40+ bancos e fintechs em segundo plano, e recupera as que chegaram com o app fechado |
| **Filtro de Ruído** | Descarta propaganda (ofertas, renegociação, SMS de operadora) e notificações sem valor |
| **Classificação de Transações** | Identifica tipo: Pix, compra, boleto, transferência, cobrança, saque, etc., e quem pagou ou recebeu |
| **Extração de Valores** | Valores em formato BR (R$ 1.250,00), ignorando saldo e limite no mesmo texto |
| **Importação de Extrato** | Arquivos OFX dos bancos e respostas da API de Contas do Open Finance, sem duplicar |
| **Extrato Consolidado** | Todas as transações, com avisos (lembretes de boleto) separados de entradas e saídas |
| **Resumo Financeiro** | Totais de entradas e saídas, gastos por categoria e fluxo de caixa |
| **Lançamento Manual** | Avulso, recorrente e agendado, com digitação de valor estilo app de banco |
| **Calculadoras** | Conversão de moedas, financeira, científica, investimentos, markup e empréstimos |
| **Investimentos** | Carteira com cotações (Yahoo Finance) e taxas do Banco Central (Selic, CDI, IPCA) |
| **Busca e Metas** | Busca no extrato e metas de economia no perfil |
| **Tema Claro e Escuro** | Escolha em Configurações, ou seguindo o sistema |
| **Padrões Configuráveis** | Tela inicial, aba do Lançar, calculadora e conta dos lançamentos manuais |
| **Padrão Brasileiro** | Todos os números em 0.000,00, independente do idioma do celular |
| **FinGuia Web** | Visualizador do banco de dados no navegador ([detalhes](#finguia-web)) |

### Próximos Passos

- Conexão com um agregador credenciado de Open Finance (o conversor do formato já existe; ver [Guia de Desenvolvimento](DESENVOLVIMENTO.md#7-importação-de-extratos-e-open-finance))
- Limpeza assistida de capturas antigas gravadas antes do filtro de propaganda

---

## Arquitetura

O projeto segue o padrão **MVVM (Model-View-ViewModel)** com separação clara de responsabilidades:

```
┌──────────────────────────────────────┐
│              UI Layer                │
│  (Jetpack Compose — Telas/Screens)   │
└────────────────┬─────────────────────┘
                 │ observa StateFlow
┌────────────────▼─────────────────────┐
│           ViewModel Layer            │
│  (TransacaoViewModel, CriptoViewModel)│
└────────────────┬─────────────────────┘
                 │ chama
┌────────────────▼─────────────────────┐
│          Repository Layer            │
│  (TransacaoRepository, CriptoApi)    │
└────────┬───────────────┬─────────────┘
         │               │
┌────────▼──────┐  ┌─────▼───────────┐
│  Room Database│  │  Retrofit API   │
│ (local SQLite)│  │  (CoinGecko,    │
└───────────────┘  │   futuros)      │
                   └─────────────────┘

┌──────────────────────────────────────┐
│         Service Layer                │
│  NotificationListenerService         │
│  ├── BancoConfig (pacotes conhecidos)│
│  └── AnalisadorNotificacao (parser)  │
└──────────────────────────────────────┘
```

---

## Stack Tecnológica

| Categoria | Tecnologia | Versão |
|---|---|---|
| Linguagem | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Navegação | NavigationSuiteScaffold (adaptativa) | - |
| ViewModel | Lifecycle ViewModel Compose | 2.8.7 |
| Banco de Dados | Room (SQLite) | 2.6.1 |
| Processamento Assíncrono | Kotlin Coroutines + Flow | - |
| HTTP Client | Retrofit 2 + Gson | 2.11.0 |
| Build | Gradle Kotlin DSL + KSP | AGP 8.7.3 |
| Min SDK | Android 7.0 (API 24) | - |
| Target SDK | Android 16 (API 36) | - |

---

## Estrutura do Projeto

```
app/src/main/java/com/finguia/
│
├── MainActivity.kt                    # Ponto de entrada da Activity
│
├── dados/                             # Camada de dados
│   ├── CriptoApi.kt                   # API CoinGecko (Retrofit)
│   ├── TransacaoBancaria.kt           # Entidade Room + enum TipoTransacao
│   ├── TransacaoDao.kt                # DAO com queries reativas (Flow)
│   ├── FinGuiaDatabase.kt             # Singleton Room Database
│   └── TransacaoRepository.kt        # Repositório de transações
│
├── service/                           # Serviços de background
│   ├── NotificationReaderService.kt   # NotificationListenerService principal
│   ├── BancoConfig.kt                 # Mapa de 40+ bancos suportados
│   └── AnalisadorNotificacao.kt       # Parser de tipo e valor das notificações
│
└── ui/
    ├── FinGuiaApp.kt                  # Raiz da navegação (enum DestinosApp)
    ├── cripto/
    │   ├── CriptoViewModel.kt         # ViewModel das criptomoedas
    │   └── TelaCripto.kt             # Tela de cotações
    ├── dashboard/
    │   └── TelaDash.kt               # Tela de dashboard (em desenvolvimento)
    ├── home/
    │   ├── TelaHome.kt               # Tela inicial com resumo e atalhos
    │   └── TelaHomeDash.kt           # Visão de fluxo mensal
    ├── transacoes/
    │   ├── TransacaoViewModel.kt      # ViewModel do extrato bancário
    │   └── TelaTransacoes.kt         # Tela de extrato com todas as transações
    └── theme/
        ├── Color.kt                   # Paleta de cores do app
        ├── Theme.kt                   # Material 3 Theme
        └── Type.kt                    # Tipografia
```

---

## Sistema de Captura de Notificações Bancárias

Esta é a funcionalidade central e diferencial do FinGuia.

### Como funciona

```
1. Usuário concede permissão de acesso a notificações (uma única vez)
         ↓
2. NotificationReaderService fica ativo em segundo plano
         ↓
3. Qualquer notificação chega ao dispositivo
         ↓
4. BancoConfig verifica se o pacote do app é um banco conhecido
   └─ Não reconhecido → descarta silenciosamente
   └─ Reconhecido → continua
         ↓
5. AnalisadorNotificacao analisa título + texto da notificação
   ├── identificarTipo()  → Pix Recebido, Compra Crédito, Boleto, etc.
   └── extrairValor()     → R$ 1.250,00 → 1250.00
         ↓
6. Transação com tipo DESCONHECIDO e valor 0.0 → descartada
         ↓
7. TransacaoBancaria criada e salva no Room Database
         ↓
8. Broadcast enviado → UI atualiza em tempo real via StateFlow
```

### Tipos de Transação Detectados

| Tipo | Exemplos de Keywords Detectadas |
|---|---|
| `PIX_RECEBIDO` | "pix recebido", "você recebeu um pix", "pix de" |
| `PIX_ENVIADO` | "pix enviado", "pix realizado", "pix para" |
| `COMPRA_CREDITO` | "compra no crédito", "crédito aprovado", "parcelado em" |
| `COMPRA_DEBITO` | "compra no débito", "débito aprovado" |
| `BOLETO_PAGO` | "boleto pago", "pagamento efetuado", "conta quitada" |
| `TRANSFERENCIA_RECEBIDA` | "TED recebido", "crédito em conta", "transferência creditada" |
| `TRANSFERENCIA_ENVIADA` | "TED enviado", "transferência realizada" |
| `ESTORNO` | "estorno", "devolução", "reembolso", "chargeback" |
| `SAQUE` | "saque realizado", "retirada em espécie" |
| `DEPOSITO` | "depósito em conta", "crédito por depósito" |
| `COBRANCA` | "fatura vencendo", "débito automático", "mensalidade" |

### Extração de Valores

O parser usa regex para capturar valores monetários em qualquer formato brasileiro:

```
"Pix recebido R$ 1.500,00 de João" → 1500.00
"Compra de R$89,90 aprovada"       → 89.90
"Débito de 250,00 efetuado"        → 250.00
```

---

## Bancos Suportados

### Bancos Tradicionais
Banco do Brasil, Itaú, Itaú Empresas, Bradesco, Santander, Caixa Econômica, Sicoob, Sicredi, Banrisul, Ailos, Banco Modal, Banco Original, Banco da Amazônia, Bancoob, HSBC

### Fintechs e Bancos Digitais
Nubank, Banco Inter, C6 Bank, Next, Neon, Agibank, Sofisa Direto, Dindin, Will Bank, BS2, Digio, Realize Bank, BV, Cred System, Banco Votorantim, Stone, Boa Compra

### Carteiras Digitais e Pagamentos
PicPay, Mercado Pago, PagBank, PayPal, Getnet, Cielo, Rede

### Corretoras e Investimentos
XP, Rico, Clear, Nuinvest, Avenue, Toro

> Novos bancos podem ser adicionados facilmente em `BancoConfig.kt` inserindo o `packageName` do app e o nome amigável.

---

## Configuração e Instalação

### Pré-requisitos

- Android Studio Ladybug ou superior
- JDK 21
- Android SDK 36

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/DevFLeo/FinGuia.git
cd FinGuia

# 2. Abra no Android Studio
# File → Open → selecione a pasta do projeto

# 3. Sincronize as dependências
# O Android Studio fará o sync automático com o Gradle

# 4. Execute no dispositivo ou emulador
# Run → Run 'app' (Shift+F10)
```

---

## FinGuia Web

Visualizador do banco de dados do app no navegador. Abre o mesmo arquivo SQLite
que o Room grava no aparelho (`finguia_database`) e mostra extrato, fluxo mensal,
totais por banco, lançamentos agendados e investimentos — útil para conferir os
dados no computador e para apresentar o projeto sem depender do celular.

```bat
web\exportar-banco.bat   :: copia o banco do aparelho via adb
web\finguia-web.bat      :: abre o visualizador em http://127.0.0.1:8080
```

O arquivo é lido dentro do navegador, com SQLite compilado para WebAssembly
(sql.js, versionado em `web/vendor`). Nada é enviado para servidor nenhum e não
é preciso internet. Detalhes e manutenção em [`web/README.md`](web/README.md).

---

## Branches e Fluxo de Trabalho

O repositório usa três branches fixas:

| Branch | Para que serve | Quem mexe |
|---|---|---|
| `producao` | Código estável, que vai para o APK entregue e para a apresentação. Só recebe merge de `desenvolvimento` depois de testado. | Ninguém faz commit direto |
| `desenvolvimento` | Onde o trabalho do dia a dia acontece. Recebe commits e branches de funcionalidade. | Toda a equipe |
| `main` | Branch padrão do GitHub/GitLab. Acompanha `producao`. | Ninguém faz commit direto |

### Fluxo do dia a dia

```bash
# 1. Comece sempre atualizado
git switch desenvolvimento
git pull

# 2. Trabalhe e commite (uma melhoria por commit)
git add <arquivos>
git commit -m "Feat: descreve a melhoria"

# 3. Envie para os dois remotos
git push origin desenvolvimento   # GitLab
git push github desenvolvimento   # GitHub
```

### Publicar uma versão estável

Só depois de `./gradlew testDebugUnitTest` passar e o app ser testado no aparelho:

```bash
git switch producao
git merge --ff-only desenvolvimento
git switch main
git merge --ff-only producao
git push origin producao main
git push github producao main
git switch desenvolvimento
```

O `--ff-only` garante que `producao` só avança para commits que já existem em
`desenvolvimento` — se o comando recusar, alguém commitou direto em `producao`.

### Remotos

| Nome | Endereço |
|---|---|
| `origin` | https://gitlab.com/DevFLeo/FinGuia |
| `github` | https://github.com/DevFLeo/FinGuia |

Quem clonou só de um lugar adiciona o outro com
`git remote add github https://github.com/DevFLeo/FinGuia` (ou `origin` para o GitLab).

Para entender a lógica do app, as funções e como apresentar o projeto, veja
o [Guia de Desenvolvimento](DESENVOLVIMENTO.md).

---

## Permissões Necessárias

### Permissão de Internet (automática)
Concedida automaticamente pelo sistema. Necessária para cotações de criptomoedas.

### Permissão de Acesso a Notificações (manual — obrigatória)
Esta permissão **não pode ser solicitada via código** — o usuário deve concedê-la manualmente:

1. Abra o FinGuia
2. Vá em **Configurações do dispositivo** → **Privacidade** → **Acesso a Notificações** (ou "Acesso especial ao app" dependendo do fabricante)
3. Encontre **FinGuia** na lista
4. Ative a permissão

Sem essa permissão, o serviço de captura não funcionará. O app deve orientar o usuário a conceder essa permissão na primeira abertura.

> **Privacidade:** O FinGuia processa as notificações localmente no dispositivo. Nenhum dado é enviado para servidores externos. Apenas notificações de aplicativos bancários reconhecidos são processadas; todas as demais são ignoradas imediatamente.

---

## Roadmap Beta

### v1.1 — Onboarding e Permissões
- [ ] Tela de boas-vindas explicando o funcionamento
- [ ] Detecção automática se a permissão de notificação está ativa
- [ ] Deep link direto para a tela de permissão do sistema
- [ ] Tutorial interativo das funcionalidades

### v1.2 — Extrato e Categorias
- [ ] Categorização automática por tipo de gasto (alimentação, transporte, etc.)
- [ ] Filtros por banco, período e tipo de transação
- [ ] Busca no extrato
- [ ] Edição manual de transações capturadas

### v1.3 — Análises e Metas
- [ ] Gráficos de evolução mensal de gastos
- [ ] Comparativo entre meses
- [ ] Metas de economia por categoria
- [ ] Alertas de gastos excessivos

### v1.4 — Lançamentos Manuais
- [ ] Formulário para lançar receitas/despesas manualmente
- [ ] Suporte a transações recorrentes (aluguel, salário)
- [ ] Importação de extratos CSV/OFX

### v2.0 — Inteligência Financeira
- [ ] Classificação automática por IA (NLP local)
- [ ] Relatórios mensais automatizados
- [ ] Previsão de saldo futuro
- [ ] Sugestões personalizadas de economia

---

## Paleta de Cores

| Nome | Hex | Uso |
|---|---|---|
| `GojoPurple` | `#7D5FFF` | Cor primária, destaques |
| `MoneyGreen` | `#2ECC71` | Receitas, lucros, positivo |
| `DebtRed` | `#FF4D4D` | Despesas, dívidas, negativo |
| `DarkBg` | `#0A0A0C` | Fundo principal |
| `CardBg` | `#16161E` | Fundo de cards |
| `GrayText` | `#888888` | Textos secundários |

---

## Licença

Projeto proprietário — todos os direitos reservados.
