package src.votacao;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.Watcher.Event;

/**
 * Implementação de Leader Election usando ZooKeeper
 * Elege um coordenador para gerenciar o processo de votação
 */
public class VotingLeaderElection implements Watcher {

  private ZooKeeper zk;
  private String root;
  private String leaderPath;
  private String currentPath;
  private int nodeId;
  private Object mutex = new Object();
  private boolean isLeader = false;

  /**
   * Construtor da Leader Election
   * 
   * @param address    Endereço do ZooKeeper
   * @param root       Caminho raiz da eleição
   * @param leaderPath Caminho do nó líder
   * @param nodeId     ID único do nó
   */
  public VotingLeaderElection(String address, String root, String leaderPath, int nodeId) throws IOException {
    this.root = root;
    this.leaderPath = leaderPath;
    this.nodeId = nodeId;
    this.zk = new ZooKeeper(address, 3000, this);

    try {
      // Criar o nó raiz se não existir
      Stat s = zk.exists(root, false);
      if (s == null) {
        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (KeeperException e) {
      System.err.println("Erro ao criar nó raiz da eleição: " + e.getMessage());
    } catch (InterruptedException e) {
      System.err.println("Interrompido ao criar eleição: " + e.getMessage());
    }
  }

  /**
   * Participa da eleição de líder
   * 
   * @return true se foi eleito líder
   */
  public boolean elect() throws KeeperException, InterruptedException {
    // Criar nó ephemeral sequencial para participar da eleição
    String prefix = root + "/candidate-";
    currentPath = zk.create(prefix, Integer.toString(nodeId).getBytes(),
        Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

    System.out.println("🎯 Election: Nó " + nodeId + " participando em " + currentPath);

    return checkLeadership();
  }

  /**
   * Verifica se este nó é o líder
   */
  private boolean checkLeadership() throws KeeperException, InterruptedException {
    List<String> candidates = zk.getChildren(root, false);
    Collections.sort(candidates);

    // Filtrar apenas candidatos válidos
    String myNode = currentPath.substring(root.length() + 1);

    for (int i = 0; i < candidates.size(); i++) {
      if (candidates.get(i).equals(myNode)) {
        if (i == 0) {
          // Este é o primeiro candidato - é o líder
          becomeLeader();
          return true;
        } else {
          // Aguardar o candidato anterior
          String previousCandidate = candidates.get(i - 1);
          watchPreviousCandidate(previousCandidate);
          return false;
        }
      }
    }

    return false;
  }

  /**
   * Torna-se o líder
   */
  private void becomeLeader() throws KeeperException, InterruptedException {
    isLeader = true;

    // Criar/atualizar nó do líder
    try {
      Stat s = zk.exists(leaderPath, false);
      if (s == null) {
        zk.create(leaderPath, Integer.toString(nodeId).getBytes(),
            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
      } else {
        zk.setData(leaderPath, Integer.toString(nodeId).getBytes(), -1);
      }

      System.out.println("👑 Election: Nó " + nodeId + " ELEITO COMO LÍDER!");

    } catch (KeeperException e) {
      System.err.println("Erro ao criar nó de líder: " + e.getMessage());
    }
  }

  /**
   * Observa o candidato anterior
   */
  private void watchPreviousCandidate(String previousCandidate) throws KeeperException, InterruptedException {
    String previousPath = root + "/" + previousCandidate;

    synchronized (mutex) {
      Stat s = zk.exists(previousPath, this);
      if (s == null) {
        // Candidato anterior já saiu, verificar liderança novamente
        checkLeadership();
      } else {
        System.out.println("👀 Election: Observando candidato anterior " + previousCandidate);
      }
    }
  }

  /**
   * Verifica se este nó é o líder atual
   */
  public boolean isLeader() {
    return isLeader;
  }

  /**
   * Obtém o ID do líder atual
   */
  public int getCurrentLeader() throws KeeperException, InterruptedException {
    try {
      byte[] data = zk.getData(leaderPath, false, null);
      return Integer.parseInt(new String(data));
    } catch (KeeperException.NoNodeException e) {
      return -1; // Nenhum líder atualmente
    }
  }

  /**
   * Abandona a eleição
   */
  public void resign() throws KeeperException, InterruptedException {
    if (currentPath != null) {
      zk.delete(currentPath, -1);
      currentPath = null;
    }

    if (isLeader && leaderPath != null) {
      try {
        zk.delete(leaderPath, -1);
      } catch (KeeperException.NoNodeException e) {
        // Nó já foi removido
      }
      isLeader = false;
    }

    System.out.println("🚪 Election: Nó " + nodeId + " abandonou a eleição");
  }

  @Override
  public void process(WatchedEvent event) {
    synchronized (mutex) {
      if (event.getType() == Event.EventType.NodeDeleted) {
        try {
          // Candidato anterior saiu, verificar liderança
          checkLeadership();
        } catch (Exception e) {
          System.err.println("Erro ao processar evento de eleição: " + e.getMessage());
        }
      }
    }
  }
}
