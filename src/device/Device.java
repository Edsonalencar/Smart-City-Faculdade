package device;

import auth.AuthService;
import core.SensorData;
import core.SensorDataGenerator;
import utils.*;

import java.net.*;
import java.io.*;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKey;

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
                byte[] encryptedMessage = buildEncryptedHybridMessage(data);

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

    private byte[] buildEncryptedHybridMessage(SensorData data) throws Exception {
        // 1. Serializar o Objeto SensorData
        byte[] payloadBytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(data);
            payloadBytes = bos.toByteArray();
        }

        // 2. Gerar Chave AES de Sessão
        SecretKey sessionKey = AESUtil.generateKey();

        // 3. Criptografar o Payload com AES (rápido)
        byte[] encryptedPayload = AESUtil.encrypt(payloadBytes, sessionKey);

        // 4. Criptografar a Chave AES com a Chave Pública RSA da Borda
        byte[] encryptedAesKeyBytes = RSAUtil.encrypt(sessionKey.getEncoded(), edgePublicKey);

        // CORREÇÃO AQUI: O tamanho deve ser exatamente 256 bytes para RSA-2048
        if (encryptedAesKeyBytes.length != 256) {
            throw new IllegalArgumentException("Erro criptografia RSA: Tamanho inesperado " + encryptedAesKeyBytes.length);
        }

        // Não precisamos mais criar um buffer maior (rsaKeyBuffer de 512),
        // usamos o encryptedAesKeyBytes diretamente pois ele já tem o tamanho correto.

        ByteArrayOutputStream finalMessageStream = new ByteArrayOutputStream();
        finalMessageStream.write(encryptedAesKeyBytes); // Escreve exatamente 256 bytes
        finalMessageStream.write(encryptedPayload);     // Escreve o resto (dados)

        return finalMessageStream.toByteArray();
    }
}