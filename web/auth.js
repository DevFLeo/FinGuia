// Login SIMULADO do FinGuia Web.
//
// Nao ha servidor de contas: usuarios e senhas ficam no localStorage deste
// navegador. A senha nunca e guardada em texto, so o hash SHA-256 de
// (sal aleatorio + senha). Serve para demonstrar o fluxo de login, sair e
// dados por usuario; NAO protege nada de quem tem acesso ao computador.

const CHAVE_USUARIOS = 'finguia-usuarios';
const CHAVE_SESSAO = 'finguia-sessao';

export const CONTA_DEMO = { usuario: 'demo', senha: 'finguia' };
export const SENHA_MINIMA = 4;

function ler(storage, chave, padrao) {
  try {
    const v = storage.getItem(chave);
    return v ? JSON.parse(v) : padrao;
  } catch (e) {
    return padrao;
  }
}

function gravar(storage, chave, valor) {
  try {
    storage.setItem(chave, JSON.stringify(valor));
    return true;
  } catch (e) {
    return false;
  }
}

function normalizarUsuario(usuario) {
  return String(usuario || '').trim().toLowerCase();
}

function paraHex(buffer) {
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

async function hashSenha(sal, senha) {
  if (!globalThis.crypto || !globalThis.crypto.subtle) {
    // crypto.subtle so existe em contexto seguro (https ou 127.0.0.1)
    throw new Error('Abra pelo finguia-web.bat (http://127.0.0.1): o navegador bloqueia a criptografia fora dele.');
  }
  const dados = new TextEncoder().encode(sal + ':' + senha);
  return paraHex(await globalThis.crypto.subtle.digest('SHA-256', dados));
}

function novoSal() {
  const bytes = new Uint8Array(16);
  globalThis.crypto.getRandomValues(bytes);
  return paraHex(bytes);
}

function usuarios() {
  return ler(localStorage, CHAVE_USUARIOS, {});
}

/** Cria uma conta. Lanca erro com mensagem para o usuario se nao der. */
export async function criarConta(usuario, senha) {
  const nome = normalizarUsuario(usuario);
  if (!/^[a-z0-9._-]{3,30}$/.test(nome)) {
    throw new Error('Usuário deve ter de 3 a 30 letras, números, ponto, hífen ou sublinhado.');
  }
  if (String(senha || '').length < SENHA_MINIMA) {
    throw new Error('A senha precisa ter pelo menos ' + SENHA_MINIMA + ' caracteres.');
  }
  const todos = usuarios();
  if (todos[nome]) throw new Error('Esse usuário já existe.');
  const sal = novoSal();
  todos[nome] = { sal, hash: await hashSenha(sal, senha), criadoEm: Date.now() };
  if (!gravar(localStorage, CHAVE_USUARIOS, todos)) {
    throw new Error('O navegador bloqueou o armazenamento local (modo privado?).');
  }
  return nome;
}

/** Confere usuario e senha. Mesma mensagem para os dois erros, de proposito. */
export async function entrar(usuario, senha) {
  const nome = normalizarUsuario(usuario);
  const conta = usuarios()[nome];
  const erro = new Error('Usuário ou senha incorretos.');
  if (!conta) throw erro;
  if ((await hashSenha(conta.sal, String(senha || ''))) !== conta.hash) throw erro;
  gravar(sessionStorage, CHAVE_SESSAO, { usuario: nome, desde: Date.now() });
  return nome;
}

/** Usuario logado nesta aba, ou null. A sessao acaba ao fechar a aba ou em sair(). */
export function usuarioAtual() {
  const s = ler(sessionStorage, CHAVE_SESSAO, null);
  return s && s.usuario && usuarios()[s.usuario] ? s.usuario : null;
}

export function sair() {
  try {
    sessionStorage.removeItem(CHAVE_SESSAO);
  } catch (e) {
    /* nada a limpar */
  }
}

/** Garante que a conta de demonstracao existe, para apresentar sem cadastro. */
export async function garantirContaDemo() {
  if (usuarios()[CONTA_DEMO.usuario]) return;
  try {
    await criarConta(CONTA_DEMO.usuario, CONTA_DEMO.senha);
  } catch (e) {
    /* sem armazenamento: o login mostra o erro ao tentar entrar */
  }
}
