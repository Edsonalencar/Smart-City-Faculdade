package infrastructure.firewall;

import java.net.*;

public class PacketFilter {
    private static final int EXTERNAL_PORT = 9800; // Porta exposta aos dispositivos
    private static final int EDGE_DEST_PORT = 9876; // Porta real da Borda (DMZ)

    public void start() {
        new Thread(() -> {
            try (DatagramSocket proxySocket = new DatagramSocket(EXTERNAL_PORT)) {
                System.out.println("🧱 Firewall (Packet Filter): Filtrando na porta " + EXTERNAL_PORT + "...");

                byte[] buffer = new byte[65535];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    proxySocket.receive(packet);

                    // Log da conexão
                    System.out.println("🧱 FW1: Pacote de " + packet.getAddress() + ":" + packet.getPort());

                    // Encaminhamento para a Borda na DMZ
                    DatagramPacket forwardPacket = new DatagramPacket(
                        packet.getData(), packet.getLength(),
                        InetAddress.getByName("localhost"), EDGE_DEST_PORT
                    );
                    proxySocket.send(forwardPacket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
