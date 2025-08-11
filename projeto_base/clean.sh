#!/bin/bash
set -euo pipefail

# >>> Ajuste este caminho se o seu ZK estiver em outro lugar
ZK="${ZK:-$HOME/tmp-ufabc/apache-zookeeper-3.9.3-bin}"
SERVER="${SERVER:-localhost:2181}"

ZKCLI="$ZK/bin/zkCli.sh"
ZKSRV="$ZK/bin/zkServer.sh"
ZKCFG="$ZK/conf/zoo.cfg"

# Paths do seu programa
PATHS=(
  /exam_queue
  /locks
  /gabarito
  /results
  /barrier
  /election
  /final
  /leader
)

usage() {
  echo "Uso: $0 [--full]"
  echo "  (sem flags)   => mata ExamSystem e deleta nós do app no ZooKeeper"
  echo "  --full        => reset TOTAL: apaga dataDir do ZK e reinicia"
}

# -------- helpers
zk_cmd() {
  # executa um comando no zkCli silenciosamente
  echo "$1" | "$ZKCLI" -server "$SERVER" >/dev/null
}

delpath() {
  local p="$1"
  # Tenta 'deleteall' (recursivo); se falhar, tenta 'rmr'
  if ! zk_cmd "deleteall $p"; then
    zk_cmd "rmr $p" || true
  fi
}

# -------- matar processos ExamSystem
echo "[1/3] Matando processos ExamSystem..."
pkill -f ExamSystem || true

if [[ "${1:-}" == "--full" ]]; then
  echo "[2/3] Reset TOTAL do ZooKeeper (apagando dataDir)..."
  # Descobre dataDir do zoo.cfg
  if [[ ! -f "$ZKCFG" ]]; then
    echo "ERRO: zoo.cfg não encontrado em $ZKCFG"; exit 1
  fi
  DATADIR="$(grep -E '^dataDir=' "$ZKCFG" | cut -d= -f2)"
  if [[ -z "${DATADIR:-}" ]]; then
    echo "ERRO: dataDir não encontrado em $ZKCFG"; exit 1
  fi

  echo "Parando servidor..."
  "$ZKSRV" stop >/dev/null || true
  echo "Apagando $DATADIR ..."
  rm -rf "$DATADIR"
  mkdir -p "$DATADIR"

  echo "Iniciando servidor..."
  "$ZKSRV" start >/dev/null
  sleep 2

  echo "[3/3] Verificando que está limpo..."
  "$ZKCLI" -server "$SERVER" ls / 2>/dev/null | tail -n +1
  echo "OK. ZooKeeper resetado."
  exit 0
fi

echo "[2/3] Limpando nós do aplicativo no ZooKeeper ($SERVER)..."
for p in "${PATHS[@]}"; do
  echo " - deletando $p"
  delpath "$p"
done

echo "[3/3] Conferindo..."
"$ZKCLI" -server "$SERVER" ls / 2>/dev/null | tail -n +1
echo "Pronto. Nós do app removidos."
