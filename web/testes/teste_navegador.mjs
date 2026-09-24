// Teste do FinGuia Web num Firefox real (headless), servido pelo servidor.js.
// Faz o fluxo pela interface e tira capturas de tela para o relatorio.
import puppeteer from 'puppeteer-core';
import { spawn } from 'child_process';
import path from 'path';
import fs from 'fs';

const AQUI = path.dirname(new URL(import.meta.url).pathname.replace(/^[/]([A-Za-z]:)/, '$1'));
const WEB = path.resolve(AQUI, '..');
const SAIDA = path.join(AQUI, 'resultados', 'capturas');
fs.mkdirSync(SAIDA, { recursive: true });
const PORTA = 8124;
const URL_APP = 'http://127.0.0.1:' + PORTA + '/';

const servidor = spawn(process.execPath, ['servidor.js', '--porta', String(PORTA), '--sem-navegador',
  '--banco', path.join(AQUI, 'dados-teste', 'banco-ficticio.db')], { cwd: WEB });
await new Promise((r) => servidor.stdout.once('data', r));

fs.mkdirSync(path.join(AQUI, 'resultados'), { recursive: true });
const resultados = [];
const errosPagina = [];
function conferir(nome, obtido, esperado) {
  const ok = JSON.stringify(obtido) === JSON.stringify(esperado);
  resultados.push({ nome, ok, obtido, esperado });
  console.log((ok ? '  OK    ' : '  FALHA ') + nome + (ok ? '' : ' | obtido: ' + JSON.stringify(obtido) + ' | esperado: ' + JSON.stringify(esperado)));
}

const navegador = await puppeteer.launch({
  browser: 'firefox',
  executablePath: process.env.FIREFOX || 'C:/Program Files/Mozilla Firefox/firefox.exe',
  headless: true,
  // Instancia separada: sem isso, com um Firefox ja aberto, o pedido e
  // repassado para a janela existente e o processo de teste sai na hora
  args: ['-no-remote'],
  env: { ...process.env, MOZ_NO_REMOTE: '1' },
});
try {
  const pagina = await navegador.newPage();
  pagina.on('pageerror', (e) => errosPagina.push(String(e.message || e)));
  pagina.on('console', (m) => { if (m.type() === 'error') errosPagina.push(m.text()); });
  await pagina.setViewport({ width: 1280, height: 900 });

  const visivel = (sel) => pagina.$eval(sel, (e) => !e.hidden).catch(() => false);
  const texto = (sel) => pagina.$eval(sel, (e) => e.textContent.split(String.fromCharCode(160)).join(' ').trim()).catch(() => null);
  const foto = (nome) => pagina.screenshot({ path: path.join(SAIDA, nome + '.png') });

  await pagina.goto(URL_APP, { waitUntil: 'load' });
  await pagina.waitForSelector('#tela-login:not([hidden])');
  conferir('abre na tela de login', await visivel('#tela-login'), true);
  await foto('web-01-login');

  await pagina.type('#login-usuario', 'demo');
  await pagina.type('#login-senha', 'errada');
  await pagina.click('#btn-entrar');
  await pagina.waitForSelector('#erro-login:not([hidden])');
  conferir('senha errada recusada (criptografia do navegador funcionando)', await texto('#erro-login'), 'Usuário ou senha incorretos.');
  await foto('web-02-login-erro');

  await pagina.type('#login-senha', 'finguia');
  await pagina.click('#btn-entrar');
  await pagina.waitForSelector('#painel:not([hidden])', { timeout: 15000 });
  await pagina.waitForSelector('[data-metrica="receitas"]');
  conferir('login abre o painel com o banco (WebAssembly carregado)', await visivel('#painel'), true);
  conferir('receitas no navegador', await texto('[data-metrica="receitas"] strong'), 'R$ 3.330,00');
  await foto('web-03-dashboard-completo');

  await pagina.click('#btn-novo-dashboard');
  await pagina.waitForSelector('#modal-novo:not([hidden])');
  await pagina.type('#novo-nome', 'Meus gastos');
  await pagina.click('#lista-templates input[value="gastos"]');
  await foto('web-04-novo-dashboard');
  await pagina.click('#btn-criar-dashboard');
  await pagina.waitForSelector('[data-bloco="gastosPorTipo"]');
  conferir('dashboard Gastos criado', await pagina.$$eval('#dashboard [data-bloco]', (b) => b.map((x) => x.dataset.bloco)),
    ['metricasGastos', 'gastosPorTipo', 'maioresGastos', 'bancos']);
  await foto('web-05-dashboard-gastos');

  await pagina.click('#btn-tema');
  await foto('web-06-dashboard-gastos-claro');

  await pagina.setViewport({ width: 390, height: 844 });
  const largura = await pagina.evaluate(() => [document.documentElement.scrollWidth, window.innerWidth]);
  conferir('celular (390px): sem rolagem horizontal', largura[0] <= largura[1], true);
  await foto('web-07-celular');
  await pagina.setViewport({ width: 1280, height: 900 });

  await pagina.click('#btn-sair');
  await pagina.waitForSelector('#tela-login:not([hidden])');
  conferir('sair volta ao login', [await visivel('#tela-login'), await visivel('#painel')], [true, false]);
  await pagina.reload({ waitUntil: 'load' });
  await pagina.waitForSelector('#tela-login:not([hidden])');
  conferir('depois de sair, recarregar continua no login', await visivel('#painel'), false);

  await pagina.type('#login-usuario', 'demo');
  await pagina.type('#login-senha', 'finguia');
  await pagina.click('#btn-entrar');
  await pagina.waitForSelector('[data-bloco="gastosPorTipo"]', { timeout: 15000 });
  conferir('dashboard escolhido continua salvo após recarregar', await pagina.$eval('#dashboard', (d) => d.dataset.template), 'gastos');
  await pagina.reload({ waitUntil: 'load' });
  await pagina.waitForSelector('#painel:not([hidden])', { timeout: 15000 });
  conferir('recarregar com sessão aberta não pede login de novo', await visivel('#tela-login'), false);

  conferir('nenhum erro de JavaScript na página', errosPagina, []);
} finally {
  await navegador.close();
  servidor.kill();
}

const falhas = resultados.filter((r) => !r.ok).length;
fs.writeFileSync(path.join(AQUI, 'resultados', 'navegador.json'), JSON.stringify(resultados, null, 1));
console.log('');
console.log(resultados.length + ' verificações, ' + falhas + ' falhas');
process.exit(falhas ? 1 : 0);
