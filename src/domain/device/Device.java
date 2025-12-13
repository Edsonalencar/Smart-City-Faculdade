package domain.device;

import core.SensorData;
import core.SensorDataGenerator;
import infrastructure.auth.AuthService;
import utils.EncryptMessageUtil;
import utils.KeyManager;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;

public class Device implements Runnable {
    protected int targetPort = 9800;

    private final String deviceId;
    private final String token;
    private final String sourceIp;

    private PublicKey edgePublicKey;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public Device(String id, String token, String sourceIp) {
        this.deviceId = id;
        this.token = token;
        this.sourceIp = sourceIp;
        try {
            this.edgePublicKey = (PublicKey) KeyManager.loadKeyFromFile("edge_public.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected SensorData generateData() {
        return SensorDataGenerator.generate(deviceId);
    }

    public void setTargetPort(int port) {
        this.targetPort = port;
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
        System.out.println("📍 Dispositivo " + deviceId + ": Buscando servidor de borda...");

        String targetAddress = queryLocationServer();
        if (targetAddress == null) {
            System.err.println("❌ Erro ao localizar serviço.");
            return;
        }

        // Parse do endereço (localhost:9800)
        String host = targetAddress.split(":")[0];
        int port = Integer.parseInt(targetAddress.split(":")[1]);
        this.targetPort = port; // Atualiza dinamicamente!

        System.out.println("🔐 Dispositivo " + deviceId + ": Solicitando autenticação remota...");
        if (!performRemoteAuth()) {
            System.err.println("⛔ ERRO FATAL: Falha na autenticação remota!");
            return;
        }

        System.out.println("✅ Dispositivo " + deviceId + ": Pronto. Enviando para " + host + ":" + port);

        // Define como rodando
        running.set(true);

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            // Loop verifica a flag 'running' a cada iteração
            while (running.get()) {

                SensorData data = generateData();
                byte[] encryptedMessage = EncryptMessageUtil.encryptedHybridMessage(data, edgePublicKey);

                byte[] packetWithIpHeader = wrapWithIpHeader(encryptedMessage);

                DatagramPacket sendPacket = new DatagramPacket(
                    packetWithIpHeader, packetWithIpHeader.length,
                    InetAddress.getByName("localhost"), targetPort
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

    private byte[] wrapWithIpHeader(byte[] payload) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(this.sourceIp); // Escreve o IP (String)
        dos.writeInt(payload.length); // Escreve o tamanho do payload
        dos.write(payload);           // Escreve o payload criptografado

        return baos.toByteArray();
    }

    private String queryLocationServer() {
        try (Socket s = new Socket("localhost", 9002);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            return in.readLine();
        } catch (Exception e) { return null; }
    }

    private boolean performRemoteAuth() {
        try (Socket s = new Socket("localhost", 9001);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            out.println(deviceId + ":" + token);
            String response = in.readLine();
            return "OK".equals(response);
        } catch (Exception e) { return false; }
    }
}