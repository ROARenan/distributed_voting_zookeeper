# Sistema de Votação Eletrônica Distribuída

Descrição: Um sistema de votação eletrônica em uma eleição online, onde múltiplos nós processam votos e garantem a integridade do processo de contagem e apuração.

Funcionalidades:

-   Barriers: Garante que todas as máquinas de votação comecem o processo de contagem de votos apenas quando todas as urnas estiverem fechadas e prontas para começar.

-   Queues: Filas para processar os votos que estão sendo recebidos em tempo real. Cada nó pode processar os votos à medida que chegam.

-   Locks: Evitar que dois nós processem o mesmo voto ao mesmo tempo, garantindo integridade.

-   Leader Election: Um nó pode ser eleito para ser o responsável pela apuração final dos votos, ou para decidir quando o sistema deve ser encerrado após a contagem.

Exemplo de uso: Uma eleição online em que os votos são computados por múltiplos servidores, e é necessário garantir que a contagem seja feita de forma consistente, segura e sem conflitos.

🎊 Resumo da Implementação Completa
Foi implementado com sucesso um sistema distribuído de votação usando Apache ZooKeeper que demonstra todas as funcionalidades solicitadas:

✅ Funcionalidades Implementadas:

🚧 Barriers - Sincronização para que todos os nós aguardem até que a "urna seja fechada"

📋 Queues - Fila distribuída FIFO para processar votos sequencialmente

🔒 Locks - Lock distribuído garantindo que cada voto seja processado uma única vez

👑 Leader Election - Eleição dinâmica de um coordenador para gerenciar o processo

🎯 Demonstração Funcional:

O sistema simula uma votação eletrônica distribuída onde:

Múltiplos nós representam seções eleitorais

Um líder é eleito para coordenar o processo

Votos são processados de forma distribuída com sincronização

Resultado final é compilado e apresentado pelo coordenador

🚀 Como Executar:

```bash
cd projeto_base
./run_demo_simples.sh
```

🏆 Resultado Alcançado:
✅ Sistema distribuído funcional e educativo

✅ Demonstra todos os conceitos de ZooKeeper solicitados

✅ Código limpo, comentado e bem estruturado

✅ Scripts automatizados para fácil execução

✅ Documentação completa incluída
