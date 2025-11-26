import client.ClientApp;
import cloud.Datacenter;
import device.Device;
import edge.EdgeServer;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Iniciando Infraestrutura da Simulação ---");

        // 1. Infraestrutura
        new Datacenter().start();
        new EdgeServer().start();

        System.out.println("--- Iniciando Dispositivos ---");

        // Lista para guardar referência aos objetos Device
        List<Device> activeDevices = new ArrayList<>();

        // Cria os dispositivos
        Device dev1 = new Device("DEV_01_POSTE", "secret_token_01");
        Device dev2 = new Device("DEV_02_SEMAFORO", "secret_token_02");
        Device dev3 = new Device("DEV_03_TOTEM", "secret_token_03");
        Device dev4 = new Device("DEV_04_CAIXA", "secret_token_04");
        Device devHacker = new Device("DEV_HACKER", "token_falso_123");

        // Adiciona na lista os que queremos controlar (o hacker vai morrer sozinho, mas podemos adicionar tb)
        activeDevices.add(dev1);
        activeDevices.add(dev2);
        activeDevices.add(dev3);
        activeDevices.add(dev4);
        activeDevices.add(devHacker);

        // Inicia as Threads
        new Thread(dev1).start();
        new Thread(dev2).start();
        new Thread(dev3).start();

        try { Thread.sleep(1000); } catch (Exception e) {}

        new Thread(devHacker).start(); // Vai falhar e parar sozinho
        new Thread(dev4).start();

        System.out.println("\n⏳ Coletando dados por 15 segundos...\n");

        try {
            Thread.sleep(15000); // Roda a simulação por 15s
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n🛑 TEMPO ESGOTADO. PARANDO TODOS OS DISPOSITIVOS...\n");

        for (Device d : activeDevices) {
            d.stop();
        }

        // Dá um segundinho para os logs de "Desligado" aparecerem
        try { Thread.sleep(2000); } catch (Exception e) {}

        System.out.println("\n--- 🏁 INICIANDO CONSULTAS DO CLIENTE ---\n");

        new ClientApp("GESTOR_PUBLICO", "admin_secret_123").start();
    }
}