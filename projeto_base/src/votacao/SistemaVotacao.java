package src.votacao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;

/**
 * Sistema principal de votação distribuída usando Apache ZooKeeper
 * 
 * Funcionalidades implementadas:
 * - Barriers: Sincronização para início da contagem
 * - Queues: Fila distribuída de votos
 * - Locks: Processamento exclusivo de votos
 * - Leader Election: Eleição de coordenador para resultado final
 */
public class SistemaVotacao {

  private static final String ZK_ADDRESS = "localhost:2181";
  private static final String BARRIER_PATH = "/urna_fechada";
  private static final String QUEUE_PATH = "/fila_votos";
  private static final String LOCK_PATH = "/lock_processamento";
  private static final String ELECTION_PATH = "/eleicao_coordenador";
  private static final String LEADER_PATH = "/coordenador_votacao";
  private static final String RESULTADO_PATH = "/resultado_votacao";

  private int nodeId;
  private VotingBarrier barrier;
  private VotingQueue queue;
  private VotingLock lock;
  private VotingLeaderElection election;

  // Contadores locais de votos
  private Map<String, Integer> contadorLocal = new HashMap<>();

  public SistemaVotacao() {
    this.nodeId = new Random().nextInt(10000);
    System.out.println("🗳️  Iniciando nó de votação ID: " + nodeId);
  }

  /**
   * Inicializa todos os componentes do sistema distribuído
   */
  public void inicializar() throws IOException, KeeperException, InterruptedException {
    System.out.println("📡 Conectando ao ZooKeeper...");

    // Inicializar componentes distribuídos
    barrier = new VotingBarrier(ZK_ADDRESS, BARRIER_PATH, 3); // 3 nós para iniciar
    queue = new VotingQueue(ZK_ADDRESS, QUEUE_PATH);
    lock = new VotingLock(ZK_ADDRESS, LOCK_PATH);
    election = new VotingLeaderElection(ZK_ADDRESS, ELECTION_PATH, LEADER_PATH, nodeId);

    System.out.println("✅ Componentes inicializados com sucesso!");
  }

  /**
   * Simula o processo de votação distribuída
   */
  public void executarVotacao() throws KeeperException, InterruptedException {
    System.out.println("\n🚀 Iniciando processo de votação distribuída...");

    // 1. Participar da eleição de líder
    System.out.println("🎯 Participando da eleição de coordenador...");
    boolean isLeader = election.elect();

    if (isLeader) {
      System.out.println("👑 ELEITO COMO COORDENADOR!");
      executarComoLider();
    } else {
      System.out.println("👥 Aguardando como participante...");
      executarComoParticipante();
    }
  }

  /**
   * Execução específica para o nó líder
   */
  private void executarComoLider() throws KeeperException, InterruptedException {
    // Simular inserção de votos na fila
    System.out.println("📝 Adicionando votos à fila distribuída...");
    String[] votos = { "A", "B", "A", "C", "B", "A", "C", "A", "B", "A" };

    for (String voto : votos) {
      queue.produce(voto);
      Thread.sleep(100); // Simular tempo entre votos
    }

    System.out.println("✅ Todos os votos foram adicionados à fila!");

    // Liberar a barreira para iniciar contagem
    System.out.println("🚧 Liberando barreira - URNA FECHADA!");
    barrier.enter();

    // Processar votos como os outros nós
    processarVotos();

    // Aguardar um pouco para outros nós processarem
    Thread.sleep(3000);

    // Compilar resultado final
    compilarResultadoFinal();
  }

  /**
   * Execução específica para nós participantes
   */
  private void executarComoParticipante() throws KeeperException, InterruptedException {
    // Aguardar liberação da barreira
    System.out.println("⏳ Aguardando liberação da urna...");
    barrier.enter();

    System.out.println("🎯 Urna liberada! Iniciando contagem...");

    // Processar votos
    processarVotos();

    // Aguardar resultado final do líder
    aguardarResultadoFinal();
  }

  /**
   * Processa votos da fila com lock distribuído
   */
  private void processarVotos() throws KeeperException, InterruptedException {
    System.out.println("🔄 Iniciando processamento de votos...");

    while (true) {
      // Tentar adquirir lock
      if (lock.acquire()) {
        try {
          // Consumir voto da fila
          String voto = queue.consume();
          if (voto == null) {
            System.out.println("📭 Fila vazia - processamento concluído!");
            break;
          }

          // Contar voto localmente
          contadorLocal.put(voto, contadorLocal.getOrDefault(voto, 0) + 1);
          System.out.println("✅ Processado voto: " + voto +
              " (Total local: " + contadorLocal + ")");

          Thread.sleep(500); // Simular tempo de processamento

        } finally {
          lock.release();
        }
      } else {
        Thread.sleep(100); // Aguardar antes de tentar novamente
      }
    }

    System.out.println("🏁 Nó " + nodeId + " finalizou processamento!");
    System.out.println("📊 Contagem local: " + contadorLocal);
  }

  /**
   * Compila e exibe o resultado final (apenas para o líder)
   */
  private void compilarResultadoFinal() throws KeeperException, InterruptedException {
    System.out.println("\n👑 COMPILANDO RESULTADO FINAL...");

    // Em um sistema real, coletaríamos dados de todos os nós
    // Para simplificação, mostraremos apenas a contagem local
    System.out.println("🎊 RESULTADO FINAL DA VOTAÇÃO:");
    System.out.println("================================");

    int totalVotos = 0;
    for (Map.Entry<String, Integer> entry : contadorLocal.entrySet()) {
      System.out.println("Candidato " + entry.getKey() + ": " + entry.getValue() + " votos");
      totalVotos += entry.getValue();
    }

    System.out.println("--------------------------------");
    System.out.println("Total de votos processados: " + totalVotos);

    // Determinar vencedor
    String vencedor = null;
    int maxVotos = 0;
    for (Map.Entry<String, Integer> entry : contadorLocal.entrySet()) {
      if (entry.getValue() > maxVotos) {
        maxVotos = entry.getValue();
        vencedor = entry.getKey();
      }
    }

    if (vencedor != null) {
      System.out.println("🏆 VENCEDOR: Candidato " + vencedor + " com " + maxVotos + " votos!");
    }

    System.out.println("================================");
    System.out.println("✅ Votação finalizada com sucesso!");

    // Notificar resultado para outros nós (simplificado)
    queue.notifyResult("VOTACAO_FINALIZADA");
  }

  /**
   * Aguarda o resultado final do líder
   */
  private void aguardarResultadoFinal() throws KeeperException, InterruptedException {
    System.out.println("⏳ Aguardando resultado final do coordenador...");

    // Em implementação real, escutaria mudanças no ZooKeeper
    // Para simplificação, aguardamos um tempo fixo
    Thread.sleep(5000);

    System.out.println("📋 Resultado recebido! Verificar saída do coordenador.");
  }

  /**
   * Método principal para executar o sistema
   */
  public static void main(String[] args) {
    SistemaVotacao sistema = new SistemaVotacao();

    try {
      sistema.inicializar();
      sistema.executarVotacao();

    } catch (Exception e) {
      System.err.println("❌ Erro durante execução: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
