import domain.client.ClientApp;
import domain.device.Device;
import domain.device.MaliciousDevice;
import infrastructure.auth.AuthService;
import infrastructure.cloud.DatacenterService;
import infrastructure.edge.EdgeServer;
import infrastructure.firewall.PacketFilter;
import infrastructure.firewall.ProxyServer;
import infrastructure.ids.IDS;
import infrastructure.location.LocationServer;
import utils.KeyManager;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- INICIANDO SIMULAÇÃO---\n\n");

        KeyManager.generateAndSaveAllKeys();

        System.out.println("\n--- INICIANDO SERVIÇOS AUXILIARES ---");
        new AuthService().start();
        new LocationServer().start();

        try { Thread.sleep(1000); } catch (Exception e) {}

        System.out.println("\n--- INICIANDO ARQUITETURA DE SEGURANÇA ---");
        new IDS().start(); // Intrusion Detection System
        new ProxyServer().start();
        new DatacenterService().start();
        new EdgeServer().start();
        new PacketFilter().start();

        try { Thread.sleep(1000); } catch (Exception e) {}

        System.out.println("\n--- INICIANDO DISPOSITIVOS ---");

        // Lista para guardar referência aos objetos Device
        List<Device> activeDevices = new ArrayList<>();

        // Cria os dispositivos
        Device dev1 = new Device("DEV_01_POSTE", "secret_token_01", "192.168.0.101");
        Device dev2 = new Device("DEV_02_SEMAFORO", "secret_token_02", "192.168.0.102");
        Device dev3 = new Device("DEV_03_TOTEM", "secret_token_03", "192.168.0.103");
        Device dev4 = new Device("DEV_04_CAIXA", "secret_token_04", "192.168.0.104");

        Device devHacker = new Device("DEV_HACKER", "token_falso_123", "192.168.0.401");

        MaliciousDevice dev5 = new MaliciousDevice("DEV_05_GAIVOTA", "secret_token_05", "192.168.0.105");
        MaliciousDevice dev6 = new MaliciousDevice("DEV_06_ESTACAO_METEOROLOGICA", "secret_token_06", "192.168.0.404");

        // Adiciona na lista os que queremos controlar (o hacker vai morrer sozinho, mas podemos adicionar tb)
        activeDevices.add(dev1);
        activeDevices.add(dev2);
        activeDevices.add(dev3);
        activeDevices.add(dev4);
        activeDevices.add(dev5);
        activeDevices.add(dev6);
        activeDevices.add(devHacker);

        // Inicia as Threads
        new Thread(dev1).start();
        new Thread(dev2).start();
        new Thread(dev3).start();

        try { Thread.sleep(1000); } catch (Exception e) {}

        new Thread(devHacker).start(); // Vai falhar e parar sozinho
        new Thread(dev4).start();
        new Thread(dev5).start();
        new Thread(dev6).start();

        try { Thread.sleep(1000); } catch (Exception e) {}

        System.out.println("\n\n⏳ Coletando dados por 15 segundos...");

        try {
            Thread.sleep(15000); // Roda a simulação por 15s
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n\n🛑 TEMPO ESGOTADO. PARANDO TODOS OS DISPOSITIVOS...\n\n");

        for (Device d : activeDevices) {
            d.stop();
        }

        // Dá um segundinho para os logs de "Desligado" aparecerem
        try { Thread.sleep(2000); } catch (Exception e) {}

        System.out.println("\n--- 🏁 INICIANDO CONSULTAS DO CLIENTE ---\n");

        new ClientApp("GESTOR_PUBLICO", "admin_secret_123").start();
    }
}