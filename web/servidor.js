#!/usr/bin/env node
// Servidor local do FinGuia Web. Sem dependencias: so o Node padrao.
//
//   node servidor.js [--porta 8080] [--banco caminho/finguia_database] [--sem-navegador]
//
// Serve os arquivos desta pasta e abre o navegador. Um --banco informado e
// exposto em /dados/finguia_database, e a pagina o carrega sozinha.

const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const RAIZ = __dirname;

const TIPOS = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.wasm': 'application/wasm',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
};

function lerArgumentos(argv) {
  const opcoes = { porta: 8080, banco: null, abrirNavegador: true };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--porta' || arg === '-p') opcoes.porta = Number(argv[++i]);
    else if (arg === '--banco' || arg === '-b') opcoes.banco = argv[++i];
    else if (arg === '--sem-navegador') opcoes.abrirNavegador = false;
    else if (arg === '--ajuda' || arg === '-h') opcoes.ajuda = true;
  }
  return opcoes;
}

/** Resolve a URL para um arquivo dentro da pasta web, barrando path traversal. */
function resolverCaminho(url) {
  const relativo = decodeURIComponent(url.split('?')[0]).replace(/^\/+/, '');
  const destino = path.resolve(RAIZ, relativo === '' ? 'index.html' : relativo);
  if (destino !== RAIZ && !destino.startsWith(RAIZ + path.sep)) return null;
  return destino;
}

/** Procura o banco nos lugares mais provaveis quando nada foi informado. */
function descobrirBanco() {
  const candidatos = [
    path.join(RAIZ, 'dados', 'finguia_database'),
    path.join(RAIZ, '..', 'backup_finguia', 'finguia_database'),
  ];
  return candidatos.find((c) => fs.existsSync(c)) || null;
}

function abrirNavegador(endereco) {
  const comandos = {
    win32: ['cmd', ['/c', 'start', '', endereco]],
    darwin: ['open', [endereco]],
  };
  const [cmd, args] = comandos[process.platform] || ['xdg-open', [endereco]];
  try {
    spawn(cmd, args, { detached: true, stdio: 'ignore' }).unref();
  } catch (e) {
    console.log('Abra manualmente: ' + endereco);
  }
}

function iniciar(opcoes) {
  const banco = opcoes.banco || descobrirBanco();
  if (opcoes.banco && !fs.existsSync(opcoes.banco)) {
    console.error('Banco nao encontrado: ' + opcoes.banco);
    process.exit(1);
  }

  const servidor = http.createServer((req, res) => {
    // Rota especial: entrega o banco escolhido sem precisar copia-lo para ca
    if (banco && req.url.split('?')[0] === '/dados/finguia_database') {
      res.writeHead(200, { 'Content-Type': 'application/octet-stream' });
      fs.createReadStream(banco).pipe(res);
      return;
    }

    const arquivo = resolverCaminho(req.url);
    if (!arquivo) {
      res.writeHead(403).end('Acesso negado');
      return;
    }
    fs.readFile(arquivo, (erro, conteudo) => {
      if (erro) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Nao encontrado: ' + req.url);
        return;
      }
      const tipo = TIPOS[path.extname(arquivo)] || 'application/octet-stream';
      res.writeHead(200, { 'Content-Type': tipo });
      res.end(conteudo);
    });
  });

  servidor.on('error', (erro) => {
    if (erro.code === 'EADDRINUSE') {
      console.error('A porta ' + opcoes.porta + ' ja esta em uso. Use --porta outra.');
      process.exit(1);
    }
    throw erro;
  });

  servidor.listen(opcoes.porta, '127.0.0.1', () => {
    const endereco = 'http://127.0.0.1:' + opcoes.porta;
    console.log('FinGuia Web rodando em ' + endereco);
    console.log(banco ? 'Banco carregado: ' + banco : 'Nenhum banco local - arraste o arquivo na pagina.');
    console.log('Ctrl+C para encerrar.');
    if (opcoes.abrirNavegador) abrirNavegador(endereco);
  });
}

const opcoes = lerArgumentos(process.argv.slice(2));
if (opcoes.ajuda) {
  console.log('Uso: node servidor.js [--porta 8080] [--banco <arquivo>] [--sem-navegador]');
} else {
  iniciar(opcoes);
}
