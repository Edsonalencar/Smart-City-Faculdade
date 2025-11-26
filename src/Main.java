import device.Device;
import edge.EdgeServer;
import utils.KeyManager;

public class Main {
    public static void main(String[] args) {

        // Gera e salva chaves RSA em arquivos
        KeyManager.generateAndSaveAllKeys();

        // Inicia o Servidor
        new EdgeServer().start();

        // Quatro dispositivos coletam e enviam dados a cada 2 ou 3 segundos.

        new Thread(() -> new Device("DEV_01_POSTE").startSending()).start();
        new Thread(() -> new Device("DEV_02_SEMAFORO").startSending()).start();
        new Thread(() -> new Device("DEV_03_TOTEM").startSending()).start();
        new Thread(() -> new Device("DEV_04_CAIXA").startSending()).start();

        System.out.println("Iniciando a simulação de 4 dispositivos enviando dados por 5 minutos...");
    }
}