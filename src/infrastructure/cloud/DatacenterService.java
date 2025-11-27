package infrastructure.cloud;

import core.SensorData;
import utils.AESUtil;
import utils.KeyManager;
import utils.RSAUtil;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatacenterService {

    private static final int PORT = 9999;
    // Simulação de Banco de Dados em Memória
    private static final List<SensorData> database = new ArrayList<>();
    private PrivateKey privateKey;

    public DatacenterService() {
        try {
            this.privateKey = (PrivateKey) KeyManager.loadKeyFromFile("dc_private.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("☁️ Datacenter: Ouvindo TCP na porta " + PORT + "...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (InputStream in = socket.getInputStream()) {

            // 1. Ler todos os bytes recebidos da Borda
            // Como é uma conexão curta, podemos ler até o fim da stream (-1)
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024]; // Buffer de leitura
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] fullMessage = buffer.toByteArray();

            // Verifica se a mensagem tem pelo menos o tamanho do cabeçalho RSA
            if (fullMessage.length <= 256) {
                System.err.println("☁️ Datacenter: Mensagem recebida muito curta ou vazia.");
                return;
            }

            // 2. Separar Chave Criptografada (256 bytes) do Payload
            byte[] encryptedKey = new byte[256];
            byte[] encryptedPayload = new byte[fullMessage.length - 256];

            System.arraycopy(fullMessage, 0, encryptedKey, 0, 256);
            System.arraycopy(fullMessage, 256, encryptedPayload, 0, encryptedPayload.length);

            // 3. Descriptografar a Chave AES (Usando a Privada do Datacenter)
            byte[] aesKeyBytes = RSAUtil.decrypt(encryptedKey, privateKey);
            SecretKey sessionKey = AESUtil.bytesToKey(aesKeyBytes);

            // 4. Descriptografar o Payload
            byte[] decryptedPayload = AESUtil.decrypt(encryptedPayload, sessionKey);

            // 5. Desserializar o Objeto
            try (ByteArrayInputStream bis = new ByteArrayInputStream(decryptedPayload);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {

                Object received = ois.readObject();
                if (received instanceof SensorData) {
                    SensorData sensorData = (SensorData) received;

                    synchronized (database) {
                        database.add(sensorData);
                    }
                    System.out.println("☁️ Datacenter: Recebido Seguro e Armazenado: " + sensorData.getDeviceId());
                }
            }
        } catch (Exception e) {
            System.err.println("☁️ Datacenter: Erro de Descriptografia/Conexão: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<SensorData> getDatabase() {
        return database;
    }

    // Relatório: Média de Poluentes por Bairro (Baseado no ID do dispositivo)
    public static String generatePollutionReport() {
        if (database.isEmpty()) return "Nenhum dado disponível para relatório.";

        // Agrupa por dispositivo e calcula média de CO2
        Map<String, Double> avgCO2 = database.stream()
                .collect(Collectors.groupingBy(
                        SensorData::getDeviceId,
                        Collectors.averagingDouble(SensorData::getCo2)
                ));

        StringBuilder sb = new StringBuilder("=== RELATÓRIO DE POLUIÇÃO (Média CO2) ===\n");
        avgCO2.forEach((device, value) ->
                sb.append(String.format(" - %s: %.2f ppm\n", device, value)));

        return sb.toString();
    }

    //Alerta: Pico de Ruído Urbano
    //Identifica áreas que ultrapassaram 75dB (limite de estresse acústico).
    public static String checkNoiseAlerts() {
        long highNoiseCount = database.stream()
                .filter(d -> d.getNoiseLevel() > 75.0)
                .count();

        if (highNoiseCount > 0) {
            return "⚠️ ALERTA CRÍTICO: Detectados " + highNoiseCount + " registros de ruído acima de 75dB! Ação recomendada: Fiscalização de trânsito.";
        }
        return "✅ Nível de ruído urbano dentro dos limites aceitáveis.";
    }

    //Previsão Simples: Tendência de Temperatura
    //Compara a primeira metade dos dados com a segunda para ver se está esquentando.
    public static String predictTemperatureTrend() {
        if (database.size() < 10) return "Dados insuficientes para previsão.";

        int split = database.size() / 2;
        double avgFirstHalf = database.subList(0, split).stream().mapToDouble(SensorData::getTemperature).average().orElse(0);
        double avgSecondHalf = database.subList(split, database.size()).stream().mapToDouble(SensorData::getTemperature).average().orElse(0);

        if (avgSecondHalf > avgFirstHalf + 0.5) {
            return "📈 PREVISÃO: Tendência de AUMENTO de temperatura detectada nas últimas horas.";
        } else if (avgSecondHalf < avgFirstHalf - 0.5) {
            return "📉 PREVISÃO: Tendência de QUEDA de temperatura.";
        }
        return "➡️ PREVISÃO: Temperatura estável.";
    }
}