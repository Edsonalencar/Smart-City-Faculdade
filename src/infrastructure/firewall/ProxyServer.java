package infrastructure.firewall;

import java.io.*;
import java.net.*;

public class ProxyServer {
    private static final int LISTEN_PORT = 9900; // Borda conecta aqui
    private static final int TARGET_PORT = 9999; // Datacenter ouve aqui

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(LISTEN_PORT)) {
                System.out.println("walls Firewall (Proxy): Interceptando na porta " + LISTEN_PORT + "...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Para cada conexão da Borda, abre um túnel para o Datacenter
                    new Thread(() -> handleProxyConnection(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleProxyConnection(Socket clientSocket) {
        try (Socket serverSocket = new Socket("localhost", TARGET_PORT);
             InputStream clientIn = clientSocket.getInputStream();
             OutputStream clientOut = clientSocket.getOutputStream();
             InputStream serverIn = serverSocket.getInputStream();
             OutputStream serverOut = serverSocket.getOutputStream()) {

            // Log de conexão TCP
            System.out.println("🔄 Proxy: Encaminhando tráfego Borda -> Datacenter.");

            // Thread para ler da Borda e mandar pro Datacenter
            new Thread(() -> copyStream(clientIn, serverOut)).start();

            // Lê do Datacenter e manda pra Borda (Resposta)
            copyStream(serverIn, clientOut);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyStream(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            // Conexão fechada
        }
    }
}