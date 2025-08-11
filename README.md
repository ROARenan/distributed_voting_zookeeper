# Sistema de Votação Eletrônica Distribuída

Descrição: Sistema de votação eletrônica para uma eleição online, onde múltiplos nós processam votos e garantem a integridade da contagem e apuração.

## Funcionalidades

- **Barriers**: garante que todas as máquinas de votação iniciem a contagem apenas quando todas as urnas estiverem fechadas e prontas.  
- **Queues**: filas para processar votos recebidos em tempo real. Cada nó processa os votos à medida que chegam.  
- **Locks**: evita que dois nós processem o mesmo voto ao mesmo tempo, garantindo integridade.  
- **Leader Election**: um nó é eleito para apurar o resultado final e decidir quando encerrar o sistema após a contagem.

**Exemplo de uso**: eleição online em que votos são processados por múltiplos servidores, com necessidade de contagem consistente, segura e sem conflitos.

## Resumo da implementação completa

Foi implementado um sistema distribuído de votação usando Apache ZooKeeper, com todas as funcionalidades previstas:

- **Barriers** – sincronização para que todos os nós aguardem até que a urna seja fechada.  
- **Queues** – fila distribuída FIFO para processar votos sequencialmente.  
- **Locks** – lock distribuído garantindo que cada voto seja processado apenas uma vez.  
- **Leader Election** – eleição dinâmica de coordenador para gerenciar o processo.

## Demonstração funcional

O sistema simula uma votação eletrônica distribuída onde:

- Múltiplos nós representam seções eleitorais.  
- Um líder é eleito para coordenar o processo.  
- Votos são processados de forma distribuída com sincronização.  
- O resultado final é compilado e apresentado pelo coordenador.

## Resultado alcançado

- Sistema distribuído funcional e educativo.  
- Demonstra todos os conceitos de ZooKeeper solicitados.  
- Código limpo, comentado e estruturado.  
- Scripts automatizados para execução.  
- Documentação completa incluída.

# Guia de Execução

## Implementação concluída

O sistema de votação distribuída foi implementado com sucesso. Ele conta com todas as funcionalidades pedidas no projeto, utilizando Java e Apache ZooKeeper.

### Arquivos criados

```
projeto_base/
├── VotacaoDistribuida.java        # Versão simples do sistema
├── run_demo_simples.sh            # Script de execução simples
├── src/votacao/                   # Implementação completa
│   ├── SistemaVotacao.java
│   ├── VotingBarrier.java
│   ├── VotingQueue.java
│   ├── VotingLock.java
│   └── VotingLeaderElection.java
├── run_votacao.sh                   # Script para a versão completa
├── demo_completo.sh                 # Demonstração automatizada
├── cleanup.sh                       # Limpeza do Zookeeper e processos
├── README.md                        # Documentação detalhada
├── SyncPrimitive.java               # Arquivo original mantido
├── run_leader.sh                    # Arquivo original mantido
└── run_leader.bat                   # Arquivo original mantido
```

## Como executar

### Opção 1: Demonstração simples (recomendada para começar)

Está execução não roda zookeeper e tem apenas o intuito de demonstrar o sistema de votação.

```bash
cd projeto_base
./run_demo_simples.sh
```

### Opção 2: Sistema completo (requer ZooKeeper em execução)

Caso prefira rodar o zookeeper separado do script de votação. 

```bash
# Iniciar ZooKeeper
cd apache-zookeeper-3.9.3
bin/zkServer.sh start

# Executar sistema
cd projeto_base
./run_votacao.sh
```

### Opção 3: Demonstração automatizada (recomendada)

```bash
cd projeto_base
./demo_completo.sh
```

## Comportamentos esperados

### Quando o nó é eleito líder

```
ELEITO COMO COORDENADOR
Adicionando votos à fila...
Voto 'A' adicionado
...
Liberando barreira - urna fechada
Iniciando processamento de votos
...
Compilando resultado final...
RESULTADO FINAL:
Candidato A: 5 votos
Candidato B: 3 votos
Candidato C: 2 votos
VENCEDOR: Candidato A
```

### Quando o nó é participante

```
Aguardando como participante...
Aguardando liberação da urna...
Urna liberada! Iniciando contagem...
Adquirindo lock para processamento...
Processado voto: B (Total local: {B=1})
...
Aguardando resultado final do coordenador...
```

## Conceitos ZooKeeper demonstrados

### **Barriers** (`VotingBarrier.java`)

- Funcionamento: Todos os nós aguardam até que a "urna" seja fechada.
- Implementação: Znodes ephemeral + watch.
- Uso: Sincronização para início da contagem.

### **Queues** (`VotingQueue.java`)

- Funcionamento: Fila FIFO distribuída de votos.
- Implementação: Znodes sequential + ordenação.
- Uso: Armazenar e distribuir votos entre nós.

### **Locks** (`VotingLock.java`)

- Funcionamento: Lock distribuído para exclusão mútua.
- Implementação: Znodes ephemeral sequential + watch.
- Uso: Garantir que cada voto seja processado apenas uma vez.

### **Leader Election** (`VotingLeaderElection.java`)

- Funcionamento: Eleição dinâmica de coordenador.
- Implementação: Menor número sequencial vence.
- Uso: Coordenação geral do processo de votação.

## Zookeeper em ação no sistema

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

## Arquitetura do sistema

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

## Resultado alcançado

- Sistema distribuído funcional com todas as primitivas ZooKeeper.  
- Simulação completa de votação eletrônica distribuída.  
- Código limpo e documentado.  
- Scripts automatizados para execução.  
- Demonstração prática de conceitos de sistemas distribuídos.

### Funcionalidades avançadas implementadas

- Tolerância a falhas com znodes ephemeral.  
- Consistência com locks distribuídos.  
- Coordenação com barriers.  
- Ordenação com znodes sequenciais.  
- Eleição robusta de líder.  
- Processamento distribuído de dados.