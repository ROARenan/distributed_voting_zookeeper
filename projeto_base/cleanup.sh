echo ""
echo " Limpando processos..."

# Parar nós de votação
pkill -f "src.votacao.SistemaVotacao" 2>/dev/null

# Parar ZooKeeper
cd "$ZK_HOME"
bin/zkServer.sh stop 2>/dev/null

echo "Limpeza concluída"