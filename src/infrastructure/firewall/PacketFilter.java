package infrastructure.firewall;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PacketFilter {
    private static final int EXTERNAL_PORT = 9800; // Porta exposta aos dispositivos
    private static final int EDGE_DEST_PORT = 9876; // Porta real da Borda (DMZ)

    private static final Set<String> ALLOWED_IPS = ConcurrentHashMap.newKeySet();

    public PacketFilter() {
        ALLOWED_IPS.add("192.168.0.101"); // Device 1
        ALLOWED_IPS.add("192.168.0.102"); // Device 2
        ALLOWED_IPS.add("192.168.0.103"); // Device 3
        ALLOWED_IPS.add("192.168.0.104"); // Device 4
        ALLOWED_IPS.add("192.168.0.105"); // Device 4

        ALLOWED_IPS.add("192.168.0.401"); // IP do Hacker (Permitido no FW, pego no IDS)
    }

    public void start() {
        new Thread(() -> {
            try (DatagramSocket proxySocket = new DatagramSocket(EXTERNAL_PORT)) {
                System.out.println("🧱 Firewall (Packet Filter): Ativo na porta " + EXTERNAL_PORT);
                System.out.println("📋 Regras Carregadas: " + ALLOWED_IPS.size() + " IPs permitidos.");

                byte[] buffer = new byte[65535];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    proxySocket.receive(packet);

                    processAndForward(proxySocket, packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processAndForward(DatagramSocket socket, DatagramPacket packet) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
             DataInputStream dis = new DataInputStream(bais)) {

            // Ler Cabeçalho Simulado (IP)
            String sourceIp = dis.readUTF();
            int payloadLength = dis.readInt();

            // Access Control List
            if (ALLOWED_IPS.contains(sourceIp)) {
                // PERMITIDO: Extrair payload original e encaminhar
                byte[] originalPayload = new byte[payloadLength];
                dis.readFully(originalPayload);

                // Encaminha para a Borda SÓ criptografia
                DatagramPacket forwardPacket = new DatagramPacket(
                    originalPayload, originalPayload.length,
                    InetAddress.getByName("localhost"), EDGE_DEST_PORT
                );
                socket.send(forwardPacket);

                System.out.println("✅ FW1: Tráfego permitido de " + sourceIp);
            } else {
                // BLOQUEADO: Drop packet
                System.err.println("⛔ FW1: BLOQUEIO! IP Desconhecido tentou acesso: " + sourceIp);
            }

        } catch (IOException e) {
            System.err.println("❌ FW1: Pacote malformado descartado.");
        }
    }
}
