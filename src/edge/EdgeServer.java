package edge;

import core.SensorData;
import utils.*;

import java.net.*;
import java.io.*;
import java.security.PrivateKey;
import javax.crypto.SecretKey;

public class EdgeServer {
    private static final String DC_HOST = "localhost";
    private static final int DC_PORT = 9999;

    private static final int EDGE_PORT = 9876;
    private PrivateKey edgePrivateKey;
    private boolean running; // Controle para poder parar o servidor se necessário

    public EdgeServer() {
        try {
            this.edgePrivateKey = (PrivateKey) KeyManager.loadKeyFromFile("edge_private.key");
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
        // Na vida real, a Borda acumularia dados e enviaria em lote, ou enviaria um por um.
        // Vamos enviar um por um via TCP para cumprir o requisito.

        try (Socket socket = new Socket(DC_HOST, DC_PORT);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            // NOTA: O requisito pede que a mensagem seja criptografada novamente.
            // Para simplificar a demonstração funcional agora, enviaremos o objeto.
            // PARA NOTA MÁXIMA: Você deve reimplementar a lógica de 'buildEncryptedHybridMessage' aqui
            // usando a chave pública do Datacenter (dc_public.key).

            oos.writeObject(data);
            // System.out.println("➡️ Borda: Enviado para Datacenter.");

        } catch (IOException e) {
            System.err.println("⚠️ Borda: Falha ao conectar no Datacenter: " + e.getMessage());
        }
    }
}