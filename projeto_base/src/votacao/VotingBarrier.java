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
 * Implementação de Barrier distribuída usando ZooKeeper
 * Permite que múltiplos nós aguardem até que todos estejam prontos
 */
public class VotingBarrier implements Watcher {

  private ZooKeeper zk;
  private String root;
  private int size;
  private String name;
  private Object mutex = new Object();

  /**
   * Construtor do Barrier
   * 
   * @param address Endereço do ZooKeeper
   * @param root    Caminho raiz do barrier
   * @param size    Número de nós que devem participar
   */
  public VotingBarrier(String address, String root, int size) throws IOException {
    this.root = root;
    this.size = size;
    this.zk = new ZooKeeper(address, 3000, this);
    this.name = System.currentTimeMillis() + "-" + Thread.currentThread().getId();

    try {
      // Criar o nó raiz se não existir
      Stat s = zk.exists(root, false);
      if (s == null) {
        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (KeeperException e) {
      System.err.println("Erro ao criar nó raiz do barrier: " + e.getMessage());
    } catch (InterruptedException e) {
      System.err.println("Interrompido ao criar barrier: " + e.getMessage());
    }
  }

  /**
   * Entra no barrier - aguarda até que todos os nós estejam prontos
   * 
   * @return true se conseguiu entrar no barrier
   */
  public boolean enter() throws KeeperException, InterruptedException {
    // Criar nó ephemeral para indicar presença
    String path = zk.create(root + "/" + name + "-", new byte[0],
        Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

    System.out.println("Barrier: Nó criado em " + path);

    while (true) {
      synchronized (mutex) {
        List<String> list = zk.getChildren(root, true);

        if (list.size() < size) {
          System.out.println("Barrier: Aguardando... (" + list.size() + "/" + size + " nós)");
          mutex.wait();
        } else {
          System.out.println("Barrier: Todos os nós estão prontos! Prosseguindo...");
          return true;
        }
      }
    }
  }

  /**
   * Sai do barrier - remove o nó criado
   */
  public boolean leave() throws KeeperException, InterruptedException {
    // Em uma implementação mais robusta, manteria referência ao path criado
    List<String> list = zk.getChildren(root, false);

    for (String child : list) {
      if (child.startsWith(name)) {
        zk.delete(root + "/" + child, -1);
        System.out.println("Barrier: Saí da barrier");
        break;
      }
    }

    while (true) {
      synchronized (mutex) {
        list = zk.getChildren(root, true);
        if (list.size() > 0) {
          mutex.wait();
        } else {
          return true;
        }
      }
    }
  }

  @Override
  public void process(WatchedEvent event) {
    synchronized (mutex) {
      mutex.notifyAll();
    }
  }
}
