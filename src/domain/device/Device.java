package domain.device;

import core.SensorData;
import core.SensorDataGenerator;
import infrastructure.auth.AuthService;
import utils.EncryptMessageUtil;
import utils.KeyManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;

public class Device implements Runnable {
    private final String deviceId;
    private final String token;
    private PublicKey edgePublicKey;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public Device(String id, String token) {
        this.deviceId = id;
        this.token = token;
        try {
            this.edgePublicKey = (PublicKey) KeyManager.loadKeyFromFile("edge_public.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running.set(false);
        System.out.println("⏹️ Dispositivo " + deviceId + ": Recebeu ordem de parada.");
    }

    @Override
    public void run() {
        startSending();
    }

    public void startSending() {
        // 1. Autenticação
        System.out.println("🔐 Dispositivo " + deviceId + ": Tentando autenticação...");
        if (!AuthService.authenticate(deviceId, token)) {
            System.err.println("⛔ ERRO FATAL: Dispositivo " + deviceId + " falhou na autenticação!");
            return;
        }
        System.out.println("✅ Dispositivo " + deviceId + ": Autenticado! Iniciando envio...");

        // Define como rodando
        running.set(true);

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            // Loop verifica a flag 'running' a cada iteração
            while (running.get()) {

                SensorData data = SensorDataGenerator.generate(deviceId);
                byte[] encryptedMessage = EncryptMessageUtil.encryptedHybridMessage(data, edgePublicKey);

                DatagramPacket sendPacket = new DatagramPacket(
                    encryptedMessage, encryptedMessage.length,
                    InetAddress.getByName("localhost"), 9876
                );

                clientSocket.send(sendPacket);
                System.out.println("📤 Dispositivo " + deviceId + ": Enviou dados.");

                // Pausa entre envios
                long sleepTime = (Math.random() > 0.5) ? 2000 : 3000;
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            System.out.println("⏹️ Dispositivo " + deviceId + ": Interrompido durante o sono.");
        } catch (Exception e) {
            System.err.println("❌ Dispositivo " + deviceId + " erro: " + e.getMessage());
        }

        System.out.println("💤 Dispositivo " + deviceId + ": Desligado.");
    }
}