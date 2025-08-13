#!/bin/bash

# Script para executar o sistema de votação distribuída
export ZK=/home/roa/Desktop/code/final_zookeeper/apache-zookeeper-3.9.3
echo "ZK=$ZK"

export CP_ZK=.:$ZK'/lib/zookeeper-3.9.3.jar':$ZK'/lib/zookeeper-jute-3.9.3.jar':$ZK'/lib/slf4j-api-1.7.30.jar':$ZK'/lib/logback-core-1.2.13.jar':$ZK'/lib/logback-classic-1.2.13.jar':$ZK'/lib/netty-handler-4.1.113.Final.jar'
echo "CP_ZK=$CP_ZK"

echo "Compilando sistema de votação..."
javac -cp $CP_ZK src/votacao/*.java

if [ $? -eq 0 ]; then
    echo "Compilação concluída com sucesso!"
    echo ""
    echo "***** SISTEMA DE VOTAÇÃO DISTRIBUÍDA *****"
    echo "Iniciando nó de votação..."
    echo ""
    
    # Executar o sistema principal
    java -cp $CP_ZK:src -Dlogback.configurationFile=file:$ZK/conf/logback.xml src.votacao.SistemaVotacao
else
    echo "Erro na compilação!"
    exit 1
fi
