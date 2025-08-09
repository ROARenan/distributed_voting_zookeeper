# Sistema de Votação Distribuída com Apache ZooKeeper

Este projeto implementa um sistema distribuído simples para demonstrar as funcionalidades principais do Apache ZooKeeper em um contexto de votação eletrônica.

## 🎯 Funcionalidades Implementadas

### 1. **Barriers** (`VotingBarrier.java`)

-   Sincroniza o início da contagem de votos
-   Todos os nós aguardam até que a "urna seja fechada"
-   Garante que o processamento só inicie quando todos estiverem prontos

### 2. **Queues** (`VotingQueue.java`)

-   Fila distribuída para armazenar votos
-   Implementa padrão Produtor/Consumidor
-   Garante ordem FIFO no processamento
-   Votos são representados como strings simples ("A", "B", "C")

### 3. **Locks** (`VotingLock.java`)

-   Lock distribuído para processamento exclusivo
-   Garante que cada voto seja processado apenas uma vez
-   Evita condições de corrida entre múltiplos nós

### 4. **Leader Election** (`VotingLeaderElection.java`)

-   Elege um coordenador dinamicamente
-   O líder é responsável por:
    -   Adicionar votos à fila
    -   Liberar a barreira para início da contagem
    -   Compilar e apresentar o resultado final

## 📁 Estrutura do Projeto

```
projeto_base/
├── src/votacao/                    # Novo sistema de votação
│   ├── SistemaVotacao.java        # Classe principal
│   ├── VotingBarrier.java         # Implementação de barriers
│   ├── VotingQueue.java           # Fila distribuída
│   ├── VotingLock.java            # Lock distribuído
│   └── VotingLeaderElection.java  # Eleição de líder
├── run_votacao.sh                 # Script para executar votação
├── SyncPrimitive.java             # Implementação original (mantida)
├── run_leader.sh                  # Script original (mantido)
└── run_leader.bat                 # Script original (mantido)
```

## 🚀 Como Executar

### Pré-requisitos

1. **ZooKeeper rodando**: Certifique-se de que o ZooKeeper está executando na porta 2181
2. **Java 8+**: JDK instalado e configurado

### Iniciando o ZooKeeper

```bash
# Navegar para o diretório do ZooKeeper
cd apache-zookeeper-3.9.3

# Iniciar o servidor
bin/zkServer.sh start
```

### Executando o Sistema de Votação

**Opção 1: Script automatizado**

```bash
cd projeto_base
./run_votacao.sh
```

**Opção 2: Compilação manual**

```bash
cd projeto_base

# Compilar
javac -cp .:../apache-zookeeper-3.9.3/lib/* src/votacao/*.java

# Executar
java -cp .:../apache-zookeeper-3.9.3/lib/*:src votacao.SistemaVotacao
```

### Executando Múltiplos Nós

Para simular um ambiente distribuído real, execute o comando em múltiplos terminais:

```bash
# Terminal 1
./run_votacao.sh

# Terminal 2 (nova janela)
./run_votacao.sh

# Terminal 3 (nova janela)
./run_votacao.sh
```

## 🧪 Comportamento Esperado

### Fluxo de Execução

1. **Conexão**: Cada nó conecta ao ZooKeeper
2. **Eleição**: Um nó é eleito como coordenador (líder)
3. **Preparação**: O líder adiciona votos simulados à fila
4. **Barreira**: Todos os nós aguardam liberação da "urna"
5. **Processamento**: Nós processam votos com lock distribuído
6. **Resultado**: Líder compila e exibe o resultado final

### Exemplo de Saída

```
🗳️  Iniciando nó de votação ID: 7532
📡 Conectando ao ZooKeeper...
✅ Componentes inicializados com sucesso!

🚀 Iniciando processo de votação distribuída...
🎯 Participando da eleição de coordenador...
👑 ELEITO COMO COORDENADOR!

📝 Adicionando votos à fila distribuída...
📝 Queue: Voto 'A' adicionado em /fila_votos/voto-0000000001
📝 Queue: Voto 'B' adicionado em /fila_votos/voto-0000000002
...

🚧 Liberando barreira - URNA FECHADA!
🔄 Iniciando processamento de votos...
🔒 Lock: Adquirido com sucesso!
✅ Processado voto: A (Total local: {A=1})
🔓 Lock: Liberado com sucesso!
...

👑 COMPILANDO RESULTADO FINAL...
🎊 RESULTADO FINAL DA VOTAÇÃO:
================================
Candidato A: 4 votos
Candidato B: 3 votos
Candidato C: 3 votos
--------------------------------
Total de votos processados: 10
🏆 VENCEDOR: Candidato A com 4 votos!
================================
✅ Votação finalizada com sucesso!
```

## 🔧 Configurações

### Parâmetros Configuráveis (em `SistemaVotacao.java`)

-   **ZK_ADDRESS**: Endereço do ZooKeeper (padrão: "localhost:2181")
-   **BARRIER_PATH**: Caminho do barrier (padrão: "/urna_fechada")
-   **QUEUE_PATH**: Caminho da fila (padrão: "/fila_votos")
-   **LOCK_PATH**: Caminho do lock (padrão: "/lock_processamento")
-   **Número de nós**: Modificar no construtor do VotingBarrier (padrão: 3)

### Votos Simulados

Os votos estão hardcoded no método `executarComoLider()`:

```java
String[] votos = {"A", "B", "A", "C", "B", "A", "C", "A", "B", "A"};
```

## 🛠️ Funcionalidades Técnicas Detalhadas

### Barriers

-   **Caminho**: `/urna_fechada`
-   **Funcionamento**: Nós criam znodes ephemeral sequenciais e aguardam até atingir o número mínimo
-   **Trigger**: Liberado quando todos os nós estão prontos

### Queues

-   **Caminho**: `/fila_votos`
-   **Padrão**: FIFO com znodes sequenciais persistentes
-   **Consumo**: Nós competem para processar o próximo voto

### Locks

-   **Caminho**: `/lock_processamento`
-   **Algoritmo**: Baseado em znodes ephemeral sequenciais
-   **Garantia**: Apenas um nó processa voto por vez

### Leader Election

-   **Caminho**: `/eleicao_coordenador`
-   **Estratégia**: Menor número sequencial vence
-   **Responsabilidades**: Coordenação geral do processo

## 🎮 Cenários de Teste

### Teste 1: Nó Único

```bash
./run_votacao.sh
```

Esperar alguns segundos para timeout do barrier.

### Teste 2: Múltiplos Nós

Executar em 3+ terminais simultaneamente para ver a coordenação completa.

### Teste 3: Falha de Nó

Interromper um nó (Ctrl+C) durante execução para testar recuperação.

## 📝 Limitações e Simplificações

1. **Votos hardcoded**: Para simplificação, votos são predefinidos
2. **Resultado local**: Cada nó conta apenas os votos que processou
3. **Sem persistência**: Resultados não são salvos em banco
4. **Interface textual**: Apenas saída no console
5. **Sem autenticação**: Não há validação de votos

## 🔍 Monitoramento com ZooKeeper CLI

```bash
# Conectar ao ZooKeeper CLI
bin/zkCli.sh

# Verificar estrutura criada
ls /
ls /urna_fechada
ls /fila_votos
ls /eleicao_coordenador

# Ver dados do líder
get /coordenador_votacao
```

## 📚 Conceitos ZooKeeper Demonstrados

-   **Znodes Ephemeral**: Para eleição e locks
-   **Znodes Sequenciais**: Para ordering e FIFO
-   **Znodes Persistentes**: Para estrutura da fila
-   **Watches**: Para notificações de mudanças
-   **ACLs**: Usando OPEN_ACL_UNSAFE para simplicidade
-   **Sessions**: Gestão automática de conexões

---

Este sistema demonstra como construir aplicações distribuídas robustas usando Apache ZooKeeper, implementando padrões fundamentais de coordenação em sistemas distribuídos.
