#!/usr/bin/env bash
# Executavel do FinGuia Web para Linux/macOS: sobe o servidor local e abre o navegador.
# Uso:  ./finguia-web.sh                  (procura o banco em web/dados/)
#       ./finguia-web.sh caminho/do/banco (usa o banco informado)
set -euo pipefail

if ! command -v node >/dev/null 2>&1; then
    echo "[ERRO] Node.js nao encontrado no PATH."
    echo "Instale em https://nodejs.org e rode este arquivo de novo."
    exit 1
fi

cd "$(dirname "$0")"

if [ $# -eq 0 ]; then
    exec node servidor.js
else
    exec node servidor.js --banco "$1"
fi
