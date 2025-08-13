package src.votacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.w3c.dom.events.Event;

/**
 * Sistema principal de votação distribuída usando Apache ZooKeeper
 * 
 * Funcionalidades implementadas:
 * - Barriers: Sincronização para início da contagem
 * - Queues: Fila distribuída de votos
 * - Locks: Processamento exclusivo de votos
 * - Leader Election: Eleição de coordenador para resultado final
 * - Agregação global: consolidação via /resultado_votacao/{nodeId}
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

  // Conexão direta para publicar/ler resultados
  private ZooKeeper zk;

  // Contadores locais de votos
  private final Map<String, Integer> contadorLocal = new HashMap<>();

  public SistemaVotacao() {
    this.nodeId = new Random().nextInt(10000);
    System.out.println("Iniciando nó de votação ID: " + nodeId);
  }

  /** Inicializa todos os componentes do sistema distribuído */
  public void inicializar() throws IOException, KeeperException, InterruptedException {
    System.out.println("Conectando ao ZooKeeper...");

    // Conexão própria do SistemaVotacao (para paths auxiliares)
    this.zk = connectZk(ZK_ADDRESS);

    // Garante o path de resultados
    ensurePath(RESULTADO_PATH);

    // Inicializar componentes distribuídos (suas classes atuais)
    barrier = new VotingBarrier(ZK_ADDRESS, BARRIER_PATH, 3); // 3 nós para iniciar
    queue = new VotingQueue(ZK_ADDRESS, QUEUE_PATH);
    lock = new VotingLock(ZK_ADDRESS, LOCK_PATH);
    election = new VotingLeaderElection(ZK_ADDRESS, ELECTION_PATH, LEADER_PATH, nodeId);

    System.out.println("Componentes inicializados com sucesso.");
  }

  /** Simula o processo de votação distribuída */
  public void executarVotacao() throws KeeperException, InterruptedException {
    System.out.println("\nIniciando processo de votação distribuída...");

    // 1) Eleição de líder
    System.out.println("Participando da eleição de coordenador...");
    boolean isLeader = election.elect();

    if (isLeader) {
      System.out.println("Eleito o lider.");
      executarComoLider();
    } else {
      System.out.println("Aguardando como participante");
      executarComoParticipante();
    }
  }

  /** Execução específica para o nó líder */
  private void executarComoLider() throws KeeperException, InterruptedException {
    // (1) Alimenta a fila
    System.out.println("Adicionando votos à queue...");
    String[] votos = { "A", "B", "A", "C", "B", "A", "C", "A", "B", "A" };
    for (String voto : votos) {
      queue.produce(voto);
      Thread.sleep(100);
    }
    System.out.println("Todos os votos foram adicionados à fila. Prontos para serem processados...");

    // (2) Libera a barreira para contagem
    System.out.println("Liberando barreira - Urnas fechadas prontas para contagem.");
    barrier.enter();

    // (3) Processa como qualquer nó
    processarVotos();

    // (4) Publica a contagem local do líder
    publicarContagemLocal();

    // (5) Aguarda os demais publicarem
    Thread.sleep(3000);

    // (6) Agrega tudo e anuncia
    compilarResultadoFinal();
    System.out.println("Final.");
  }

  /** Execução específica para nós participantes */
  private void executarComoParticipante() throws KeeperException, InterruptedException {
    System.out.println("Aguardando liberação da urna...");
    barrier.enter();
    System.out.println("Urna liberada! Iniciando contagem...");

    processarVotos();

    // Publica contagem local para o líder agregar
    publicarContagemLocal();

    // Simples espera pelo anúncio (poderia ser um watch em um znode de "final")
    aguardarResultadoFinal();
  }

  /** Processa votos da fila com lock distribuído */
  private void processarVotos() throws KeeperException, InterruptedException {
    System.out.println("Iniciando processamento de votos...");

    while (true) {
      if (lock.acquire()) {
        try {
          String voto = queue.consume();
          if (voto == null) {
            System.out.println("Queue vazia - processamento concluído.");
            break;
          }
          contadorLocal.put(voto, contadorLocal.getOrDefault(voto, 0) + 1);
          System.out.println("Processado voto: " + voto + " (Total urna local: " + contadorLocal + ")");
          Thread.sleep(200);
        } finally {
          lock.release();
        }
      } else {
        Thread.sleep(80);
      }
    }

    System.out.println("Nó" + nodeId + " finalizou processamento!");
    System.out.println("Contagem local: " + contadorLocal);
  }

  /** Publica a contagem local em /resultado_votacao/{nodeId} */
  private void publicarContagemLocal() throws KeeperException, InterruptedException {
    String nodePath = RESULTADO_PATH + "/" + nodeId;
    byte[] data = serializeContagem(contadorLocal);

    Stat s = zk.exists(nodePath, false);
    if (s == null) {
      zk.create(nodePath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      System.out.println("Publicado resultado local em " + nodePath);
    } else {
      zk.setData(nodePath, data, s.getVersion());
      System.out.println("Atualizado resultado local em " + nodePath);
    }
  }

  /** Lê todos os resultados em /resultado_votacao e agrega */
  private void compilarResultadoFinal() throws KeeperException, InterruptedException {
    System.out.println("\nCOMPILANDO RESULTADO FINAL (agregado entre nós) para encontrar o vencedor...");
    Map<String, Integer> agregado = new HashMap<>();

    List<String> filhos = zk.getChildren(RESULTADO_PATH, false);
    for (String filho : filhos) {
      String path = RESULTADO_PATH + "/" + filho;
      byte[] data = zk.getData(path, false, null);
      Map<String, Integer> parcial = deserializeContagem(data);
      somar(agregado, parcial);
    }

    imprimirResultado("RESULTADO FINAL DA VOTAÇÃO (GLOBAL)", agregado);
  }

  /** Aguarda "anúncio" do líder (simplificado com sleep) */
  private void aguardarResultadoFinal() throws InterruptedException {
    System.out.println("Aguardando resultado final do coordenador...");
    Thread.sleep(5000);
    System.out.println("Resultado recebido!");
  }

  /* ====================== Utilitários ====================== */

  private static void somar(Map<String, Integer> base, Map<String, Integer> inc) {
    for (Map.Entry<String, Integer> e : inc.entrySet()) {
      base.put(e.getKey(), base.getOrDefault(e.getKey(), 0) + e.getValue());
    }
  }

  private static void imprimirResultado(String titulo, Map<String, Integer> contagem) {
    System.out.println(titulo);
    System.out.println("================================");
    int total = 0;
    String vencedor = null;
    int max = 0;

    for (Map.Entry<String, Integer> e : contagem.entrySet()) {
      System.out.println("Candidato " + e.getKey() + ": " + e.getValue() + " votos");
      total += e.getValue();
      if (e.getValue() > max) {
        max = e.getValue();
        vencedor = e.getKey();
      }
    }
    System.out.println("--------------------------------");
    System.out.println("Total de votos processados: " + total);
    if (vencedor != null) {
      System.out.println("VENCEDOR: Candidato " + vencedor + " com " + max + " votos.");
    }
    System.out.println("================================");
    System.out.println("Votação finalizada com sucesso. =)");
  }

  /** Serializa como "A=3;B=1;..." (sem dependências externas) */
  private static byte[] serializeContagem(Map<String, Integer> m) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      if (sb.length() > 0)
        sb.append(';');
      sb.append(e.getKey()).append('=').append(e.getValue());
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  /** Deserializa "A=3;B=1;..." */
  private static Map<String, Integer> deserializeContagem(byte[] data) {
    Map<String, Integer> m = new HashMap<>();
    if (data == null || data.length == 0)
      return m;
    String s = new String(data, StandardCharsets.UTF_8);
    for (String part : s.split(";")) {
      if (part.isEmpty())
        continue;
      String[] kv = part.split("=", 2);
      if (kv.length == 2) {
        try {
          m.put(kv[0], Integer.parseInt(kv[1]));
        } catch (NumberFormatException ignore) {
        }
      }
    }
    return m;
  }

  /** Conecta e espera estado SYNC_CONNECTED */
  private static ZooKeeper connectZk(String address) throws IOException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    ZooKeeper zk = new ZooKeeper(address, 3000, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        if (event.getState() == Event.KeeperState.SyncConnected) {
          latch.countDown();
        }
      }
    });
    latch.await();
    return zk;
  }

  /** Garante que um znode persistente exista */
  private void ensurePath(String path) throws KeeperException, InterruptedException {
    try {
      Stat s = zk.exists(path, false);
      if (s == null) {
        zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (KeeperException.NodeExistsException e) {
      // O nó já existe, está tudo bem
      System.out.println("Path " + path + " já existe, continuando...");
    }
  }

  /** Método principal */
  public static void main(String[] args) {
    SistemaVotacao sistema = new SistemaVotacao();
    try {
      sistema.inicializar();
      sistema.executarVotacao();
    } catch (Exception e) {
      System.err.println("Erro ne execução: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
