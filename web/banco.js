// Acesso ao arquivo SQLite do app via sql.js (WebAssembly, tudo no navegador).
// As consultas espelham as do TransacaoDao/InvestimentoDao para que os numeros
// batam exatamente com o que o app Android mostra.

import { TABELAS_ESPERADAS, TIPOS_RECEITA, TIPOS_DESPESA } from './esquema.js';

let SQL = null;

/** Carrega o runtime WebAssembly uma unica vez. */
async function runtime() {
  if (!SQL) {
    SQL = await initSqlJs({ locateFile: (arquivo) => `vendor/${arquivo}` });
  }
  return SQL;
}

function listaSql(valores) {
  return valores.map((v) => `'${v}'`).join(',');
}

/** Converte o resultado do sql.js em array de objetos. */
function linhas(resultado) {
  if (!resultado || resultado.length === 0) return [];
  const { columns, values } = resultado[0];
  return values.map((linha) => {
    const obj = {};
    columns.forEach((coluna, i) => {
      obj[coluna] = linha[i];
    });
    return obj;
  });
}

export class BancoFinGuia {
  constructor(db) {
    this.db = db;
  }

  /** Abre um arquivo finguia_database exportado do aparelho. */
  static async abrir(bytes) {
    const sql = await runtime();
    let db;
    try {
      db = new sql.Database(new Uint8Array(bytes));
    } catch (e) {
      throw new Error('Arquivo não é um banco SQLite válido.');
    }
    const banco = new BancoFinGuia(db);
    banco.validar();
    return banco;
  }

  /** Garante que o arquivo é mesmo do FinGuia antes de montar as telas. */
  validar() {
    const encontradas = linhas(
      this.db.exec("SELECT name FROM sqlite_master WHERE type = 'table'")
    ).map((l) => l.name);
    const faltando = TABELAS_ESPERADAS.filter((t) => !encontradas.includes(t));
    if (faltando.length === TABELAS_ESPERADAS.length) {
      throw new Error(
        'Este banco não parece ser do FinGuia (nenhuma tabela conhecida encontrada).'
      );
    }
    this.tabelas = encontradas;
    this.tabelasFaltando = faltando;
  }

  temTabela(nome) {
    return this.tabelas.includes(nome);
  }

  consultar(sql, params = []) {
    const stmt = this.db.prepare(sql);
    stmt.bind(params);
    const saida = [];
    while (stmt.step()) saida.push(stmt.getAsObject());
    stmt.free();
    return saida;
  }

  /** Equivalente a TransacaoDao.listarTodas(): só lançamentos efetivados. */
  transacoes() {
    if (!this.temTabela('transacoes_bancarias')) return [];
    return this.consultar(
      'SELECT * FROM transacoes_bancarias WHERE efetivado = 1 ORDER BY timestampMs DESC'
    );
  }

  /** Equivalente a TransacaoDao.listarAgendadas(): lançamentos futuros. */
  agendadas() {
    if (!this.temTabela('transacoes_bancarias')) return [];
    return this.consultar(
      'SELECT * FROM transacoes_bancarias WHERE efetivado = 0 ORDER BY dataAgendada ASC'
    );
  }

  /** Equivalente a TransacaoDao.listarRecorrentes(). */
  recorrentes() {
    if (!this.temTabela('transacoes_bancarias')) return [];
    return this.consultar(
      'SELECT * FROM transacoes_bancarias WHERE recorrente = 1 ORDER BY timestampMs DESC'
    );
  }

  /** Totais calculados no SQLite, iguais a totalReceitas()/totalDespesas(). */
  totais() {
    if (!this.temTabela('transacoes_bancarias')) {
      return { receitas: 0, despesas: 0, saldo: 0 };
    }
    const soma = (tipos) => {
      const r = this.consultar(
        `SELECT SUM(valor) AS total FROM transacoes_bancarias
         WHERE efetivado = 1 AND tipo IN (${listaSql(tipos)})`
      );
      return r[0]?.total ?? 0;
    };
    const receitas = soma(TIPOS_RECEITA);
    const despesas = soma(TIPOS_DESPESA);
    return { receitas, despesas, saldo: receitas - despesas };
  }

  investimentos() {
    if (!this.temTabela('investimentos')) return [];
    const registros = this.consultar(
      'SELECT * FROM investimentos ORDER BY dataCompraMs DESC'
    );
    // Reproduz os campos calculados de Investimento.kt (valorAtual / lucro)
    return registros.map((inv) => {
      const valorAtual = inv.valorInvestido * (1 + inv.rentabilidadePct / 100);
      return { ...inv, valorAtual, lucro: valorAtual - inv.valorInvestido };
    });
  }

  categorias() {
    if (!this.temTabela('categorias_custom')) return [];
    return this.consultar('SELECT * FROM categorias_custom ORDER BY label ASC');
  }

  fechar() {
    this.db.close();
  }
}
