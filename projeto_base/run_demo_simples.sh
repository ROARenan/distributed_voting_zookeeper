#!/bin/bash

# Script simples para demonstrar o sistema de votação
export ZK=/home/ferdinando-longoni/tmp-ufabc/distributed_voting_zookeeper/apache-zookeeper-3.9.3
echo "ZK=$ZK"

export CP_ZK=.:$ZK'/lib/zookeeper-3.9.3.jar':$ZK'/lib/zookeeper-jute-3.9.3.jar':$ZK'/lib/slf4j-api-1.7.30.jar':$ZK'/lib/logback-core-1.2.13.jar':$ZK'/lib/logback-classic-1.2.13.jar':$ZK'/lib/netty-handler-4.1.113.Final.jar'

echo " Compilando sistema simplificado..."
javac -cp $CP_ZK VotacaoDistribuida.java

if [ $? -eq 0 ]; then
    echo " Compilação concluída!"
    echo ""
    echo "  ***** DEMO: SISTEMA DE VOTAÇÃO DISTRIBUÍDA *****"
    echo ""
    
    # Executar demonstração
    java -cp $CP_ZK VotacaoDistribuida
else
    echo " Erro na compilação!"
    exit 1
fi
