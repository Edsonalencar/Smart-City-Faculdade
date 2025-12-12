package infrastructure.ids;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IDS {
    private static final int IDS_PORT = 7000;

    private static final String LOG_FILE = "ids_security_log.txt";

    public void start() {
        // Garante que o arquivo de log existe (ou cria um novo com cabeçalho)
        initializeLogFile();

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(IDS_PORT)) {
                System.out.println("🛡️ IDS: Monitorando alertas na porta " + IDS_PORT + "...");
                System.out.println("📝 IDS: Gravando logs em '" + LOG_FILE + "'");

                while (true) {
                    Socket alertSocket = serverSocket.accept();
                    // Processa cada alerta em uma nova thread para não travar o IDS
                    new Thread(() -> handleAlert(alertSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initializeLogFile() {
        File file = new File(LOG_FILE);
        if (!file.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("=== INÍCIO DO LOG DE SEGURANÇA (IDS) ===");
                writer.println("Data de Criação: " + new Date());
                writer.println("--------------------------------------------------");
            } catch (IOException e) {
                System.err.println("❌ IDS: Erro ao criar arquivo de log: " + e.getMessage());
            }
        }
    }

    private void handleAlert(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String alertMsg = reader.readLine();

            if (alertMsg != null) {
                // Grava no Arquivo
                logAlertToFile(alertMsg);

                if (alertMsg.contains("ANOMALIA_DETECTADA")) {
                    System.out.println("🚨 IDS: ALERTA CRÍTICO RECEBIDO! " + alertMsg);

                    // Extrai o ID. Formato esperado: "ANOMALIA_DETECTADA: DEVICE_ID Motivo..."
                    String[] parts = alertMsg.split(":");
                    if (parts.length > 1) {
                        String maliciousId = parts[1].trim().split(" ")[0]; // Pega a primeira palavra após os dois pontos

                        // Dispara comando de bloqueio para a Borda
                        sendCommandToEdge("BLOCK " + maliciousId);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("❌ IDS: Erro ao processar alerta: " + e.getMessage());
        }
    }

    private void sendCommandToEdge(String command) {
        try (Socket edgeSocket = new Socket("localhost", 9877);
             PrintWriter out = new PrintWriter(edgeSocket.getOutputStream(), true)) {

            out.println(command);
            String logMsg = "AÇÃO TOMADA: Comando enviado para Borda -> " + command;
            System.out.println("🛡️ IDS: " + logMsg);
            logAlertToFile(logMsg);

        } catch (IOException e) {
            System.err.println("❌ IDS: Falha ao contatar Borda: " + e.getMessage());
            logAlertToFile("ERRO CRÍTICO: Falha ao contatar Borda para bloqueio: " + e.getMessage());
        }
    }

    private synchronized void logAlertToFile(String msg) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] %s", timestamp, msg);

        // Imprime no console também para visualização em tempo real
        System.out.println("📝 IDS Log: " + msg);

        // Escreve no arquivo
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            System.err.println("❌ IDS: Erro ao escrever no arquivo de log: " + e.getMessage());
        }
    }
}