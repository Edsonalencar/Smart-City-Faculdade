package infrastructure.location;

import java.io.*;
import java.net.*;

public class LocationServer {
    private static final int PORT = 9002;

    // O serviço de ingestão: PacketFilter
    private static final String SERVICE_ADDRESS = "localhost:9800";

    public void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT)) {
                System.out.println("📍 LocationServer: Ouvindo na porta " + PORT);
                while (true) {
                    try (Socket client = server.accept();
                         PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

                        // Protocolo simples: Qualquer conexão recebe o endereço
                        out.println(SERVICE_ADDRESS);
                        System.out.println("📍 LocationServer: Endereço fornecido.");
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }
}