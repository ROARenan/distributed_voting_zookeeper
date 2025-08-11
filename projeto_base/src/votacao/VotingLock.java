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
import org.apache.zookeeper.data.Stat;

/**
 * Implementação de Lock distribuído usando ZooKeeper
 * Garante acesso exclusivo ao processamento de votos
 */
public class VotingLock implements Watcher {

  private ZooKeeper zk;
  private String root;
  private String lockPath;
  private String currentPath;
  private Object mutex = new Object();

  /**
   * Construtor do Lock
   * 
   * @param address Endereço do ZooKeeper
   * @param root    Caminho raiz do lock
   */
  public VotingLock(String address, String root) throws IOException {
    this.root = root;
    this.zk = new ZooKeeper(address, 3000, this);
    this.lockPath = root + "/lock-";

    try {
      // Criar o nó raiz se não existir
      Stat s = zk.exists(root, false);
      if (s == null) {
        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (KeeperException e) {
      System.err.println("Erro ao criar nó raiz do lock: " + e.getMessage());
    } catch (InterruptedException e) {
      System.err.println("Interrompido ao criar lock: " + e.getMessage());
    }
  }

  /**
   * Tenta adquirir o lock
   * 
   * @return true se conseguiu adquirir o lock
   */
  public boolean acquire() throws KeeperException, InterruptedException {
    // Criar nó ephemeral sequencial
    currentPath = zk.create(lockPath, new byte[0], Ids.OPEN_ACL_UNSAFE,
        CreateMode.EPHEMERAL_SEQUENTIAL);

    return checkLock();
  }

  /**
   * Verifica se este nó tem o lock
   */
  private boolean checkLock() throws KeeperException, InterruptedException {
    List<String> children = zk.getChildren(root, false);
    Collections.sort(children);

    // Filtrar apenas nós de lock
    String myNode = currentPath.substring(root.length() + 1);

    for (int i = 0; i < children.size(); i++) {
      if (children.get(i).equals(myNode)) {
        if (i == 0) {
          // Este é o primeiro nó - tem o lock
          System.out.println("Lock: Adquirido com sucesso!");
          return true;
        } else {
          // Aguardar o nó anterior
          String previousNode = children.get(i - 1);
          return waitForLock(previousNode);
        }
      }
    }

    return false;
  }

  /**
   * Aguarda o nó anterior ser removido
   */
  private boolean waitForLock(String previousNode) throws KeeperException, InterruptedException {
    String previousPath = root + "/" + previousNode;

    synchronized (mutex) {
      Stat s = zk.exists(previousPath, this);
      if (s != null) {
        System.out.println("Lock: Aguardando liberação...");
        mutex.wait();
        return checkLock(); // Verificar novamente após notificação
      } else {
        return checkLock(); // Nó anterior já foi removido
      }
    }
  }

  /**
   * Libera o lock
   */
  public void release() throws KeeperException, InterruptedException {
    if (currentPath != null) {
      zk.delete(currentPath, -1);
      currentPath = null;
      System.out.println("Lock: Liberado com sucesso!");
    }
  }

  /**
   * Tenta adquirir o lock com timeout
   * 
   * @param timeoutMs Tempo limite em milissegundos
   * @return true se conseguiu adquirir o lock no tempo especificado
   */
  public boolean tryAcquire(long timeoutMs) throws KeeperException, InterruptedException {
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      if (acquire()) {
        return true;
      }
      Thread.sleep(100); // Aguardar um pouco antes de tentar novamente
    }

    return false;
  }

  @Override
  public void process(WatchedEvent event) {
    synchronized (mutex) {
      if (event.getType() == Event.EventType.NodeDeleted) {
        mutex.notifyAll();
      }
    }
  }
}
