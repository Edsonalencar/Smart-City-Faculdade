package device;

import auth.AuthService;
import core.SensorData;
import core.SensorDataGenerator;
import utils.*;

import java.net.*;
import java.io.*;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class Device {

    private final String deviceId;
    private String token;

    private static final String EDGE_HOST = "localhost";
    private static final int EDGE_PORT = 9876;

    private PublicKey edgePublicKey;

    public Device(String id, String token) {
        this.deviceId = id;
        this.token = token;

        try {
            // O dispositivo precisa da chave pública da Borda para criptografar a chave AES
            this.edgePublicKey = (PublicKey) KeyManager.loadKeyFromFile("edge_public.key");
            System.out.println("✅ Dispositivo " + id + ": Chave pública da Borda carregada.");
        } catch (Exception e) {
            System.err.println("❌ Dispositivo " + id + ": Falha ao carregar chave pública. Abortando.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void startSending() {
        // 1. Simulação do Passo de Autenticação
        System.out.println("🔐 Dispositivo " + deviceId + ": Tentando autenticação...");

        boolean isAuthenticated = AuthService.authenticate(deviceId, token);

        if (!isAuthenticated) {
            System.err.println("⛔ ERRO FATAL: Dispositivo " + deviceId + " falhou na autenticação! Credenciais inválidas.");
            return; // Encerra a execução deste dispositivo
        }

        System.out.println("✅ Dispositivo " + deviceId + ": Autenticado com sucesso! Iniciando envio...");

        // Simulação: Envia dados durante 5 minutos (300 segundos)
        long endTime = System.currentTimeMillis() + (5 * 60 * 1000);

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            while (System.currentTimeMillis() < endTime) {

                SensorData data = SensorDataGenerator.generate(deviceId);
                byte[] encryptedMessage = buildEncryptedHybridMessage(data);

                DatagramPacket sendPacket = new DatagramPacket(
                        encryptedMessage, encryptedMessage.length,
                        InetAddress.getByName(EDGE_HOST), EDGE_PORT
                );

                clientSocket.send(sendPacket);
                System.out.println("📤 Dispositivo " + deviceId + ": Enviando dados criptografados: " + data.toString());

                // Espera 2 ou 3 segundos
                long sleepTime = (Math.random() > 0.5) ? 2000 : 3000;
                Thread.sleep(sleepTime);
            }
        } catch (Exception e) {
            System.err.println("❌ Dispositivo " + deviceId + ": Erro ao enviar dados: " + e.getMessage());
        }
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