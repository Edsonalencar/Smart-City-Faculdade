package domain.client;

import infrastructure.cloud.DatacenterService;

public class ClientApp {

    private String clientId;
    private String token;

    public ClientApp(String clientId, String token) {
        this.clientId = clientId;
        this.token = token;
    }

    public void start() {
        System.out.println("___________________________________________________________");
        System.out.println("💻 CLIENTE " + clientId + " INICIANDO SESSÃO HTTP...");

        // Simulação de delay de rede
        try { Thread.sleep(1000); } catch (Exception e) {}

        // Executa as 5 Consultas Requisitadas
        performSimulatedRequest("GET", "/api/reports/pollution");
        performSimulatedRequest("GET", "/api/alerts/safety");
        performSimulatedRequest("GET", "/api/health/recommendations");
        performSimulatedRequest("GET", "/api/maintenance/status");
        performSimulatedRequest("GET", "/api/forecast/trends");

        // Teste de Erro (404)
        performSimulatedRequest("GET", "/api/invalid/endpoint");
    }

    /**
     * Simula o envio de uma requisição HTTP via socket e a impressão da resposta.
     */
    private void performSimulatedRequest(String method, String url) {
        System.out.println("\n-----------------------------------------------------------");
        System.out.println("📤 ENVIANDO REQUISIÇÃO:");
        System.out.println(method + " " + url + " HTTP/1.1");
        System.out.println("Host: datacenter.smartcity.br");
        System.out.println("Authorization: Bearer " + "*****"); // Oculta o token no log visual
        System.out.println("-----------------------------------------------------------");

        // Aqui a mágica acontece: chamamos o "Router" do Datacenter simulando a rede
        // Na prática real, isso seria: socket.getOutputStream().write(...)
        String rawResponse = DatacenterService.processHttpRequest(method, url, this.token);

        System.out.println("📥 RESPOSTA RECEBIDA:");
        System.out.println(rawResponse);

        try { Thread.sleep(1500); } catch (Exception e) {} // Pausa para leitura
    }
}