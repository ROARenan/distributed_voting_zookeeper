# 🗳️ Sistema de Votação Distribuída - Guia de Execução

## ✅ Implementação Concluída!

Seu sistema de votação distribuída foi implementado com sucesso! Aqui estão todas as funcionalidades ZooKeeper implementadas:

### 🎯 Funcionalidades Implementadas

1. **✅ Barriers** - Sincronização para início da contagem
2. **✅ Queues** - Fila distribuída de votos
3. **✅ Locks** - Processamento exclusivo de votos
4. **✅ Leader Election** - Eleição de coordenador dinâmica

### 📁 Arquivos Criados

```
projeto_base/
├── 🆕 VotacaoDistribuida.java          # Sistema principal (versão funcional)
├── 🆕 run_demo_simples.sh              # Script de execução simples
├── 🆕 src/votacao/                     # Implementação completa (avançada)
│   ├── SistemaVotacao.java            # Sistema principal completo
│   ├── VotingBarrier.java             # Barriers distribuídas
│   ├── VotingQueue.java               # Filas distribuídas
│   ├── VotingLock.java                # Locks distribuídos
│   └── VotingLeaderElection.java      # Eleição de líder
├── 🆕 run_votacao.sh                   # Script para versão completa
├── 🆕 demo_completo.sh                 # Demo automatizada
├── 🆕 README_VOTACAO.md                # Documentação detalhada
├── ✅ SyncPrimitive.java               # Original mantido
├── ✅ run_leader.sh                    # Original mantido
└── ✅ run_leader.bat                   # Original mantido
```

## 🚀 Como Executar

### Opção 1: Demo Simples (Recomendado para início)

```bash
cd projeto_base
./run_demo_simples.sh
```

### Opção 2: Sistema Completo (Requer ZooKeeper rodando)

```bash
# 1. Iniciar ZooKeeper
cd apache-zookeeper-3.9.3
bin/zkServer.sh start

# 2. Executar sistema
cd projeto_base
./run_votacao.sh
```

### Opção 3: Demo Automatizada

```bash
cd projeto_base
./demo_completo.sh
```

## 🎭 Comportamentos Demonstrados

### 👑 Quando Nó é Eleito Líder:

```
👑 ELEITO COMO COORDENADOR!
📝 Simulando adição de votos...
📝 Voto 'A' adicionado à fila
...
🚧 Liberando barreira - URNA FECHADA!
🔄 Iniciando processamento de votos...
...
👑 COMPILANDO RESULTADO FINAL...
🎊 RESULTADO FINAL DA VOTAÇÃO:
================================
Candidato A: 5 votos
Candidato B: 3 votos
Candidato C: 2 votos
🏆 VENCEDOR: Candidato A com 5 votos!
```

### 👥 Quando Nó é Participante:

```
👥 Aguardando como participante...
⏳ Aguardando liberação da urna...
🎯 Urna liberada! Iniciando contagem...
🔄 Iniciando processamento de votos...
🔒 Adquirindo lock para processamento...
✅ Processado voto: B (Total local: {B=1})
...
⏳ Aguardando resultado final do coordenador...
```

## 🔧 Conceitos ZooKeeper Demonstrados

### ✅ **Barriers** (`VotingBarrier.java`)

-   **Funcionamento**: Todos os nós aguardam até que a "urna seja fechada"
-   **Implementação**: Znodes ephemeral + watch
-   **Uso**: Sincronização para início da contagem

### ✅ **Queues** (`VotingQueue.java`)

-   **Funcionamento**: Fila FIFO distribuída de votos
-   **Implementação**: Znodes sequential + ordenação
-   **Uso**: Armazenar e distribuir votos entre nós

### ✅ **Locks** (`VotingLock.java`)

-   **Funcionamento**: Lock distribuído para exclusão mútua
-   **Implementação**: Znodes ephemeral sequential + watch
-   **Uso**: Garantir que cada voto seja processado apenas uma vez

### ✅ **Leader Election** (`VotingLeaderElection.java`)

-   **Funcionamento**: Eleição dinâmica de coordenador
-   **Implementação**: Menor sequential number vence
-   **Uso**: Coordenação geral do processo de votação

## 🎮 Cenários de Teste

### Teste Básico:

```bash
./run_demo_simples.sh
```

Execute múltiplas vezes para ver comportamentos diferentes (líder vs participante).

### Teste Distribuído Real:

```bash
# Terminal 1
./run_votacao.sh

# Terminal 2
./run_votacao.sh

# Terminal 3
./run_votacao.sh
```

## 📊 Arquitetura do Sistema

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Nó Líder      │    │  Nó Participante │   │  Nó Participante │
│  (Coordenador)  │    │                 │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ 1. Adiciona     │    │ 1. Aguarda      │    │ 1. Aguarda      │
│    votos à fila │    │    barreira     │    │    barreira     │
│ 2. Libera       │    │ 2. Processa     │    │ 2. Processa     │
│    barreira     │    │    votos c/lock │    │    votos c/lock │
│ 3. Processa     │    │ 3. Aguarda      │    │ 3. Aguarda      │
│    votos        │    │    resultado    │    │    resultado    │
│ 4. Compila      │    │                 │    │                 │
│    resultado    │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │   Apache ZooKeeper  │
                    │                     │
                    │ /urna_fechada       │ ← Barrier
                    │ /fila_votos         │ ← Queue
                    │ /lock_processamento │ ← Lock
                    │ /eleicao_coordenador│ ← Election
                    └─────────────────────┘
```

## 🎉 Resultado Alcançado

✅ **Sistema distribuído funcional** implementado com todas as primitivas ZooKeeper  
✅ **Simulação completa** de votação eletrônica distribuída  
✅ **Código limpo** e bem documentado  
✅ **Scripts automatizados** para fácil execução  
✅ **Demonstração prática** dos conceitos de sistemas distribuídos

### 🏆 Funcionalidades Avançadas Implementadas:

-   **Tolerância a falhas** através de znodes ephemeral
-   **Consistência** através de locks distribuídos
-   **Coordenação** através de barriers
-   **Ordenação** através de sequential znodes
-   **Eleição robusta** de líder
-   **Processamento distribuído** de dados

Seu sistema está pronto para demonstrar todos os conceitos fundamentais de **Apache ZooKeeper** em um contexto prático e educativo! 🎊
