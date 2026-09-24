// FinGuia Web: fluxo entre as telas. Login simulado (auth.js) -> abrir o banco
// (banco.js) -> dashboard ativo, montado pelo template (dashboards.js) com os
// blocos de widgets.js.

import { BancoFinGuia } from './banco.js';
import * as auth from './auth.js';
import * as dashboards from './dashboards.js';
import { BLOCOS, NOMES_BLOCOS } from './widgets.js';

const $ = (sel) => document.querySelector(sel);
const mostrar = (sel, visivel) => { $(sel).hidden = !visivel; };

const estado = {
  usuario: null,
  banco: null,
  nomeArquivo: null,
  ctx: null,
  modoCadastro: false,
};

// ====================================================================== telas

function telaLogin() {
  ['#tela-carregar', '#painel'].forEach((s) => mostrar(s, false));
  ['#caixa-dashboard', '#btn-novo-dashboard', '#btn-excluir-dashboard', '#btn-csv', '#btn-trocar', '#btn-sair', '#usuario-logado']
    .forEach((s) => mostrar(s, false));
  $('#subtitulo').textContent = 'Visualizador do banco de dados do app';
  definirModoCadastro(false);
  $('#form-login').reset();
  mostrar('#erro-login', false);
  mostrar('#tela-login', true);
  $('#login-usuario').focus();
}

function telaCarregar() {
  mostrar('#tela-login', false);
  mostrar('#painel', false);
  ['#caixa-dashboard', '#btn-novo-dashboard', '#btn-excluir-dashboard', '#btn-csv', '#btn-trocar'].forEach((s) => mostrar(s, false));
  mostrar('#erro-carregar', false);
  mostrar('#tela-carregar', true);
}

function telaPainel() {
  mostrar('#tela-login', false);
  mostrar('#tela-carregar', false);
  ['#caixa-dashboard', '#btn-novo-dashboard', '#btn-excluir-dashboard', '#btn-csv', '#btn-trocar'].forEach((s) => mostrar(s, true));
  mostrar('#painel', true);
  $('#subtitulo').textContent = estado.nomeArquivo || '';

  const b = estado.banco;
  const aviso = $('#aviso-tabelas');
  aviso.hidden = !b.tabelasFaltando.length;
  aviso.textContent = b.tabelasFaltando.length
    ? 'Banco de uma versão anterior: faltam as tabelas ' + b.tabelasFaltando.join(', ') + '. Os blocos correspondentes ficam vazios.'
    : '';

  atualizarSeletor();
  desenharDashboard();
}

// ====================================================================== login

function definirModoCadastro(cadastro) {
  estado.modoCadastro = cadastro;
  $('#titulo-login').textContent = cadastro ? 'Criar conta' : 'Entrar';
  $('#btn-entrar').textContent = cadastro ? 'Criar conta e entrar' : 'Entrar';
  $('#btn-alternar-cadastro').textContent = cadastro ? 'Já tenho conta. Entrar' : 'Não tem conta? Criar conta';
  $('#login-senha').autocomplete = cadastro ? 'new-password' : 'current-password';
  mostrar('#erro-login', false);
}

async function enviarLogin(ev) {
  ev.preventDefault();
  const usuario = $('#login-usuario').value;
  const senha = $('#login-senha').value;
  const botao = $('#btn-entrar');
  botao.disabled = true;
  // O erro da tentativa anterior sai antes da nova ser conferida
  mostrar('#erro-login', false);
  try {
    if (estado.modoCadastro) await auth.criarConta(usuario, senha);
    estado.usuario = await auth.entrar(usuario, senha);
    await depoisDoLogin();
  } catch (e) {
    const erro = $('#erro-login');
    erro.textContent = e.message;
    erro.hidden = false;
    $('#login-senha').value = '';
    $('#login-senha').focus();
  } finally {
    botao.disabled = false;
  }
}

async function depoisDoLogin() {
  $('#usuario-logado').textContent = estado.usuario;
  mostrar('#usuario-logado', true);
  mostrar('#btn-sair', true);
  if (estado.banco) return telaPainel();
  // Servidor local com um banco em web/dados/ (ou --banco): abre sozinho
  try {
    const r = await fetch('dados/finguia_database');
    if (r.ok) return abrirBanco(await r.arrayBuffer(), 'dados/finguia_database');
  } catch (e) {
    /* sem banco local */
  }
  telaCarregar();
}

function sair() {
  auth.sair();
  if (estado.banco) estado.banco.fechar();
  Object.assign(estado, { usuario: null, banco: null, nomeArquivo: null, ctx: null });
  $('#dashboard').replaceChildren();
  fecharModal();
  telaLogin();
}

// ====================================================================== banco

async function abrirBanco(bytes, nome) {
  try {
    const banco = await BancoFinGuia.abrir(bytes);
    if (estado.banco) estado.banco.fechar();
    estado.banco = banco;
    estado.nomeArquivo = nome;
    telaPainel();
  } catch (e) {
    telaCarregar();
    const erro = $('#erro-carregar');
    erro.textContent = e.message || 'Não foi possível ler este arquivo.';
    erro.hidden = false;
  }
}

function ligarCarregamento() {
  const alvo = $('#tela-carregar');
  const entrada = $('#entrada-arquivo');
  const abrirArquivo = async (arquivo) => abrirBanco(await arquivo.arrayBuffer(), arquivo.name);

  $('#btn-escolher').addEventListener('click', () => entrada.click());
  alvo.addEventListener('click', (ev) => {
    if (!ev.target.closest('button, details, a')) entrada.click();
  });
  entrada.addEventListener('change', () => {
    if (entrada.files[0]) abrirArquivo(entrada.files[0]);
    entrada.value = '';
  });
  ['dragenter', 'dragover'].forEach((t) => alvo.addEventListener(t, (ev) => { ev.preventDefault(); alvo.classList.add('sobre'); }));
  ['dragleave', 'drop'].forEach((t) => alvo.addEventListener(t, (ev) => { ev.preventDefault(); alvo.classList.remove('sobre'); }));
  alvo.addEventListener('drop', (ev) => {
    const arquivo = ev.dataTransfer && ev.dataTransfer.files && ev.dataTransfer.files[0];
    if (arquivo) abrirArquivo(arquivo);
  });
}

function trocarBanco() {
  if (estado.banco) estado.banco.fechar();
  Object.assign(estado, { banco: null, nomeArquivo: null, ctx: null });
  $('#dashboard').replaceChildren();
  telaCarregar();
}

// ================================================================= dashboards

function atualizarSeletor() {
  const sel = $('#seletor-dashboard');
  const ativo = dashboards.ativo(estado.usuario);
  sel.replaceChildren(...dashboards.listar(estado.usuario).map((d) => {
    const op = document.createElement('option');
    op.value = d.id;
    op.textContent = d.nome + ' · ' + dashboards.TEMPLATES[d.template].nome;
    op.selected = d.id === ativo.id;
    return op;
  }));
  $('#btn-excluir-dashboard').disabled = sel.options.length <= 1;
}

function desenharDashboard() {
  const b = estado.banco;
  const ctx = {
    transacoes: b.transacoes(),
    agendadas: b.agendadas(),
    investimentos: b.investimentos(),
    totais: b.totais(),
    estado: {},
  };
  estado.ctx = ctx;

  const painel = dashboards.ativo(estado.usuario);
  const template = dashboards.TEMPLATES[painel.template];
  const raiz = $('#dashboard');
  raiz.dataset.template = painel.template;
  raiz.replaceChildren();

  if (!template.abas) {
    raiz.append(...template.blocos.map((nome) => BLOCOS[nome](ctx)));
    return;
  }

  // Template com abas: primeiro bloco fixo em cima, os demais em abas
  const [fixo, ...emAbas] = template.blocos;
  raiz.appendChild(BLOCOS[fixo](ctx));
  const nav = document.createElement('nav');
  nav.className = 'abas';
  nav.setAttribute('role', 'tablist');
  const conteudos = emAbas.map((nome, i) => {
    const aba = document.createElement('button');
    aba.type = 'button';
    aba.className = 'aba' + (i === 0 ? ' ativa' : '');
    aba.dataset.aba = nome;
    aba.setAttribute('role', 'tab');
    aba.textContent = NOMES_BLOCOS[nome];
    nav.appendChild(aba);
    const bloco = BLOCOS[nome](ctx);
    bloco.hidden = i !== 0;
    return bloco;
  });
  nav.addEventListener('click', (ev) => {
    const aba = ev.target.closest('.aba');
    if (!aba) return;
    nav.querySelectorAll('.aba').forEach((a) => a.classList.toggle('ativa', a === aba));
    conteudos.forEach((c) => { c.hidden = c.dataset.bloco !== aba.dataset.aba; });
  });
  raiz.append(nav, ...conteudos);
}

function trocarDashboard() {
  dashboards.ativar(estado.usuario, $('#seletor-dashboard').value);
  desenharDashboard();
}

function excluirDashboard() {
  const ativo = dashboards.ativo(estado.usuario);
  if (!window.confirm('Excluir o dashboard "' + ativo.nome + '"? Os dados do banco não são apagados.')) return;
  try {
    dashboards.excluir(estado.usuario, ativo.id);
  } catch (e) {
    window.alert(e.message);
    return;
  }
  atualizarSeletor();
  desenharDashboard();
}

// -------------------------------------------------------------- modal "novo"

function abrirModal() {
  const lista = $('#lista-templates');
  lista.replaceChildren(...Object.entries(dashboards.TEMPLATES).map(([chave, t], i) => {
    const opcao = document.createElement('label');
    opcao.className = 'opcao-template';
    const radio = document.createElement('input');
    radio.type = 'radio';
    radio.name = 'template';
    radio.value = chave;
    radio.checked = i === 0;
    const nome = document.createElement('strong');
    nome.textContent = t.nome;
    const desc = document.createElement('span');
    desc.textContent = t.descricao;
    opcao.append(radio, nome, desc);
    return opcao;
  }));
  $('#form-novo').reset();
  lista.querySelector('input').checked = true;
  mostrar('#erro-novo', false);
  mostrar('#modal-novo', true);
  $('#novo-nome').focus();
}

function fecharModal() {
  mostrar('#modal-novo', false);
}

function criarDashboard(ev) {
  ev.preventDefault();
  const escolhido = $('#lista-templates input:checked');
  try {
    dashboards.criar(estado.usuario, $('#novo-nome').value, escolhido && escolhido.value);
  } catch (e) {
    const erro = $('#erro-novo');
    erro.textContent = e.message;
    erro.hidden = false;
    return;
  }
  fecharModal();
  atualizarSeletor();
  desenharDashboard();
}

// ================================================================ exportacao

function exportarCsv() {
  const ctx = estado.ctx;
  if (!ctx) return;
  // Com extrato na tela, exporta o que ele esta filtrando; senao, tudo
  const lista = ctx.estado.filtrarExtrato ? ctx.estado.filtrarExtrato() : ctx.transacoes;
  const colunas = ['id', 'banco', 'tipo', 'valor', 'descricao', 'timestampMs', 'recorrente'];
  const escapar = (v) => '"' + String(v === null || v === undefined ? '' : v).replace(/"/g, '""') + '"';
  const linhas = [colunas.join(';')];
  lista.forEach((t) => {
    linhas.push(colunas.map((c) => escapar(c === 'timestampMs' ? new Date(t[c]).toISOString() : t[c])).join(';'));
  });
  // BOM no inicio para o Excel em pt-BR reconhecer os acentos
  const bom = String.fromCharCode(0xfeff);
  const blob = new Blob([bom + linhas.join(String.fromCharCode(13, 10))], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'finguia-extrato.csv';
  a.click();
  URL.revokeObjectURL(url);
}

// ====================================================================== tema

function aplicarTema(tema) {
  document.documentElement.dataset.tema = tema;
  try {
    localStorage.setItem('finguia-tema', tema);
  } catch (e) {
    /* modo privado: segue sem persistir */
  }
}

function iniciarTema() {
  let salvo = null;
  try {
    salvo = localStorage.getItem('finguia-tema');
  } catch (e) {
    /* ignorado */
  }
  aplicarTema(salvo || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro'));
}

// =============================================================== inicializacao

function ligarEventos() {
  $('#form-login').addEventListener('submit', enviarLogin);
  $('#btn-alternar-cadastro').addEventListener('click', () => definirModoCadastro(!estado.modoCadastro));
  $('#btn-sair').addEventListener('click', sair);
  $('#btn-trocar').addEventListener('click', trocarBanco);
  $('#btn-csv').addEventListener('click', exportarCsv);
  $('#btn-tema').addEventListener('click', () =>
    aplicarTema(document.documentElement.dataset.tema === 'escuro' ? 'claro' : 'escuro'));
  $('#seletor-dashboard').addEventListener('change', trocarDashboard);
  $('#btn-novo-dashboard').addEventListener('click', abrirModal);
  $('#btn-excluir-dashboard').addEventListener('click', excluirDashboard);
  $('#form-novo').addEventListener('submit', criarDashboard);
  $('#btn-cancelar-novo').addEventListener('click', fecharModal);
  $('#modal-novo').addEventListener('click', (ev) => { if (ev.target.id === 'modal-novo') fecharModal(); });
  document.addEventListener('keydown', (ev) => { if (ev.key === 'Escape') fecharModal(); });
  ligarCarregamento();
}

async function iniciar() {
  iniciarTema();
  ligarEventos();
  await auth.garantirContaDemo();
  estado.usuario = auth.usuarioAtual();
  if (estado.usuario) await depoisDoLogin();
  else telaLogin();
}

// Exposto para os testes automatizados aguardarem a inicializacao
window.finguiaPronto = iniciar();
