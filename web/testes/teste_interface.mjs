// Teste de ponta a ponta do FinGuia Web no jsdom: index.html real, banco
// exportado do emulador e valores conferidos contra o SQLite (esperado.json).
// Cobre login simulado, sair, dashboards com templates e os blocos de dados.
import { JSDOM } from 'jsdom';
import fs from 'fs';
import path from 'path';
import initSqlJs from 'sql.js';

const AQUI = path.dirname(new URL(import.meta.url).pathname.replace(/^[/]([A-Za-z]:)/, '$1'));
const WEB = path.resolve(AQUI, '..').split(path.sep).join('/') + '/';
const esperado = JSON.parse(fs.readFileSync(path.join(AQUI, 'dados-teste', 'esperado.json'), 'utf8'));
const bytes = fs.readFileSync(path.join(AQUI, 'dados-teste', 'banco-ficticio.db'));

const dom = new JSDOM(fs.readFileSync(WEB + 'index.html', 'utf8'), { url: 'http://127.0.0.1/', pretendToBeVisual: true });
const { window } = dom;
for (const n of ['document', 'Node', 'Event', 'localStorage', 'sessionStorage']) globalThis[n] = window[n];
globalThis.window = window;
window.matchMedia = () => ({ matches: false, addEventListener() {} });
let confirmar = true;
window.confirm = () => confirmar;
window.alert = () => {};

let csvGerado = null;
globalThis.Blob = class { constructor(p) { csvGerado = p.join(''); } };
globalThis.URL = { createObjectURL: () => 'blob:teste', revokeObjectURL() {} };
window.HTMLAnchorElement.prototype.click = function () {};
globalThis.initSqlJs = (o) => initSqlJs({ ...o, locateFile: (f) => WEB + 'vendor/' + f });
globalThis.fetch = async (u) => (u === 'dados/finguia_database' ? { ok: true, arrayBuffer: async () => bytes } : { ok: false });

await import('file:///' + WEB + 'app.js');
await window.finguiaPronto;

// ------------------------------------------------------------------ ajudantes
const $ = (s) => window.document.querySelector(s);
const $$ = (s) => [...window.document.querySelectorAll(s)];
const esperar = (ms) => new Promise((r) => setTimeout(r, ms));
async function ate(cond, ms = 4000) {
  const fim = Date.now() + ms;
  while (Date.now() < fim) { if (cond()) return true; await esperar(25); }
  return false;
}
const moeda = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v).replace(String.fromCharCode(160), ' ');
const txt = (s) => ($(s) ? $(s).textContent.replace(String.fromCharCode(160), ' ').trim() : '(ausente)');
const disparar = (el, tipo) => el.dispatchEvent(new window.Event(tipo, { bubbles: true, cancelable: true }));
const blocos = () => $$('#dashboard [data-bloco]').map((b) => b.dataset.bloco);
async function logar(usuario, senha, cadastro = false) {
  if (cadastro !== (txt('#titulo-login') === 'Criar conta')) $('#btn-alternar-cadastro').click();
  $('#login-usuario').value = usuario;
  $('#login-senha').value = senha;
  disparar($('#form-login'), 'submit');
  await ate(() => !$('#tela-login').hidden || !$('#erro-login').hidden);
  await ate(() => !$('#painel').hidden || !$('#erro-login').hidden);
}
async function novoDashboard(nome, template) {
  $('#btn-novo-dashboard').click();
  $('#novo-nome').value = nome;
  const radio = $('#lista-templates input[value="' + template + '"]');
  if (radio) radio.checked = true;
  disparar($('#form-novo'), 'submit');
  await esperar(50);
}

fs.mkdirSync(path.join(AQUI, 'resultados'), { recursive: true });
const resultados = [];
let grupo = '';
const secao = (nome) => { grupo = nome; console.log(''); console.log('== ' + nome); };
function conferir(nome, obtido, esperadoV) {
  const ok = JSON.stringify(obtido) === JSON.stringify(esperadoV);
  resultados.push({ grupo, nome, ok, obtido, esperado: esperadoV });
  console.log((ok ? '  OK    ' : '  FALHA ') + nome + (ok ? '' : ' | obtido: ' + JSON.stringify(obtido) + ' | esperado: ' + JSON.stringify(esperadoV)));
}

// =============================================================== 1. login
secao('Login simulado');
conferir('abre na tela de login', [$('#tela-login').hidden, $('#painel').hidden], [false, true]);
const contas = JSON.parse(localStorage.getItem('finguia-usuarios') || '{}');
conferir('conta demo criada automaticamente', Object.keys(contas), ['demo']);
conferir('senha guardada como hash SHA-256 (64 hex), nunca em texto', /^[0-9a-f]{64}$/.test(contas.demo.hash) && !('senha' in contas.demo), true);

await logar('demo', 'senha-errada');
conferir('senha errada mostra erro', txt('#erro-login'), 'Usuário ou senha incorretos.');
conferir('senha errada limpa o campo de senha', $('#login-senha').value, '');
await logar('ninguem', 'finguia');
conferir('usuário inexistente tem a mesma mensagem', txt('#erro-login'), 'Usuário ou senha incorretos.');

await logar('DEMO', 'finguia');
conferir('login certo (usuário sem diferenciar maiúsculas) abre o painel', $('#painel').hidden, false);
conferir('usuário exibido no topo', txt('#usuario-logado'), 'demo');
conferir('sessão gravada só nesta aba (sessionStorage)', JSON.parse(sessionStorage.getItem('finguia-sessao')).usuario, 'demo');

// =================================================== 2. dashboard Principal
secao('Dashboard Principal (template Completo)');
conferir('um dashboard inicial', $$('#seletor-dashboard option').map((o) => o.textContent), ['Principal · Completo']);
conferir('excluir desabilitado com um só dashboard', $('#btn-excluir-dashboard').disabled, true);
conferir('receitas', txt('[data-metrica="receitas"] strong'), moeda(esperado.receitas));
conferir('despesas', txt('[data-metrica="despesas"] strong'), moeda(esperado.despesas));
conferir('saldo', txt('[data-metrica="saldo"] strong'), moeda(esperado.saldo));
conferir('lançamentos', txt('[data-metrica="lancamentos"] strong'), String(esperado.efetivadas));
conferir('abas', $$('#dashboard .aba').map((a) => a.textContent), ['Extrato', 'Fluxo mensal', 'Por banco', 'Agendados', 'Investimentos']);
conferir('itens no extrato', $$('[data-bloco="extrato"] .item').length, esperado.efetivadas);
conferir('agendados', $$('[data-bloco="agendados"] .item').length, esperado.agendadas);
conferir('bancos', $$('[data-bloco="bancos"] .item').length, esperado.bancos);
conferir('gráfico desenhado', $$('[data-bloco="fluxo"] svg rect').length > 0, true);
const busca = $('[data-bloco="extrato"] [data-campo="busca"]');
busca.value = 'maria'; disparar(busca, 'input');
conferir('busca "maria"', $$('[data-bloco="extrato"] .item').length, esperado.maria);
$('#btn-csv').click();
const linhasCsv = csvGerado.split(String.fromCharCode(13, 10));
conferir('CSV exporta o que o extrato filtra (cabeçalho + 1)', linhasCsv.length, esperado.maria + 1);
conferir('CSV com BOM para acentos no Excel', csvGerado.charCodeAt(0), 65279);
busca.value = ''; disparar(busca, 'input');
$('[data-aba="fluxo"]').click();
conferir('trocar de aba mostra só o bloco escolhido', [$('[data-bloco="fluxo"]').hidden, $('[data-bloco="extrato"]').hidden], [false, true]);

// ====================================================== 3. novos dashboards
secao('Novo dashboard com outro template');
$('#btn-novo-dashboard').click();
conferir('modal abre', $('#modal-novo').hidden, false);
conferir('4 templates oferecidos', $$('#lista-templates input').map((i) => i.value), ['completo', 'resumo', 'gastos', 'investimentos']);
disparar($('#form-novo'), 'submit');
conferir('nome vazio é recusado', txt('#erro-novo'), 'Dê um nome ao dashboard.');
$('#btn-cancelar-novo').click();
conferir('cancelar fecha o modal', $('#modal-novo').hidden, true);

await novoDashboard('Meus gastos', 'gastos');
conferir('dashboard criado e ativo', $('#seletor-dashboard').selectedOptions[0].textContent, 'Meus gastos · Gastos');
conferir('blocos do template Gastos', blocos(), ['metricasGastos', 'gastosPorTipo', 'maioresGastos', 'bancos']);
conferir('total gasto', txt('[data-metrica="despesas"] strong'), moeda(esperado.despesas));
conferir('quantidade de saídas', txt('[data-metrica="saidas"] strong'), String(esperado.saidas_qtd));
conferir('maior saída', txt('[data-metrica="maior"] strong'), moeda(esperado.maior_saida));
conferir('uma barra por tipo de saída', $$('.barra-linha').length, esperado.saidas_tipos);
conferir('tipo com mais gasto vem primeiro', $('.barra-linha').dataset.tipo, esperado.tipo_mais_gasto);
conferir('maiores gastos: no máximo 5, o primeiro é o maior', [$$('[data-bloco="maioresGastos"] .item').length, txt('[data-bloco="maioresGastos"] .item .valor')], [Math.min(5, esperado.saidas_qtd), '-' + moeda(esperado.maior_saida)]);

await novoDashboard('meus GASTOS', 'resumo');
conferir('nome repetido (sem diferenciar maiúsculas) é recusado', txt('#erro-novo'), 'Já existe um dashboard com esse nome.');
$('#btn-cancelar-novo').click();

await novoDashboard('Visão rápida', 'resumo');
conferir('blocos do template Resumo', blocos(), ['metricas', 'fluxo', 'maioresGastos']);
await novoDashboard('Carteira', 'investimentos');
conferir('blocos do template Investimentos', blocos(), ['metricasInvestimentos', 'investimentos']);
conferir('carteira vazia mostra aviso', [txt('[data-metrica="ativos"] strong'), txt('[data-bloco="investimentos"] .vazio')], [String(esperado.investimentos), 'Nenhum investimento cadastrado.']);
conferir('seletor com 4 dashboards', $('#seletor-dashboard').options.length, 4);

// ================================================= 4. alternar, excluir, salvar
secao('Alternar, excluir e persistência');
const sel = $('#seletor-dashboard');
sel.value = [...sel.options].find((o) => o.textContent.startsWith('Principal')).value;
disparar(sel, 'change');
conferir('voltar ao Principal redesenha o template Completo', $('#dashboard').dataset.template, 'completo');
confirmar = false; $('#btn-excluir-dashboard').click();
conferir('excluir e cancelar não apaga', sel.options.length, 4);
confirmar = true; $('#btn-excluir-dashboard').click();
conferir('excluir e confirmar apaga', [...sel.options].map((o) => o.textContent.split(' · ')[0]), ['Meus gastos', 'Visão rápida', 'Carteira']);
conferir('outro dashboard assume depois de excluir o ativo', $('#dashboard').dataset.template, 'gastos');
const salvos = JSON.parse(localStorage.getItem('finguia-dashboards:demo')).map((d) => d.nome + '/' + d.template);
conferir('dashboards salvos por usuário no navegador', salvos, ['Meus gastos/gastos', 'Visão rápida/resumo', 'Carteira/investimentos']);

// ================================================================= 5. sair
secao('Sair');
$('#btn-sair').click();
conferir('volta para o login', [$('#tela-login').hidden, $('#painel').hidden], [false, true]);
conferir('sessão encerrada', sessionStorage.getItem('finguia-sessao'), null);
conferir('dashboard limpo da tela', $('#dashboard').children.length, 0);
conferir('controles do painel escondidos', ['#btn-sair', '#btn-novo-dashboard', '#caixa-dashboard', '#btn-csv'].map((s) => $(s).hidden), [true, true, true, true]);

// =========================================================== 6. outra conta
secao('Criar conta e isolamento entre usuários');
await logar('ana', '123', true);
conferir('senha curta é recusada', txt('#erro-login'), 'A senha precisa ter pelo menos 4 caracteres.');
await logar('a', 'segredo123', true);
conferir('usuário curto é recusado', txt('#erro-login').startsWith('Usuário deve ter de 3 a 30'), true);
await logar('demo', 'segredo123', true);
conferir('usuário existente é recusado', txt('#erro-login'), 'Esse usuário já existe.');
await logar('ana', 'segredo123', true);
conferir('conta criada e já logada', txt('#usuario-logado'), 'ana');
conferir('ana começa só com o dashboard Principal', [...$('#seletor-dashboard').options].map((o) => o.textContent), ['Principal · Completo']);
conferir('senha da ana não aparece no armazenamento', localStorage.getItem('finguia-usuarios').includes('segredo123'), false);
$('#btn-sair').click();
await logar('demo', 'finguia');
conferir('demo continua com os dashboards dele e o último ativo', [$('#seletor-dashboard').options.length, $('#dashboard').dataset.template], [3, 'gastos']);

// =================================================================== fim
const falhas = resultados.filter((r) => !r.ok);
fs.writeFileSync(path.join(AQUI, 'resultados', 'interface.json'), JSON.stringify(resultados, null, 1));
console.log('');
console.log(resultados.length + ' verificações, ' + falhas.length + ' falhas');
process.exit(falhas.length ? 1 : 0);
