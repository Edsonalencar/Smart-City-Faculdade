package infrastructure.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private static final int PORT = 9001;
    private static final ConcurrentHashMap<String, String> db = new ConcurrentHashMap<>();

    static {
        db.put("DEV_01_POSTE", "secret_token_01");
        db.put("DEV_02_SEMAFORO", "secret_token_02");
        db.put("DEV_03_TOTEM", "secret_token_03");
        db.put("DEV_04_CAIXA", "secret_token_04");
        db.put("DEV_05_GAIVOTA", "secret_token_05");
        db.put("DEV_06_ESTACAO_METEOROLOGICA", "secret_token_06");
        db.put("GESTOR_PUBLICO", "admin_secret_123");
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT)) {
                System.out.println("🔑 AuthServer: Ouvindo na porta " + PORT);

                while (true) {
                    new Thread(new Handler(server.accept())).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private static class Handler implements Runnable {
        private Socket socket;
        public Handler(Socket s) { this.socket = s; }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Protocolo simples: "ID:TOKEN"
                String credentials = in.readLine();
                if (credentials != null && credentials.contains(":")) {
                    String[] parts = credentials.split(":");
                    String id = parts[0];
                    String token = parts[1];

                    if (token.equals(db.get(id))) {
                        out.println("OK"); // Autenticado
                        System.out.println("🔑 AuthServer: " + id + " autenticado.");
                    } else {
                        out.println("FAIL");
                        System.out.println("⛔ AuthServer: Falha para " + id);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}