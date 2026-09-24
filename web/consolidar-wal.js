#!/usr/bin/env node
// O Room grava em modo WAL: o arquivo finguia_database costuma vir quase vazio e
// os dados reais ficam no finguia_database-wal. O sql.js do navegador nao le WAL,
// entao antes de abrir no FinGuia Web precisamos dobrar o WAL dentro do .db.
//
//   node consolidar-wal.js dados/finguia_database
//
// Requer Node 22+ (modulo node:sqlite). Os arquivos -wal e -shm precisam estar
// na mesma pasta, com os mesmos nomes que tinham no aparelho.

const fs = require('fs');
const path = require('path');

const alvo = process.argv[2];
if (!alvo) {
  console.error('Uso: node consolidar-wal.js <caminho/finguia_database>');
  process.exit(1);
}
if (!fs.existsSync(alvo)) {
  console.error('Arquivo nao encontrado: ' + alvo);
  process.exit(1);
}

let DatabaseSync;
try {
  ({ DatabaseSync } = require('node:sqlite'));
} catch (e) {
  console.error('Este Node nao tem o modulo node:sqlite (precisa da versao 22 ou mais nova).');
  console.error('Alternativa: sqlite3 "' + alvo + '" "PRAGMA wal_checkpoint(TRUNCATE);"');
  process.exit(1);
}

const wal = alvo + '-wal';
const antes = fs.statSync(alvo).size;
const tamanhoWal = fs.existsSync(wal) ? fs.statSync(wal).size : 0;

const db = new DatabaseSync(alvo);
db.exec('PRAGMA wal_checkpoint(TRUNCATE);');
db.exec('PRAGMA journal_mode=DELETE;');

const linhas = db.prepare('SELECT COUNT(*) AS n FROM transacoes_bancarias').get();
db.close();

// Depois do checkpoint o -wal/-shm viram lixo: apagar evita reabrir dados velhos
[alvo + '-wal', alvo + '-shm'].forEach((f) => {
  if (fs.existsSync(f)) fs.rmSync(f);
});

console.log('Banco consolidado: ' + path.resolve(alvo));
console.log('  tamanho: ' + antes + ' -> ' + fs.statSync(alvo).size + ' bytes (WAL de ' + tamanhoWal + ')');
console.log('  transacoes: ' + linhas.n);
