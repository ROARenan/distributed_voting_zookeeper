package src.votacao;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

/**
 * Implementação de Queue distribuída usando ZooKeeper
 * Gerencia uma fila de votos sincronizada entre os nós
 */
public class VotingQueue implements Watcher {

  private ZooKeeper zk;
  private String root;
  private Object mutex = new Object();

  /**
   * Construtor da Queue
   * 
   * @param address Endereço do ZooKeeper
   * @param root    Caminho raiz da fila
   */
  public VotingQueue(String address, String root) throws IOException {
    this.root = root;
    this.zk = new ZooKeeper(address, 3000, this);

    try {
      // Criar o nó raiz se não existir
      Stat s = zk.exists(root, false);
      if (s == null) {
        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (KeeperException e) {
      System.err.println("Erro ao criar nó raiz da fila: " + e.getMessage());
    } catch (InterruptedException e) {
      System.err.println("Interrompido ao criar fila: " + e.getMessage());
    }
  }

  /**
   * Adiciona um voto à fila (Producer)
   * 
   * @param voto O voto a ser adicionado
   */
  public boolean produce(String voto) throws KeeperException, InterruptedException {
    try {
      String path = zk.create(root + "/voto-", voto.getBytes(),
          Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
      System.out.println("📝 Queue: Voto '" + voto + "' adicionado em " + path);
      return true;
    } catch (KeeperException e) {
      System.err.println("Erro ao adicionar voto à fila: " + e.getMessage());
      return false;
    }
  }

  /**
   * Consome um voto da fila (Consumer)
   * 
   * @return O voto consumido ou null se a fila estiver vazia
   */
  public String consume() throws KeeperException, InterruptedException {
    while (true) {
      synchronized (mutex) {
        List<String> list = zk.getChildren(root, true);

        if (list.isEmpty()) {
          return null; // Fila vazia
        }

        // Encontrar o elemento com menor sequência (FIFO)
        String minNode = null;
        for (String node : list) {
          if (node.startsWith("voto-")) {
            if (minNode == null || node.compareTo(minNode) < 0) {
              minNode = node;
            }
          }
        }

        if (minNode == null) {
          return null; // Nenhum voto encontrado
        }

        String fullPath = root + "/" + minNode;
        try {
          // Ler dados do voto
          byte[] data = zk.getData(fullPath, false, null);
          String voto = new String(data);

          // Remover o voto da fila
          zk.delete(fullPath, -1);

          System.out.println("📤 Queue: Voto '" + voto + "' consumido de " + fullPath);
          return voto;

        } catch (KeeperException.NoNodeException e) {
          // Outro nó já consumiu este voto, tentar novamente
          continue;
        }
      }
    }
  }

  /**
   * Verifica quantos votos estão na fila
   * 
   * @return Número de votos pendentes
   */
  public int size() throws KeeperException, InterruptedException {
    List<String> list = zk.getChildren(root, false);
    int count = 0;
    for (String node : list) {
      if (node.startsWith("voto-")) {
        count++;
      }
    }
    return count;
  }

  /**
   * Método para notificar resultado final (usado pelo líder)
   */
  public void notifyResult(String message) throws KeeperException, InterruptedException {
    zk.create(root + "/resultado-", message.getBytes(),
        Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
    System.out.println("📢 Queue: Resultado notificado - " + message);
  }

  @Override
  public void process(WatchedEvent event) {
    synchronized (mutex) {
      mutex.notifyAll();
    }
  }
}
