## Sistema de Votação Eletrônica Distribuída

    Descrição: Um sistema de votação eletrônica em uma eleição online, onde múltiplos nós processam votos e garantem a integridade do processo de contagem e apuração.

    Funcionalidades:

        Barriers: Garante que todas as máquinas de votação comecem o processo de contagem de votos apenas quando todas as urnas estiverem fechadas e prontas para começar.

        Queues: Filas para processar os votos que estão sendo recebidos em tempo real. Cada nó pode processar os votos à medida que chegam.

        Locks: Evitar que dois nós processem o mesmo voto ao mesmo tempo, garantindo integridade.

        Leader Election: Um nó pode ser eleito para ser o responsável pela apuração final dos votos, ou para decidir quando o sistema deve ser encerrado após a contagem.

    Exemplo de uso: Uma eleição online em que os votos são computados por múltiplos servidores, e é necessário garantir que a contagem seja feita de forma consistente, segura e sem conflitos.
