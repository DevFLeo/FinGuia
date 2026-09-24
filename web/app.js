// Interface do FinGuia Web: le o banco do app e monta extrato, fluxo e investimentos.

import { BancoFinGuia } from './banco.js';
import {
  ROTULO_TIPO,
  ROTULO_CATEGORIA_INVESTIMENTO,
  sinal,
} from './esquema.js';

const $ = (sel) => document.querySelector(sel);

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const mesExtenso = new Intl.DateTimeFormat('pt-BR', { month: 'short', year: '2-digit' });

let banco = null;
let transacoes = [];

// ---------------------------------------------------------------- carregamento

function mostrarErro(msg) {
  const el = $('#erro-carregar');
  el.textContent = msg;
  el.hidden = false;
}

async function carregarArquivo(arquivo) {
  $('#erro-carregar').hidden = true;
  try {
    const bytes = await arquivo.arrayBuffer();
    if (banco) banco.fechar();
    banco = await BancoFinGuia.abrir(bytes);
    transacoes = banco.transacoes();
    montarPainel(arquivo.name);
  } catch (e) {
    mostrarErro(e.message || 'Nao foi possivel ler este arquivo.');
  }
}

function ligarCarregamento() {
  const alvo = $('#tela-carregar');
  const entrada = $('#entrada-arquivo');

  $('#btn-escolher').addEventListener('click', () => entrada.click());
  alvo.addEventListener('click', (ev) => {
    if (ev.target.closest('button, details, a')) return;
    entrada.click();
  });
  entrada.addEventListener('change', () => {
    if (entrada.files[0]) carregarArquivo(entrada.files[0]);
  });

  ['dragenter', 'dragover'].forEach((evt) =>
    alvo.addEventListener(evt, (ev) => {
      ev.preventDefault();
      alvo.classList.add('sobre');
    })
  );
  ['dragleave', 'drop'].forEach((evt) =>
    alvo.addEventListener(evt, (ev) => {
      ev.preventDefault();
      alvo.classList.remove('sobre');
    })
  );
  alvo.addEventListener('drop', (ev) => {
    const arquivo = ev.dataTransfer && ev.dataTransfer.files && ev.dataTransfer.files[0];
    if (arquivo) carregarArquivo(arquivo);
  });
}

// ---------------------------------------------------------------- painel

function montarPainel(nomeArquivo) {
  $('#tela-carregar').hidden = true;
  $('#painel').hidden = false;
  $('#btn-csv').hidden = false;
  $('#btn-trocar').hidden = false;
  $('.sub').textContent = nomeArquivo;

  if (banco.tabelasFaltando.length) {
    const aviso = $('#aviso-tabelas');
    aviso.textContent =
      'Banco de uma versao anterior: faltam as tabelas ' +
      banco.tabelasFaltando.join(', ') +
      '. As abas correspondentes ficam vazias.';
    aviso.hidden = false;
  }

  renderMetricas();
  preencherFiltros();
  renderExtrato();
  renderFluxo();
  renderBancos();
  renderAgendados();
  renderInvestimentos();
}

function renderMetricas() {
  const { receitas, despesas, saldo } = banco.totais();
  $('#m-receitas').textContent = moeda.format(receitas);
  $('#m-despesas').textContent = moeda.format(despesas);
  $('#m-saldo').textContent = moeda.format(saldo);
  $('#m-saldo').style.color = saldo < 0 ? 'var(--saida)' : 'var(--entrada)';
  $('#m-total').textContent = transacoes.length;
}

function preencherFiltros() {
  const tipos = [...new Set(transacoes.map((t) => t.tipo))].sort();
  const bancos = [...new Set(transacoes.map((t) => t.banco))].sort();
  const preencher = (sel, valores, formatar) => {
    const el = $(sel);
    valores.forEach((v) => {
      const op = document.createElement('option');
      op.value = v;
      op.textContent = formatar(v);
      el.appendChild(op);
    });
  };
  preencher('#filtro-tipo', tipos, (t) => ROTULO_TIPO[t] || t);
  preencher('#filtro-banco', bancos, (b) => b);
}

function transacoesFiltradas() {
  const termo = $('#busca').value.trim().toLowerCase();
  const tipo = $('#filtro-tipo').value;
  const nomeBanco = $('#filtro-banco').value;
  return transacoes.filter((t) => {
    if (tipo && t.tipo !== tipo) return false;
    if (nomeBanco && t.banco !== nomeBanco) return false;
    if (!termo) return true;
    return [t.descricao, t.banco, t.tituloNotificacao, t.textoNotificacao].some((campo) =>
      (campo || '').toLowerCase().includes(termo)
    );
  });
}

function itemTransacao(t) {
  const s = sinal(t.tipo);
  const classe = s > 0 ? 'entrada' : s < 0 ? 'saida' : '';
  const prefixo = s > 0 ? '+' : s < 0 ? '-' : '';
  const el = document.createElement('div');
  el.className = 'item';
  el.innerHTML =
    '<div class="corpo"><div class="titulo"></div><div class="meta"></div></div>' +
    '<div class="valor ' + classe + '"></div>';
  el.querySelector('.valor').textContent = prefixo + moeda.format(Math.abs(t.valor));
  el.querySelector('.titulo').textContent = t.descricao || ROTULO_TIPO[t.tipo] || t.tipo;
  el.querySelector('.meta').textContent =
    t.banco +
    ' - ' +
    (ROTULO_TIPO[t.tipo] || t.tipo) +
    ' - ' +
    dataHora.format(new Date(t.timestampMs)) +
    (t.recorrente ? ' - recorrente' : '');
  return el;
}

function renderLista(seletor, itens, montar, mensagemVazia) {
  const alvo = $(seletor);
  alvo.replaceChildren();
  if (!itens.length) {
    const vazio = document.createElement('p');
    vazio.className = 'vazio';
    vazio.textContent = mensagemVazia;
    alvo.appendChild(vazio);
    return;
  }
  itens.forEach((item) => alvo.appendChild(montar(item)));
}

function renderExtrato() {
  renderLista(
    '#lista-transacoes',
    transacoesFiltradas(),
    itemTransacao,
    'Nenhum lancamento encontrado com esses filtros.'
  );
}

// ---------------------------------------------------------------- fluxo mensal

function agruparPorMes() {
  const mapa = new Map();
  transacoes.forEach((t) => {
    const d = new Date(t.timestampMs);
    const chave = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
    if (!mapa.has(chave)) mapa.set(chave, { chave, data: d, entradas: 0, saidas: 0 });
    const mes = mapa.get(chave);
    const s = sinal(t.tipo);
    if (s > 0) mes.entradas += t.valor;
    else if (s < 0) mes.saidas += t.valor;
  });
  return [...mapa.values()].sort((a, b) => a.chave.localeCompare(b.chave)).slice(-12);
}

function renderFluxo() {
  const meses = agruparPorMes();
  const alvo = $('#grafico-fluxo');
  alvo.replaceChildren();
  if (!meses.length) {
    const vazio = document.createElement('p');
    vazio.className = 'vazio';
    vazio.textContent = 'Sem lancamentos para montar o grafico.';
    alvo.appendChild(vazio);
    return;
  }

  const L = 720;
  const A = 260;
  const base = A - 34;
  const topo = 16;
  const maximo = Math.max(...meses.flatMap((m) => [m.entradas, m.saidas]), 1);
  const largura = L / meses.length;
  const barra = Math.min(22, largura / 3);

  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', '0 0 ' + L + ' ' + A);
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', 'Entradas e saidas por mes');

  const linhaBase = document.createElementNS(ns, 'line');
  linhaBase.setAttribute('x1', 0);
  linhaBase.setAttribute('x2', L);
  linhaBase.setAttribute('y1', base);
  linhaBase.setAttribute('y2', base);
  linhaBase.setAttribute('stroke', 'var(--borda)');
  svg.appendChild(linhaBase);

  const series = [
    { campo: 'entradas', cor: 'var(--entrada)', deslocamento: -barra - 2 },
    { campo: 'saidas', cor: 'var(--saida)', deslocamento: 2 },
  ];

  meses.forEach((m, i) => {
    const centro = i * largura + largura / 2;
    series.forEach(({ campo, cor, deslocamento }) => {
      const altura = (m[campo] / maximo) * (base - topo);
      const r = document.createElementNS(ns, 'rect');
      r.setAttribute('x', centro + deslocamento);
      r.setAttribute('y', base - altura);
      r.setAttribute('width', barra);
      r.setAttribute('height', Math.max(altura, 0));
      r.setAttribute('rx', 3);
      r.setAttribute('fill', cor);
      const titulo = document.createElementNS(ns, 'title');
      titulo.textContent =
        mesExtenso.format(m.data) + ' - ' + campo + ': ' + moeda.format(m[campo]);
      r.appendChild(titulo);
      svg.appendChild(r);
    });

    const rotulo = document.createElementNS(ns, 'text');
    rotulo.setAttribute('x', centro);
    rotulo.setAttribute('y', base + 18);
    rotulo.setAttribute('text-anchor', 'middle');
    rotulo.setAttribute('font-size', '11');
    rotulo.setAttribute('fill', 'var(--texto-fraco)');
    rotulo.textContent = mesExtenso.format(m.data);
    svg.appendChild(rotulo);
  });

  alvo.appendChild(svg);

  const legenda = document.createElement('div');
  legenda.className = 'legenda';
  const le = document.createElement('span');
  le.className = 'l-entrada';
  le.textContent = 'Entradas';
  const ls = document.createElement('span');
  ls.className = 'l-saida';
  ls.textContent = 'Saidas';
  legenda.append(le, ls);
  alvo.appendChild(legenda);
}

// ---------------------------------------------------------------- por banco

function renderBancos() {
  const mapa = new Map();
  transacoes.forEach((t) => {
    if (!mapa.has(t.banco)) mapa.set(t.banco, { banco: t.banco, entradas: 0, saidas: 0, qtd: 0 });
    const b = mapa.get(t.banco);
    b.qtd += 1;
    const s = sinal(t.tipo);
    if (s > 0) b.entradas += t.valor;
    else if (s < 0) b.saidas += t.valor;
  });
  const bancos = [...mapa.values()].sort(
    (a, b) => b.entradas + b.saidas - (a.entradas + a.saidas)
  );

  renderLista(
    '#lista-bancos',
    bancos,
    (b) => {
      const saldo = b.entradas - b.saidas;
      const el = document.createElement('div');
      el.className = 'item';
      el.innerHTML =
        '<div class="corpo"><div class="titulo"></div><div class="meta"></div></div>' +
        '<div class="valor ' + (saldo >= 0 ? 'entrada' : 'saida') + '"></div>';
      el.querySelector('.valor').textContent = moeda.format(saldo);
      el.querySelector('.titulo').textContent = b.banco;
      el.querySelector('.meta').textContent =
        b.qtd +
        ' lancamento(s) - entradas ' +
        moeda.format(b.entradas) +
        ' - saidas ' +
        moeda.format(b.saidas);
      return el;
    },
    'Nenhum banco registrado.'
  );
}

// ---------------------------------------------------------------- agendados

function renderAgendados() {
  renderLista(
    '#lista-agendados',
    banco.agendadas(),
    (t) => {
      const el = document.createElement('div');
      el.className = 'item';
      el.innerHTML =
        '<div class="corpo"><div class="titulo"></div><div class="meta"></div></div>' +
        '<div class="valor"></div>';
      el.querySelector('.valor').textContent = moeda.format(t.valor);
      el.querySelector('.titulo').textContent = t.descricao || ROTULO_TIPO[t.tipo] || t.tipo;
      const quando = t.dataAgendada ? dataHora.format(new Date(t.dataAgendada)) : 'sem data';
      el.querySelector('.meta').textContent = t.banco + ' - previsto para ' + quando;
      return el;
    },
    'Nenhum lancamento agendado.'
  );
}

// ---------------------------------------------------------------- investimentos

function renderInvestimentos() {
  renderLista(
    '#lista-investimentos',
    banco.investimentos(),
    (inv) => {
      const el = document.createElement('div');
      el.className = 'item';
      el.innerHTML =
        '<div class="corpo"><div class="titulo"></div><div class="meta"></div></div>' +
        '<div class="valor ' + (inv.lucro >= 0 ? 'entrada' : 'saida') + '"></div>';
      el.querySelector('.valor').textContent = moeda.format(inv.valorAtual);

      const titulo = el.querySelector('.titulo');
      titulo.textContent = inv.nome;
      if (inv.ticker) {
        const tag = document.createElement('span');
        tag.className = 'etiqueta';
        tag.textContent = inv.ticker;
        titulo.append(' ', tag);
      }

      const categoria = ROTULO_CATEGORIA_INVESTIMENTO[inv.categoria] || inv.categoria;
      el.querySelector('.meta').textContent =
        categoria +
        ' - aplicado ' +
        moeda.format(inv.valorInvestido) +
        ' - resultado ' +
        (inv.lucro >= 0 ? '+' : '') +
        moeda.format(inv.lucro) +
        ' (' +
        inv.rentabilidadePct.toFixed(2) +
        '%)';
      return el;
    },
    'Nenhum investimento cadastrado.'
  );
}

// ---------------------------------------------------------------- exportacao

function exportarCsv() {
  const colunas = ['id', 'banco', 'tipo', 'valor', 'descricao', 'timestampMs', 'recorrente'];
  const escapar = (v) => '"' + String(v === null || v === undefined ? '' : v).replace(/"/g, '""') + '"';
  const linhas = [colunas.join(';')];
  transacoesFiltradas().forEach((t) => {
    linhas.push(
      colunas
        .map((c) => (c === 'timestampMs' ? escapar(new Date(t[c]).toISOString()) : escapar(t[c])))
        .join(';')
    );
  });
  // BOM para o Excel em pt-BR reconhecer os acentos
  const blob = new Blob(['﻿' + linhas.join('\r\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'finguia-extrato.csv';
  a.click();
  URL.revokeObjectURL(url);
}

// ---------------------------------------------------------------- tema

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
  const preferido = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro';
  aplicarTema(salvo || preferido);
}

// ---------------------------------------------------------------- inicializacao

function ligarPainel() {
  $('#busca').addEventListener('input', renderExtrato);
  $('#filtro-tipo').addEventListener('change', renderExtrato);
  $('#filtro-banco').addEventListener('change', renderExtrato);
  $('#btn-csv').addEventListener('click', exportarCsv);
  $('#btn-trocar').addEventListener('click', () => location.reload());
  $('#btn-tema').addEventListener('click', () =>
    aplicarTema(document.documentElement.dataset.tema === 'escuro' ? 'claro' : 'escuro')
  );

  document.querySelectorAll('.aba').forEach((aba) => {
    aba.addEventListener('click', () => {
      document.querySelectorAll('.aba').forEach((a) => a.classList.toggle('ativa', a === aba));
      document.querySelectorAll('.conteudo-aba').forEach((c) => {
        c.hidden = c.id !== 'aba-' + aba.dataset.aba;
      });
    });
  });
}

iniciarTema();
ligarCarregamento();
ligarPainel();

// Atalho: se o servidor local encontrou um banco em web/dados/, abre direto.
fetch('dados/finguia_database')
  .then((r) => (r.ok ? r.arrayBuffer() : Promise.reject(new Error('sem banco local'))))
  .then(async (bytes) => {
    if (banco) return;
    banco = await BancoFinGuia.abrir(bytes);
    transacoes = banco.transacoes();
    montarPainel('dados/finguia_database');
  })
  .catch(() => {
    /* sem banco local: segue na tela de carregamento */
  });
