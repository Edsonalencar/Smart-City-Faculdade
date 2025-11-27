package infrastructure.edge;

import core.SensorData;
import utils.*;

import java.net.*;
import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class EdgeServer {
    private static final String DC_HOST = "localhost";
    private static final int DC_PORT = 9999;

    private static final int EDGE_PORT = 9876;
    private PrivateKey edgePrivateKey;
    private PublicKey dcPublicKey;

    private boolean running; // Controle para poder parar o servidor se necessário

    public EdgeServer() {
        try {
            this.edgePrivateKey = (PrivateKey) KeyManager.loadKeyFromFile("edge_private.key");
            this.dcPublicKey = (PublicKey) KeyManager.loadKeyFromFile("dc_public.key");

            System.out.println("✅ Borda: Chave privada carregada.");
        } catch (Exception e) {
            System.err.println("❌ Borda: Falha ao carregar chave. " + e.getMessage());
        }
    }

    public void start() {
        this.running = true;
        // Cria uma Thread dedicada para o servidor não travar o main
        new Thread(this::runServer).start();
    }

    private void runServer() {
        try (DatagramSocket serverSocket = new DatagramSocket(EDGE_PORT)) {
            System.out.println("📡 Borda: Servidor UDP ouvindo na porta " + EDGE_PORT + " (em background)...");

            byte[] receiveData = new byte[65535];

            while (running) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket); // Bloqueia aqui esperando dados

                // Copia os dados para um novo array para processar (thread-safe)
                byte[] rawReceivedData = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), 0, rawReceivedData, 0, receivePacket.getLength());

                // 🔥 CRÍTICO: Processa a mensagem em uma NOVA thread separada.
                // Isso libera o servidor imediatamente para ouvir o próximo pacote.
                new Thread(() -> processEncryptedMessage(rawReceivedData)).start();
            }
        } catch (Exception e) {
            System.err.println("❌ Erro no Servidor de Borda: " + e.getMessage());
        }
    }

    private void processEncryptedMessage(byte[] rawData) {
        // [Lógica idêntica a anterior de Descriptografia Híbrida]
        int rsaKeySize = 256;

        if (rawData.length <= rsaKeySize) return;

        try {
            byte[] encryptedAesKeyBytes = new byte[rsaKeySize];
            byte[] encryptedPayload = new byte[rawData.length - rsaKeySize];

            System.arraycopy(rawData, 0, encryptedAesKeyBytes, 0, rsaKeySize);
            System.arraycopy(rawData, rsaKeySize, encryptedPayload, 0, encryptedPayload.length);

            // 1. Descriptografa Chave AES (Lento)
            byte[] decryptedAesKeyBytes = RSAUtil.decrypt(encryptedAesKeyBytes, edgePrivateKey);
            SecretKey sessionKey = AESUtil.bytesToKey(decryptedAesKeyBytes);

            // 2. Descriptografa Payload (Rápido)
            byte[] decryptedPayload = AESUtil.decrypt(encryptedPayload, sessionKey);

            // 3. Desserializa
            try (ByteArrayInputStream bis = new ByteArrayInputStream(decryptedPayload);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {

                SensorData data = (SensorData) ois.readObject();
                // Simula um processamento/análise
                System.out.println("✅ [THREAD " + Thread.currentThread().getId() + "] Borda recebeu de " + data.toString());
                sendToDatacenter(data);
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar pacote: " + e.getMessage());
        }
    }

    private void sendToDatacenter(SensorData data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);

            byte[] payloadBytes = bos.toByteArray();
            SecretKey sessionKey = AESUtil.generateKey();

            byte[] encryptedPayload = AESUtil.encrypt(payloadBytes, sessionKey);
            byte[] encryptedAesKey = RSAUtil.encrypt(sessionKey.getEncoded(), dcPublicKey);

            if (encryptedAesKey.length != 256) {
                throw new IllegalStateException("Tamanho de chave criptografada incorreto!");
            }

            try (Socket socket = new Socket(DC_HOST, DC_PORT);
                 OutputStream out = socket.getOutputStream()) {

                out.write(encryptedAesKey);
                out.write(encryptedPayload);
                out.flush();
            }

            System.out.println("➡️ Borda: Dados encaminhados criptografados ao Datacenter.");
        } catch (Exception e) {
            System.err.println("⚠️ Borda: Falha ao enviar ao Datacenter: " + e.getMessage());
        }
    }
}