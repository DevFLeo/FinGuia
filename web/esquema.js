// Espelha o esquema Room do app Android (FinGuiaDatabase, versao 5).
// Qualquer mudanca nas entidades Kotlin precisa ser refletida aqui.

// Tipos considerados entrada pelo TransacaoDao.listarReceitas()/totalReceitas()
export const TIPOS_RECEITA = [
  'PIX_RECEBIDO',
  'TRANSFERENCIA_RECEBIDA',
  'DEPOSITO',
  'ESTORNO',
];

// Tipos considerados saida pelo TransacaoDao.listarDespesas()/totalDespesas()
export const TIPOS_DESPESA = [
  'PIX_ENVIADO',
  'COMPRA_DEBITO',
  'COMPRA_CREDITO',
  'BOLETO_PAGO',
  'TRANSFERENCIA_ENVIADA',
  'SAQUE',
];

// Rotulos legiveis para o enum TipoTransacao
export const ROTULO_TIPO = {
  PIX_RECEBIDO: 'Pix recebido',
  PIX_ENVIADO: 'Pix enviado',
  COMPRA_DEBITO: 'Compra no débito',
  COMPRA_CREDITO: 'Compra no crédito',
  BOLETO_PAGO: 'Boleto pago',
  TRANSFERENCIA_RECEBIDA: 'Transferência recebida',
  TRANSFERENCIA_ENVIADA: 'Transferência enviada',
  COBRANCA: 'Cobrança',
  ESTORNO: 'Estorno',
  SAQUE: 'Saque',
  DEPOSITO: 'Depósito',
  DESCONHECIDO: 'Não identificado',
};

export const ROTULO_CATEGORIA_INVESTIMENTO = {
  ACOES_BR: 'Ações Brasil',
  ACOES_INTER: 'Ações internacionais',
  IMOVEIS: 'Imóveis e FIIs',
  RENDA_FIXA: 'Renda fixa',
  CRIPTO: 'Criptomoedas',
  OUTROS: 'Outros',
};

export function ehReceita(tipo) {
  return TIPOS_RECEITA.includes(tipo);
}

export function ehDespesa(tipo) {
  return TIPOS_DESPESA.includes(tipo);
}

/** Sinal contabil do tipo: +1 entrada, -1 saida, 0 neutro (COBRANCA/DESCONHECIDO). */
export function sinal(tipo) {
  if (ehReceita(tipo)) return 1;
  if (ehDespesa(tipo)) return -1;
  return 0;
}

// Tabelas que o visualizador espera encontrar no arquivo .db do app
export const TABELAS_ESPERADAS = [
  'transacoes_bancarias',
  'categorias_custom',
  'investimentos',
];
