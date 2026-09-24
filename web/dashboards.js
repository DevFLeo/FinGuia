// Dashboards do usuario: cada um tem nome e um template. O template diz quais
// blocos (widgets.js) aparecem e em que arranjo. Salvos por usuario no
// localStorage deste navegador.

export const TEMPLATES = {
  completo: {
    nome: 'Completo',
    descricao: 'Totais e abas com extrato, fluxo mensal, bancos, agendados e investimentos.',
    abas: true,
    blocos: ['metricas', 'extrato', 'fluxo', 'bancos', 'agendados', 'investimentos'],
  },
  resumo: {
    nome: 'Resumo',
    descricao: 'Visão rápida: totais, fluxo mensal e os maiores gastos.',
    abas: false,
    blocos: ['metricas', 'fluxo', 'maioresGastos'],
  },
  gastos: {
    nome: 'Gastos',
    descricao: 'Para onde vai o dinheiro: saídas por tipo, maiores gastos e por banco.',
    abas: false,
    blocos: ['metricasGastos', 'gastosPorTipo', 'maioresGastos', 'bancos'],
  },
  investimentos: {
    nome: 'Investimentos',
    descricao: 'Carteira: total aplicado, resultado e cada investimento.',
    abas: false,
    blocos: ['metricasInvestimentos', 'investimentos'],
  },
};

export const TEMPLATE_PADRAO = 'completo';

const chaveLista = (usuario) => 'finguia-dashboards:' + usuario;
const chaveAtivo = (usuario) => 'finguia-dashboard-ativo:' + usuario;

function ler(chave, padrao) {
  try {
    const v = localStorage.getItem(chave);
    return v ? JSON.parse(v) : padrao;
  } catch (e) {
    return padrao;
  }
}

function gravar(chave, valor) {
  try {
    localStorage.setItem(chave, JSON.stringify(valor));
  } catch (e) {
    /* sem armazenamento: continua funcionando ate recarregar */
  }
}

function novoId() {
  return 'd' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
}

/** Dashboards do usuario. Na primeira vez cria o "Principal" com o template completo. */
export function listar(usuario) {
  let lista = ler(chaveLista(usuario), null);
  if (!Array.isArray(lista) || lista.length === 0) {
    lista = [{ id: novoId(), nome: 'Principal', template: TEMPLATE_PADRAO }];
    gravar(chaveLista(usuario), lista);
  }
  // Template removido numa versao futura cai no padrao em vez de quebrar
  return lista.map((d) => (TEMPLATES[d.template] ? d : { ...d, template: TEMPLATE_PADRAO }));
}

export function ativo(usuario) {
  const lista = listar(usuario);
  const id = ler(chaveAtivo(usuario), null);
  return lista.find((d) => d.id === id) || lista[0];
}

export function ativar(usuario, id) {
  gravar(chaveAtivo(usuario), id);
}

/** Cria e ja ativa um dashboard. Lanca erro com mensagem para o usuario. */
export function criar(usuario, nome, template) {
  const limpo = String(nome || '').trim();
  if (!limpo) throw new Error('Dê um nome ao dashboard.');
  if (limpo.length > 40) throw new Error('Nome muito longo (máximo 40 caracteres).');
  if (!TEMPLATES[template]) throw new Error('Escolha um template.');
  const lista = listar(usuario);
  if (lista.some((d) => d.nome.toLowerCase() === limpo.toLowerCase())) {
    throw new Error('Já existe um dashboard com esse nome.');
  }
  const novo = { id: novoId(), nome: limpo, template };
  gravar(chaveLista(usuario), [...lista, novo]);
  ativar(usuario, novo.id);
  return novo;
}

/** Exclui um dashboard. O ultimo nao pode ser excluido. */
export function excluir(usuario, id) {
  const lista = listar(usuario);
  if (lista.length <= 1) throw new Error('É preciso manter pelo menos um dashboard.');
  const resto = lista.filter((d) => d.id !== id);
  gravar(chaveLista(usuario), resto);
  // Se o excluido era o ativo, o primeiro que sobrou passa a ser
  if (!resto.some((d) => d.id === ler(chaveAtivo(usuario), null))) {
    ativar(usuario, resto[0].id);
  }
}
