import cloud.Datacenter;
import device.Device;
import edge.EdgeServer;
import utils.KeyManager;

public class Main {
    public static void main(String[] args) {
        // Gera e salva chaves RSA em arquivos
        KeyManager.generateAndSaveAllKeys();

        System.out.println("Iniciando a simulação de 4 dispositivos enviando dados por 5 minutos...");

        // Inicia o Servidor de Borda
        new EdgeServer().start();

        // Inicia o DataCenter
        new Datacenter().start();

        System.out.println("Iniciando a simulação de dispositivos e autenticação...");

        // 2. Dispositivos Legítimos (Token correto)
        new Thread(() -> new Device("DEV_01_POSTE", "secret_token_01").startSending()).start();
        new Thread(() -> new Device("DEV_02_SEMAFORO", "secret_token_02").startSending()).start();
        new Thread(() -> new Device("DEV_03_TOTEM", "secret_token_03").startSending()).start();

        // Vamos dar um tempinho para ver os legítimos rodando
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // 3. Dispositivo HACKER/Malicioso (Token errado)
        // Este aqui deve falhar e imprimir a mensagem de erro fatal
        new Thread(() -> new Device("DEV_HACKER", "token_falso_123").startSending()).start();

        // 4. Mais um legítimo depois
        new Thread(() -> new Device("DEV_04_CAIXA", "secret_token_04").startSending()).start();
    }
}