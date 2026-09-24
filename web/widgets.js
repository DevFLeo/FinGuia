// Blocos que os templates combinam (dashboards.js). Cada bloco recebe o
// contexto com os dados ja lidos do banco e devolve um elemento pronto.
// Nenhum bloco usa id fixo: o atributo data-bloco identifica cada um.

import { ROTULO_TIPO, ROTULO_CATEGORIA_INVESTIMENTO, sinal } from './esquema.js';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const mesCurto = new Intl.DateTimeFormat('pt-BR', { month: 'short', year: '2-digit' });

export const NOMES_BLOCOS = {
  extrato: 'Extrato',
  fluxo: 'Fluxo mensal',
  bancos: 'Por banco',
  agendados: 'Agendados',
  investimentos: 'Investimentos',
  gastosPorTipo: 'Saídas por tipo',
  maioresGastos: 'Maiores gastos',
};

// ------------------------------------------------------------------ ajudantes

function el(tag, classe, texto) {
  const e = document.createElement(tag);
  if (classe) e.className = classe;
  if (texto !== undefined) e.textContent = texto;
  return e;
}

function cartao(bloco, titulo) {
  const c = el('section', 'cartao bloco');
  c.dataset.bloco = bloco;
  if (titulo) c.appendChild(el('h3', 'titulo-bloco', titulo));
  return c;
}

function item(titulo, meta, valor, classeValor) {
  const linha = el('div', 'item');
  const corpo = el('div', 'corpo');
  corpo.append(el('div', 'titulo', titulo), el('div', 'meta', meta));
  linha.append(corpo, el('div', 'valor ' + (classeValor || ''), valor));
  return linha;
}

function vazio(texto) {
  return el('p', 'vazio', texto);
}

function metrica(chave, rotulo, valor, classe) {
  const m = el('article', 'cartao metrica ' + (classe || ''));
  m.dataset.metrica = chave;
  m.append(el('span', 'rotulo', rotulo), el('strong', null, valor));
  return m;
}

function linhaMetricas(...metricas) {
  const l = el('div', 'linha-resumo');
  l.append(...metricas);
  return l;
}

export const saidas = (transacoes) => transacoes.filter((t) => sinal(t.tipo) < 0);

function itemTransacao(t) {
  const s = sinal(t.tipo);
  const classe = s > 0 ? 'entrada' : s < 0 ? 'saida' : 'neutro';
  const prefixo = s > 0 ? '+' : s < 0 ? '-' : '';
  const meta = t.banco + ' · ' + (ROTULO_TIPO[t.tipo] || t.tipo) + ' · ' +
    dataHora.format(new Date(t.timestampMs)) + (t.recorrente ? ' · recorrente' : '');
  return item(t.descricao || ROTULO_TIPO[t.tipo] || t.tipo, meta, prefixo + moeda.format(Math.abs(t.valor)), classe);
}

// ------------------------------------------------------------------- metricas

export function metricas(ctx) {
  const { receitas, despesas, saldo } = ctx.totais;
  const bloco = el('div');
  bloco.dataset.bloco = 'metricas';
  bloco.appendChild(linhaMetricas(
    metrica('receitas', 'Receitas', moeda.format(receitas), 'entrada'),
    metrica('despesas', 'Despesas', moeda.format(despesas), 'saida'),
    metrica('saldo', 'Saldo', moeda.format(saldo), saldo < 0 ? 'saida' : 'entrada'),
    metrica('lancamentos', 'Lançamentos', String(ctx.transacoes.length)),
  ));
  return bloco;
}

export function metricasGastos(ctx) {
  const lista = saidas(ctx.transacoes);
  const total = lista.reduce((s, t) => s + t.valor, 0);
  const maior = lista.reduce((m, t) => (t.valor > m ? t.valor : m), 0);
  const bloco = el('div');
  bloco.dataset.bloco = 'metricasGastos';
  bloco.appendChild(linhaMetricas(
    metrica('despesas', 'Total gasto', moeda.format(total), 'saida'),
    metrica('saidas', 'Saídas', String(lista.length)),
    metrica('media', 'Média por saída', moeda.format(lista.length ? total / lista.length : 0)),
    metrica('maior', 'Maior saída', moeda.format(maior)),
  ));
  return bloco;
}

export function metricasInvestimentos(ctx) {
  const inv = ctx.investimentos;
  const aplicado = inv.reduce((s, i) => s + i.valorInvestido, 0);
  const atual = inv.reduce((s, i) => s + i.valorAtual, 0);
  const resultado = atual - aplicado;
  const bloco = el('div');
  bloco.dataset.bloco = 'metricasInvestimentos';
  bloco.appendChild(linhaMetricas(
    metrica('aplicado', 'Aplicado', moeda.format(aplicado)),
    metrica('atual', 'Valor atual', moeda.format(atual)),
    metrica('resultado', 'Resultado', (resultado >= 0 ? '+' : '') + moeda.format(resultado), resultado < 0 ? 'saida' : 'entrada'),
    metrica('ativos', 'Ativos', String(inv.length)),
  ));
  return bloco;
}

// -------------------------------------------------------------------- extrato

export function extrato(ctx) {
  const c = cartao('extrato');
  const filtros = el('div', 'filtros');
  const busca = el('input');
  busca.type = 'search';
  busca.placeholder = 'Buscar por descrição, banco ou notificação…';
  busca.dataset.campo = 'busca';
  const tipo = el('select');
  tipo.dataset.campo = 'tipo';
  const banco = el('select');
  banco.dataset.campo = 'banco';
  const opcoes = (sel, primeira, valores, rotulo) => {
    sel.appendChild(Object.assign(el('option', null, primeira), { value: '' }));
    valores.forEach((v) => sel.appendChild(Object.assign(el('option', null, rotulo(v)), { value: v })));
  };
  opcoes(tipo, 'Todos os tipos', [...new Set(ctx.transacoes.map((t) => t.tipo))].sort(), (t) => ROTULO_TIPO[t] || t);
  opcoes(banco, 'Todos os bancos', [...new Set(ctx.transacoes.map((t) => t.banco))].sort(), (b) => b);
  filtros.append(busca, tipo, banco);

  const lista = el('div', 'lista');
  const filtrar = () => {
    const termo = busca.value.trim().toLowerCase();
    return ctx.transacoes.filter((t) => {
      if (tipo.value && t.tipo !== tipo.value) return false;
      if (banco.value && t.banco !== banco.value) return false;
      if (!termo) return true;
      return [t.descricao, t.banco, t.tituloNotificacao, t.textoNotificacao]
        .some((campo) => (campo || '').toLowerCase().includes(termo));
    });
  };
  const desenhar = () => {
    const itens = filtrar();
    lista.replaceChildren(...(itens.length ? itens.map(itemTransacao) : [vazio('Nenhum lançamento encontrado com esses filtros.')]));
  };
  busca.addEventListener('input', desenhar);
  tipo.addEventListener('change', desenhar);
  banco.addEventListener('change', desenhar);
  desenhar();

  // A exportacao CSV usa o que o extrato visivel esta filtrando
  ctx.estado.filtrarExtrato = filtrar;
  c.append(filtros, lista);
  return c;
}

// ---------------------------------------------------------------------- fluxo

export function fluxo(ctx) {
  const c = cartao('fluxo', 'Entradas e saídas por mês');
  const meses = new Map();
  ctx.transacoes.forEach((t) => {
    const d = new Date(t.timestampMs);
    const chave = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
    if (!meses.has(chave)) meses.set(chave, { chave, data: d, entradas: 0, saidas: 0 });
    const s = sinal(t.tipo);
    if (s > 0) meses.get(chave).entradas += t.valor;
    else if (s < 0) meses.get(chave).saidas += t.valor;
  });
  const dados = [...meses.values()].sort((a, b) => a.chave.localeCompare(b.chave)).slice(-12);
  if (!dados.length) {
    c.appendChild(vazio('Sem lançamentos para montar o gráfico.'));
    return c;
  }

  const L = 720, A = 260, base = A - 34, topo = 16;
  const maximo = Math.max(...dados.flatMap((m) => [m.entradas, m.saidas]), 1);
  const largura = L / dados.length;
  const barra = Math.min(22, largura / 3);
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', '0 0 ' + L + ' ' + A);
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', 'Entradas e saídas por mês');
  const eixo = document.createElementNS(ns, 'line');
  [['x1', 0], ['x2', L], ['y1', base], ['y2', base], ['stroke', 'var(--borda)']].forEach(([k, v]) => eixo.setAttribute(k, v));
  svg.appendChild(eixo);

  dados.forEach((m, i) => {
    const centro = i * largura + largura / 2;
    [['entradas', 'var(--entrada)', -barra - 2], ['saidas', 'var(--saida)', 2]].forEach(([campo, cor, desloc]) => {
      const altura = (m[campo] / maximo) * (base - topo);
      const r = document.createElementNS(ns, 'rect');
      [['x', centro + desloc], ['y', base - altura], ['width', barra], ['height', Math.max(altura, 0)], ['rx', 3], ['fill', cor]]
        .forEach(([k, v]) => r.setAttribute(k, v));
      const dica = document.createElementNS(ns, 'title');
      dica.textContent = mesCurto.format(m.data) + ' — ' + campo + ': ' + moeda.format(m[campo]);
      r.appendChild(dica);
      svg.appendChild(r);
    });
    const rotulo = document.createElementNS(ns, 'text');
    [['x', centro], ['y', base + 18], ['text-anchor', 'middle'], ['font-size', '11'], ['fill', 'var(--texto-fraco)']]
      .forEach(([k, v]) => rotulo.setAttribute(k, v));
    rotulo.textContent = mesCurto.format(m.data);
    svg.appendChild(rotulo);
  });

  const legenda = el('div', 'legenda');
  legenda.append(el('span', 'l-entrada', 'Entradas'), el('span', 'l-saida', 'Saídas'));
  c.append(svg, legenda);
  return c;
}

// --------------------------------------------------------------------- bancos

export function bancos(ctx) {
  const c = cartao('bancos', 'Por banco');
  const mapa = new Map();
  ctx.transacoes.forEach((t) => {
    if (!mapa.has(t.banco)) mapa.set(t.banco, { banco: t.banco, entradas: 0, saidas: 0, qtd: 0 });
    const b = mapa.get(t.banco);
    b.qtd += 1;
    const s = sinal(t.tipo);
    if (s > 0) b.entradas += t.valor;
    else if (s < 0) b.saidas += t.valor;
  });
  const lista = [...mapa.values()].sort((a, b) => b.entradas + b.saidas - (a.entradas + a.saidas));
  const div = el('div', 'lista');
  div.replaceChildren(...(lista.length ? lista.map((b) => {
    const saldo = b.entradas - b.saidas;
    return item(b.banco, b.qtd + ' lançamento(s) · entradas ' + moeda.format(b.entradas) + ' · saídas ' + moeda.format(b.saidas),
      moeda.format(saldo), saldo >= 0 ? 'entrada' : 'saida');
  }) : [vazio('Nenhum banco registrado.')]));
  c.appendChild(div);
  return c;
}

// ------------------------------------------------------------------ agendados

export function agendados(ctx) {
  const c = cartao('agendados', 'Agendados');
  const div = el('div', 'lista');
  div.replaceChildren(...(ctx.agendadas.length ? ctx.agendadas.map((t) => {
    const quando = t.dataAgendada ? dataHora.format(new Date(t.dataAgendada)) : 'sem data';
    return item(t.descricao || ROTULO_TIPO[t.tipo] || t.tipo, t.banco + ' · previsto para ' + quando, moeda.format(t.valor));
  }) : [vazio('Nenhum lançamento agendado.')]));
  c.appendChild(div);
  return c;
}

// -------------------------------------------------------------- investimentos

export function investimentos(ctx) {
  const c = cartao('investimentos', 'Investimentos');
  const div = el('div', 'lista');
  div.replaceChildren(...(ctx.investimentos.length ? ctx.investimentos.map((inv) => {
    const categoria = ROTULO_CATEGORIA_INVESTIMENTO[inv.categoria] || inv.categoria;
    const meta = categoria + ' · aplicado ' + moeda.format(inv.valorInvestido) + ' · resultado ' +
      (inv.lucro >= 0 ? '+' : '') + moeda.format(inv.lucro) + ' (' + inv.rentabilidadePct.toFixed(2).replace('.', ',') + '%)';
    const linha = item(inv.nome + (inv.ticker ? ' · ' + inv.ticker : ''), meta, moeda.format(inv.valorAtual),
      inv.lucro >= 0 ? 'entrada' : 'saida');
    return linha;
  }) : [vazio('Nenhum investimento cadastrado.')]));
  c.appendChild(div);
  return c;
}

// -------------------------------------------------------------- gastos por tipo

export function gastosPorTipo(ctx) {
  const c = cartao('gastosPorTipo', 'Saídas por tipo');
  const lista = saidas(ctx.transacoes);
  const total = lista.reduce((s, t) => s + t.valor, 0);
  if (!total) {
    c.appendChild(vazio('Nenhuma saída registrada.'));
    return c;
  }
  const porTipo = new Map();
  lista.forEach((t) => porTipo.set(t.tipo, (porTipo.get(t.tipo) || 0) + t.valor));
  const barras = el('div', 'barras');
  [...porTipo.entries()].sort((a, b) => b[1] - a[1]).forEach(([tipo, valor]) => {
    const pct = (valor / total) * 100;
    const linha = el('div', 'barra-linha');
    linha.dataset.tipo = tipo;
    const topo = el('div', 'barra-topo');
    topo.append(el('span', null, ROTULO_TIPO[tipo] || tipo),
      el('span', 'barra-valor', moeda.format(valor) + ' · ' + pct.toFixed(1).replace('.', ',') + '%'));
    const trilho = el('div', 'barra-trilho');
    const preenchido = el('div', 'barra-preenchida');
    preenchido.style.width = pct.toFixed(1) + '%';
    trilho.appendChild(preenchido);
    linha.append(topo, trilho);
    barras.appendChild(linha);
  });
  c.appendChild(barras);
  return c;
}

// ------------------------------------------------------------ maiores gastos

export function maioresGastos(ctx) {
  const c = cartao('maioresGastos', 'Maiores gastos');
  const top = saidas(ctx.transacoes).sort((a, b) => b.valor - a.valor).slice(0, 5);
  const div = el('div', 'lista');
  div.replaceChildren(...(top.length ? top.map(itemTransacao) : [vazio('Nenhuma saída registrada.')]));
  c.appendChild(div);
  return c;
}

export const BLOCOS = {
  metricas, metricasGastos, metricasInvestimentos, extrato, fluxo, bancos,
  agendados, investimentos, gastosPorTipo, maioresGastos,
};
