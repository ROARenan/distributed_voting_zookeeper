#!/bin/bash

# Script de demonstração completa do Sistema de Votação Distribuída
# Este script automatiza todo o processo de execução

echo "=========================================="
echo "    SISTEMA DE VOTAÇÃO DISTRIBUÍDA"
echo "   Demonstração com Apache ZooKeeper"
echo "=========================================="
echo ""

# Configurar caminhos
export PROJECT_ROOT="/home/ferdinando-longoni/tmp-ufabc/distributed_voting_zookeeper"
export ZK_HOME="$PROJECT_ROOT/apache-zookeeper-3.9.3"
export PROJETO_BASE="$PROJECT_ROOT/projeto_base"

# Verificar se diretórios existem
if [ ! -d "$ZK_HOME" ]; then
    echo "Erro: Diretório ZooKeeper não encontrado: $ZK_HOME"
    exit 1
fi

if [ ! -d "$PROJETO_BASE" ]; then
    echo "Erro: Diretório do projeto não encontrado: $PROJETO_BASE"
    exit 1
fi

echo "  Diretórios verificados:"
echo "   ZooKeeper: $ZK_HOME"
echo "   Projeto: $PROJETO_BASE"
echo ""

# Função para verificar se ZooKeeper está rodando
check_zookeeper() {
    echo "🔍 Verificando se ZooKeeper está rodando..."
    
    # Tentar conectar na porta 2181
    if nc -z localhost 2181 2>/dev/null; then
        echo "- ZooKeeper está rodando na porta 2181"
        return 0
    else
        echo "- ZooKeeper não está rodando"
        return 1
    fi
}

# Função para iniciar ZooKeeper
start_zookeeper() {
    echo " -- Iniciando ZooKeeper..."
    
    cd "$ZK_HOME"
    
    # Criar diretório de dados se não existir
    mkdir -p /tmp/zookeeper
    
    # Iniciar ZooKeeper em background
    nohup bin/zkServer.sh start > /tmp/zookeeper-startup.log 2>&1 &
    
    # Aguardar ZooKeeper inicializar
    echo " - Aguardando ZooKeeper inicializar..."
    for i in {1..10}; do
        if check_zookeeper; then
            echo "- ZooKeeper iniciado com sucesso!"
            return 0
        fi
        echo "   Tentativa $i/10..."
        sleep 2
    done
    
    echo " - Falha ao iniciar ZooKeeper"
    echo "- Log de inicialização:"
    cat /tmp/zookeeper-startup.log
    return 1
}

# Função para compilar o projeto
compile_project() {
    echo " Compilando sistema de votação Java..."
    
    cd "$PROJETO_BASE"
    
    # Configurar classpath
    export CP_ZK=".:$ZK_HOME/lib/zookeeper-3.9.3.jar:$ZK_HOME/lib/zookeeper-jute-3.9.3.jar:$ZK_HOME/lib/slf4j-api-1.7.30.jar:$ZK_HOME/lib/logback-core-1.2.13.jar:$ZK_HOME/lib/logback-classic-1.2.13.jar:$ZK_HOME/lib/netty-handler-4.1.113.Final.jar"
    
    # Compilar
    javac -cp "$CP_ZK" src/votacao/*.java
    
    if [ $? -eq 0 ]; then
        echo " Compilação concluída com sucesso!"
        return 0
    else
        echo " Erro na compilação!"
        return 1
    fi
}

# Função para executar um nó de votação
run_voting_node() {
    local node_id=$1
    local log_file="/tmp/voting_node_$node_id.log"
    
    cd "$PROJETO_BASE"
    
    export CP_ZK=".:$ZK_HOME/lib/zookeeper-3.9.3.jar:$ZK_HOME/lib/zookeeper-jute-3.9.3.jar:$ZK_HOME/lib/slf4j-api-1.7.30.jar:$ZK_HOME/lib/logback-core-1.2.13.jar:$ZK_HOME/lib/logback-classic-1.2.13.jar:$ZK_HOME/lib/netty-handler-4.1.113.Final.jar"
    
    echo " Iniciando nó de votação $node_id..."
    
    # Executar em background e capturar saída
    java -cp "$CP_ZK:src" \
         -Dlogback.configurationFile=file:$ZK_HOME/conf/logback.xml \
         src.votacao.SistemaVotacao > "$log_file" 2>&1 &
    
    local pid=$!
    echo "   PID: $pid"
    echo "   Log: $log_file"
    
    return $pid
}

# Função para monitorar logs
monitor_logs() {
    echo ""
    echo " Monitorando execução..."
    echo "   Pressione Ctrl+C para parar o monitoramento"
    echo ""
    
    # Monitorar logs de todos os nós
    tail -f /tmp/voting_node_*.log 2>/dev/null &
    local tail_pid=$!
    
    # Aguardar input do usuário
    read -p "Pressione Enter para parar o monitoramento..."
    
    # Parar monitoramento
    kill $tail_pid 2>/dev/null
}

# Função para limpar processos
cleanup() {
    echo ""
    echo "  Limpando processos e Zooekeeper."
    
    # Parar nós de votação
    pkill -f "src.votacao.SistemaVotacao" 2>/dev/null
    
    # Parar ZooKeeper
    cd "$ZK_HOME"
    bin/zkServer.sh stop 2>/dev/null
    
    echo " Limpeza concluída"
}

# Função principal
main() {
    # Verificar/Iniciar ZooKeeper
    if ! check_zookeeper; then
        if ! start_zookeeper; then
            echo "❌ Falha ao iniciar ZooKeeper. Abortando."
            exit 1
        fi
    fi
    
    # Compilar projeto
    if ! compile_project; then
        echo "❌ Falha na compilação. Abortando."
        cleanup
        exit 1
    fi
    
    echo ""
    echo " =========================================="
    echo "   INICIANDO DEMONSTRAÇÃO"
    echo " =========================================="
    echo ""
    
    # Limpar logs anteriores
    rm -f /tmp/voting_node_*.log
    
    # Executar múltiplos nós
    echo " Iniciando nós de votação..."
    
    run_voting_node 1
    run_voting_node 2
    run_voting_node 3
    
    echo ""
    echo " Todos os nós iniciados!"
    echo ""
    echo " Aguardando 5 segundos para sincronização..."
    sleep 5
    
    # Monitorar logs
    monitor_logs
    
    # Limpeza final
    cleanup
}

# Configurar trap para limpeza em caso de interrupção
trap cleanup EXIT

# Executar função principal
main

echo ""
echo " Demonstração finalizada!"
echo " Logs salvos em /tmp/voting_node_*.log"
