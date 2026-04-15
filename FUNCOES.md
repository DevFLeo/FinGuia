# FinGuia — Documentação Interna de Funções

> Tutorial interno da equipe. Use como referência rápida na hora de adicionar código novo ou montar o TCC.

---

## Navegação / Estrutura Principal

### `FinGuiaApp.kt`
| Função / Composable | O que faz |
|---|---|
| `FinGuiaApp()` | Composable raiz. Gerencia a navegação entre as 6 telas usando `NavigationSuiteScaffold`. |
| `DestinosApp` (enum) | Define os destinos de navegação: Início, Painel, Lançar, Extrato, Criptos, Tema. Cada entrada tem rótulo (`rotulo`) e ícone (`icone`). |

### `MainActivity.kt`
| Função / Método | O que faz |
|---|---|
| `onCreate()` | Ponto de entrada do app. Ativa edge-to-edge e inicializa o Compose com `FinGuiaApp` dentro do `FinGuiaTheme`. |

---

## Telas (UI)

### `TelaHome.kt`
| Função / Composable | O que faz |
|---|---|
| `telaHome()` | Tela principal. Exibe 3 botões de ícone no topo (Configurações, Notificações, Perfil), gráfico de rosca com saldo total, grid de 6 atalhos via `BotaoAcao` (Lançar, Editar, Painel, Calculadora, Metas, Busca) e dois botões destacados separados: Criptomoedas e Minhas Contas. |
| `BotaoIconeTopo()` | Botão de ícone com cantos arredondados para a barra superior. |
| `BotaoAcao()` | Card de ação com ícone e label. Usado no grid de atalhos da home. |
| `GraficoRosca()` | Gráfico de rosca desenhado no Canvas. Recebe uma porcentagem e exibe o progresso colorido. |

### `TelaDash.kt`
| Função / Composable | O que faz |
|---|---|
| `HomeScreen()` | Tela de dashboard simples com mensagem de boas-vindas e botão para ver as movimentações do dia. |

### `TelaHomeDash.kt`
| Função / Composable | O que faz |
|---|---|
| `TelaHomeDash()` | Tela de inteligência financeira. Exibe gráfico de linha e resumo mensal de entradas (+R$8.000) e saídas (-R$2.760). |
| `LineChartPlaceholder()` | Gráfico de linha desenhado no Canvas usando curvas de Bézier cúbicas. Usa a cor primária do tema. |

### `TelaCripto.kt`
| Função / Composable | O que faz |
|---|---|
| `TelaCripto()` | Tela de criptomoedas. Gerencia os estados: carregando, erro ou lista de criptos com preço e variação 24h. |
| `CartaoCripto()` | Card individual de cripto exibindo nome, símbolo, preço e ícone de tendência colorido. |

### `TelaTransacoes.kt`
| Função / Composable | O que faz |
|---|---|
| `TelaTransacoes()` | Tela de extrato. Mostra cards de resumo (receitas/despesas) e lista lazy de todas as transações com opção de deletar. |
| `ResumoCard()` | Card de resumo com valor total, ícone e cor. |
| `CartaoTransacao()` | Card de uma transação: banco, descrição, horário, valor e botão de deletar. |
| `EstadoVazio()` | Tela de estado vazio exibida quando não há transações cadastradas. |
| `iconeParaTipo()` | Mapeia o enum `TipoTransacao` para o ícone Material correspondente. |
| `formatarValor()` | Formata um Double para o formato de Real brasileiro (ex: R$ 1.234,56). |

### `TelaLancar.kt`
| Função / Composable | O que faz |
|---|---|
| `TelaLancar()` | Tela de lançamento com três abas: Lançar, Recorrente e Notificação. |
| `BarraAbas()` | Componente de abas com seleção por clique. |
| `AbaLancar()` | Aba para lançar receitas (Freela, Esporádico, Salário) e despesas (Contas, Emergência, Alimentação, etc.). |
| `AbaRecorrente()` | Aba para gerenciar transações recorrentes: salário fixo e contas fixas. |
| `AbaNotificacao()` | Aba para configurar alertas de notificação e editar palavras-chave de detecção. |
| `TituloSecao()` | Componente de título de seção com ícone e texto colorido. |
| `CardGanho()` | Card de receita/ganho com ícone e labels. |
| `CardDivida()` | Card de despesa/dívida com ícone e labels. |
| `CardRecorrente()` | Card de transação recorrente com ícone, labels e borda colorida. |

---

## Dados (Banco de Dados Local)

### `TransacaoBancaria.kt`
| Classe / Enum | O que faz |
|---|---|
| `TipoTransacao` (enum) | Lista todos os tipos de transação: `PIX_RECEBIDO`, `PIX_ENVIADO`, `COMPRA_DEBITO`, `COMPRA_CREDITO`, `BOLETO_PAGO`, `TRANSFERENCIA_RECEBIDA`, `TRANSFERENCIA_ENVIADA`, `COBRANCA`, `ESTORNO`, `SAQUE`, `DEPOSITO`, `DESCONHECIDO`. |
| `TransacaoBancaria` (entity) | Entidade Room representando uma transação: `id`, `banco`, `pacoteApp`, `tipo`, `valor`, `descricao`, `tituloNotificacao`, `textoNotificacao`, `timestampMs`. |

### `TransacaoDao.kt`
| Função / Método | O que faz |
|---|---|
| `inserir()` | Insere uma transação no banco. Ignora conflitos (não duplica). |
| `listarTodas()` | Retorna `Flow` com todas as transações ordenadas por data (mais recente primeiro). |
| `listarReceitas()` | Retorna `Flow` apenas com transações de entrada/receita. |
| `listarDespesas()` | Retorna `Flow` apenas com transações de saída/despesa. |
| `totalReceitas()` | Retorna `Flow` com a soma total de todas as receitas. |
| `totalDespesas()` | Retorna `Flow` com a soma total de todas as despesas. |
| `deletar()` | Deleta uma transação pelo id. |
| `deletarTodas()` | Apaga todas as transações do banco. |

### `FinGuiaDatabase.kt`
| Função / Método | O que faz |
|---|---|
| `transacaoDao()` | Retorna a instância do DAO de transações. |
| `obterInstancia()` | Singleton thread-safe do banco Room. Garante que só existe uma instância do banco no app. |

### `TransacaoRepository.kt`
| Função / Método | O que faz |
|---|---|
| `salvar()` | Salva uma transação no banco via DAO. |
| `listarTodas()` | Retorna `Flow` de todas as transações. |
| `listarReceitas()` | Retorna `Flow` de receitas. |
| `listarDespesas()` | Retorna `Flow` de despesas. |
| `totalReceitas()` | Retorna `Flow` com total de receitas. |
| `totalDespesas()` | Retorna `Flow` com total de despesas. |
| `deletar()` | Deleta uma transação pelo id. |

---

## Dados (API de Criptomoedas)

### `CriptoApi.kt`
| Classe / Objeto | O que faz |
|---|---|
| `Cripto` (data class) | Representa uma criptomoeda: id, símbolo, nome, preço atual, variação 24h e URL da imagem. |
| `CriptoApiServico` (interface) | Interface Retrofit com `buscarCriptos()` que consulta a API CoinGecko. |
| `CriptoRetrofit` (object) | Singleton lazy que fornece a instância do Retrofit configurada para a API de cripto. |

---

## ViewModels

### `CriptoViewModel.kt`
| Função / Método | O que faz |
|---|---|
| `buscarCriptos()` | Chama a API CoinGecko, filtra moedas bloqueadas (`IDS_BLOQUEADOS`) e atualiza o `StateFlow` com o resultado. Chamada automaticamente no `init` a cada 30 segundos. |
| `EstadoCripto` (sealed class) | Estados da tela de cripto: `Carregando`, `Sucesso(lista)`, `Erro(mensagem)`. |

### `TransacaoViewModel.kt`
| Função / Propriedade | O que faz |
|---|---|
| `transacoes` | `StateFlow` com todas as transações do repositório. |
| `totalReceitas` | `StateFlow` com a soma total de receitas. |
| `totalDespesas` | `StateFlow` com a soma total de despesas. |
| `deletar()` | Deleta uma transação pelo id chamando o repositório. |

---

## Serviços (Background)

### `NotificationReaderService.kt`
| Função / Método | O que faz |
|---|---|
| `onCreate()` | Inicializa o serviço de leitura de notificações e o repositório de transações. |
| `onDestroy()` | Limpa o escopo de coroutines quando o serviço é destruído. |
| `onListenerConnected()` | Loga quando o listener se conecta ao serviço de notificações do sistema. |
| `onNotificationPosted()` | Intercepta notificações postadas, filtra apenas de bancos conhecidos e encaminha para processamento. |
| `onNotificationRemoved()` | Chamado quando uma notificação é removida (sem ação implementada). |
| `processarNotificacao()` | Analisa a notificação do banco, extrai tipo/valor/descrição, salva no banco e dispara broadcast. |
| `ACTION_NOVA_TRANSACAO` | Constante da action do broadcast enviado quando uma nova transação é detectada. |
| `EXTRA_BANCO`, `EXTRA_DESCRICAO`, `EXTRA_VALOR` | Chaves dos extras do Intent enviado no broadcast de nova transação. |

### `AnalisadorNotificacao.kt`
| Função / Constante | O que faz |
|---|---|
| `identificarTipo()` | Analisa o texto da notificação e retorna o `TipoTransacao` correspondente por palavras-chave. |
| `extrairValor()` | Extrai o valor monetário do texto da notificação usando regex no formato R$ brasileiro. |
| `gerarDescricao()` | Gera uma descrição legível da transação a partir do tipo, banco e valor extraídos. |
| `REGEX_VALOR` | Padrão regex para capturar valores em R$ (ex: R$ 1.234,56). |
| `PALAVRAS_TIPO` | Mapa de palavras-chave → `TipoTransacao` usado na identificação. |

### `BancoConfig.kt`
| Função / Constante | O que faz |
|---|---|
| `nomeBanco()` | Recebe o pacote do app (ex: `br.com.nubank`) e retorna o nome amigável do banco. |
| `ehBancoConhecido()` | Verifica se um pacote de app corresponde a algum banco/fintech suportado. |
| `BANCOS_SUPORTADOS` | Mapa de pacotes → nomes de bancos, fintechs, carteiras digitais e corretoras cadastradas. |

---

## Tema Visual

### `Theme.kt`
| Função / Composable | O que faz |
|---|---|
| `FinGuiaTheme()` | Aplica o tema do app (cores, tipografia, aparência do sistema) com suporte a dark/light mode. |

### `Color.kt`
| Constante | O que faz |
|---|---|
| `MoneyGreen` | Verde para ganhos financeiros. |
| `DangerRed` | Vermelho para alertas e perdas. |
| `DeepNavy` | Azul escuro de fundo. |
| `GojoPurple` | Roxo de destaque principal do app. |
| `DarkBg` | Cor de fundo escura das telas. |
| `CardBg` | Cor de fundo dos cards. |
| `GrayText` | Cinza para textos secundários. |

### `Type.kt`
| Constante | O que faz |
|---|---|
| `Typography` | Configuração de tipografia Material 3 com estilos para corpo, título e label. |
