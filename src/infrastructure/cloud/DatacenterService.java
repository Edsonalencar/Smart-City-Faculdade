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
import java.util.Date;
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

            byte[] encryptedKey = new byte[256];
            byte[] encryptedPayload = new byte[fullMessage.length - 256];

            System.arraycopy(fullMessage, 0, encryptedKey, 0, 256);
            System.arraycopy(fullMessage, 256, encryptedPayload, 0, encryptedPayload.length);

            byte[] aesKeyBytes = RSAUtil.decrypt(encryptedKey, privateKey);
            SecretKey sessionKey = AESUtil.bytesToKey(aesKeyBytes);

            byte[] decryptedPayload = AESUtil.decrypt(encryptedPayload, sessionKey);

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


    public static String processHttpRequest(String method, String path, String authToken) {
        if (!authToken.equals("admin_secret_123")) {
            return "HTTP/1.1 403 FORBIDDEN\n\nErro: Acesso Negado via Token.";
        }

        if (!method.equals("GET")) {
            return "HTTP/1.1 405 METHOD NOT ALLOWED\n\nApenas GET é suportado.";
        }

        String responseBody = "";

        switch (path) {
            case "/api/reports/pollution":
                responseBody = reportPollutionIndex();
                break;
            case "/api/alerts/safety":
                responseBody = reportSafetyAlerts();
                break;
            case "/api/health/recommendations":
                responseBody = reportHealthRecommendations();
                break;
            case "/api/maintenance/status":
                responseBody = reportDeviceStatus();
                break;
            case "/api/forecast/trends":
                responseBody = reportFutureTrends();
                break;
            default:
                return "HTTP/1.1 404 NOT FOUND\n\nEndpoint não encontrado.";
        }

        return "HTTP/1.1 200 OK\n" +
                "Date: " + new Date() + "\n" +
                "Content-Type: text/plain; charset=utf-8\n" +
                "Content-Length: " + responseBody.length() + "\n" +
                "\n" +
                responseBody;
    }


    //Relatório de Qualidade do Ar (AQI)
    private static String reportPollutionIndex() {
        StringBuilder sb = new StringBuilder("=== 🏭 RELATÓRIO DE QUALIDADE DO AR (Médias) ===\n");

        Map<String, Double> avgPM25 = database.stream()
                .collect(Collectors.groupingBy(SensorData::getDeviceId, Collectors.averagingDouble(SensorData::getPm25)));

        Map<String, Double> avgCO2 = database.stream()
                .collect(Collectors.groupingBy(SensorData::getDeviceId, Collectors.averagingDouble(SensorData::getCo2)));

        avgPM25.forEach((device, pm) -> {
            double co2 = avgCO2.getOrDefault(device, 0.0);
            String status = (pm > 25 || co2 > 1000) ? "[RUIM]" : "[BOM]";
            sb.append(String.format(" %s %s -> PM2.5: %.1f | CO2: %.0f ppm\n", status, device, pm, co2));
        });
        return sb.toString();
    }

    //Alertas de Segurança Pública (Ruído e Temp)
    private static String reportSafetyAlerts() {
        StringBuilder sb = new StringBuilder("=== 🚨 ALERTAS DE SEGURANÇA E EMERGÊNCIA ===\n");

        long noiseAlerts = database.stream().filter(d -> d.getNoiseLevel() > 75.0).count();
        long fireRisk = database.stream().filter(d -> d.getTemperature() > 50.0).count(); // Exagero para teste

        if (noiseAlerts == 0 && fireRisk == 0) return sb.append("✅ Nenhum incidente de segurança detectado.").toString();

        if (noiseAlerts > 0) sb.append(String.format("⚠️ ALERTA: %d ocorrências de ruído excessivo (>75dB).\n", noiseAlerts));
        if (fireRisk > 0) sb.append(String.format("🔥 PERIGO: %d sensores detectaram calor extremo (>50°C)!\n", fireRisk));

        return sb.toString();
    }

    // Recomendações de Saúde (Baseado em UV e Umidade)
    private static String reportHealthRecommendations() {
        double avgUV = database.stream().mapToDouble(SensorData::getUvIndex).average().orElse(0);
        double avgHum = database.stream().mapToDouble(SensorData::getHumidity).average().orElse(0);

        StringBuilder sb = new StringBuilder("=== 🏥 BOLETIM DE SAÚDE PÚBLICA ===\n");
        sb.append(String.format(" - Índice UV Médio: %.1f\n - Umidade Média: %.1f%%\n\n", avgUV, avgHum));

        sb.append("RECOMENDAÇÕES:\n");
        if (avgUV > 6.0) sb.append(" ☀️ ALTO RISCO UV: Use protetor solar e evite exposição direta.\n");
        else sb.append(" ☁️ UV Baixo: Exposição segura.\n");

        if (avgHum < 30.0) sb.append(" 💧 AR SECO: Hidrate-se e evite exercícios ao ar livre.\n");
        else sb.append(" 🏃 Umidade ideal para práticas esportivas.\n");

        return sb.toString();
    }

    //Status de Manutenção dos Sensores
    private static String reportDeviceStatus() {
        StringBuilder sb = new StringBuilder("=== 🛠️ STATUS TÉCNICO DA REDE ===\n");

        Map<String, Long> msgCount = database.stream()
                .collect(Collectors.groupingBy(SensorData::getDeviceId, Collectors.counting()));

        msgCount.forEach((device, count) -> {
            String health = count < 3 ? "⚠️ VERIFICAR" : "✅ ONLINE";
            sb.append(String.format("Device: %-15s | Pkts: %02d | Status: %s\n", device, count, health));
        });
        return sb.toString();
    }

    //Previsão de Tendências (Temperatura)
    private static String reportFutureTrends() {
        if (database.size() < 4) return "Dados insuficientes para previsão.";

        int split = database.size() / 2;
        double firstHalfAvg = database.subList(0, split).stream().mapToDouble(SensorData::getTemperature).average().orElse(0);
        double secondHalfAvg = database.subList(split, database.size()).stream().mapToDouble(SensorData::getTemperature).average().orElse(0);

        String arrow = (secondHalfAvg > firstHalfAvg) ? "📈 EM ALTA" : "📉 EM QUEDA";

        return String.format("=== 🔮 PREVISÃO METEOROLÓGICA ===\n" +
                "Tendência Térmica: %s\n" +
                "Variação calculada: %.2f°C -> %.2f°C", arrow, firstHalfAvg, secondHalfAvg);
    }
}