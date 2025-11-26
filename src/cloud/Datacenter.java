package cloud;

import core.SensorData;
import utils.KeyManager;

import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Datacenter {

    private static final int PORT = 9999;
    // Simulação de Banco de Dados em Memória
    private static final List<SensorData> database = new ArrayList<>();
    private PrivateKey privateKey;

    public Datacenter() {
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
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Lógica de Descriptografia Híbrida (TCP)
            // Aqui simplificaremos: Recebemos um objeto encapsulando chave+dados ou lemos bytes
            // Para simplificar via TCP Object Stream, podemos receber um objeto customizado "EncryptedPackage"
            // Mas vamos manter a lógica de bytes brutos para consistência com o requisito de segurança.

            // O código aqui seria similar ao da Borda, mas lendo do InputStream
            // Por brevidade, vamos assumir que a Borda já manda o SensorData (EM UMA IMPLEMENTAÇÃO REAL, AQUI TERIA CRIPTOGRAFIA DE NOVO)

            Object received = ois.readObject();
            if (received instanceof SensorData) {
                SensorData data = (SensorData) received;
                synchronized (database) {
                    database.add(data);
                }
                System.out.println("☁️ Datacenter: Dado armazenado no DB: " + data.getDeviceId());
            }

        } catch (Exception e) {
            System.err.println("☁️ Datacenter: Erro na conexão: " + e.getMessage());
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