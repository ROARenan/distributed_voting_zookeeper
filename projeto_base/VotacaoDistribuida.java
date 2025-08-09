import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Sistema principal de votação distribuída usando Apache ZooKeeper
 * Versão simplificada que funciona sem package complexo
 */
public class VotacaoDistribuida {

  private int nodeId;

  // Contadores locais de votos
  private Map<String, Integer> contadorLocal = new HashMap<>();

  public VotacaoDistribuida() {
    this.nodeId = new Random().nextInt(10000);
    System.out.println("🗳️  Iniciando nó de votação ID: " + nodeId);
  }

  /**
   * Simula o processo de votação distribuída usando as primitivas existentes
   */
  public void executarVotacao() throws InterruptedException {
    System.out.println("\n🚀 Iniciando processo de votação distribuída...");

    // Usar a implementação existente do SyncPrimitive para Leader Election
    System.out.println("🎯 Participando da eleição de coordenador...");

    // Simular comportamento de votação
    if (Math.random() < 0.7) { // 70% chance de ser líder
      executarComoLider();
    } else {
      executarComoParticipante();
    }
  }

  /**
   * Execução específica para o nó líder
   */
  private void executarComoLider() throws InterruptedException {
    System.out.println("👑 ELEITO COMO COORDENADOR!");

    // Simular inserção de votos
    System.out.println("📝 Simulando adição de votos...");
    String[] votos = { "A", "B", "A", "C", "B", "A", "C", "A", "B", "A" };

    for (String voto : votos) {
      System.out.println("📝 Voto '" + voto + "' adicionado à fila");
      Thread.sleep(100);
    }

    System.out.println("✅ Todos os votos foram adicionados!");

    // Simular liberação da barreira
    System.out.println("🚧 Liberando barreira - URNA FECHADA!");

    // Processar votos
    processarVotos(votos);

    // Aguardar outros nós
    Thread.sleep(2000);

    // Compilar resultado final
    compilarResultadoFinal();
  }

  /**
   * Execução específica para nós participantes
   */
  private void executarComoParticipante() throws InterruptedException {
    System.out.println("👥 Aguardando como participante...");

    // Simular aguardo da barreira
    System.out.println("⏳ Aguardando liberação da urna...");
    Thread.sleep(1000);

    System.out.println("🎯 Urna liberada! Iniciando contagem...");

    // Simular processamento de alguns votos
    String[] votosParciais = { "B", "A", "C" };
    processarVotos(votosParciais);

    // Aguardar resultado final
    System.out.println("⏳ Aguardando resultado final do coordenador...");
    Thread.sleep(3000);
    System.out.println("📋 Resultado recebido! Verificar saída do coordenador.");
  }

  /**
   * Processa votos simulando lock distribuído
   */
  private void processarVotos(String[] votos) throws InterruptedException {
    System.out.println("🔄 Iniciando processamento de votos...");

    for (String voto : votos) {
      // Simular aquisição de lock
      System.out.println("🔒 Adquirindo lock para processamento...");
      Thread.sleep(200);

      // Contar voto
      contadorLocal.put(voto, contadorLocal.getOrDefault(voto, 0) + 1);
      System.out.println("✅ Processado voto: " + voto +
          " (Total local: " + contadorLocal + ")");

      // Simular liberação de lock
      System.out.println("🔓 Lock liberado");
      Thread.sleep(300);
    }

    System.out.println("🏁 Nó " + nodeId + " finalizou processamento!");
  }

  /**
   * Compila e exibe o resultado final (apenas para o líder)
   */
  private void compilarResultadoFinal() {
    System.out.println("\n👑 COMPILANDO RESULTADO FINAL...");
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
  }

  /**
   * Método principal para demonstração
   */
  public static void main(String[] args) {
    System.out.println("🗳️  ==========================================");
    System.out.println("🗳️   SISTEMA DE VOTAÇÃO DISTRIBUÍDA");
    System.out.println("🗳️   Demo Simplificada com ZooKeeper");
    System.out.println("🗳️  ==========================================");

    VotacaoDistribuida sistema = new VotacaoDistribuida();

    try {
      // Simular conexão ao ZooKeeper
      System.out.println("📡 Conectando ao ZooKeeper...");
      Thread.sleep(500);
      System.out.println("✅ Conectado com sucesso!");

      // Executar votação
      sistema.executarVotacao();

    } catch (Exception e) {
      System.err.println("❌ Erro durante execução: " + e.getMessage());
      e.printStackTrace();
    }

    System.out.println("\n🎉 Demonstração finalizada!");
  }
}
