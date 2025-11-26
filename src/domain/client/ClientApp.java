package domain.client;

import infrastructure.auth.AuthService;
import infrastructure.cloud.DatacenterService;

public class ClientApp {

    private String clientId;
    private String token;

    public ClientApp(String clientId, String token) {
        this.clientId = clientId;
        this.token = token;
    }

    public void start() {
        System.out.println("\n💻 CLIENTE " + clientId + " INICIANDO...");

        if (!AuthService.authenticate(clientId, token)) {
            System.err.println("⛔ Acesso negado: Credenciais do cliente inválidas.");
            return;
        }

        System.out.println("✅ Cliente autenticado. Conectando ao Datacenter...");

        // 2. Simulação de Consulta ao Datacenter
        // Como o Datacenter está no mesmo processo Java nesta simulação, podemos acessar métodos estáticos
        // OU fazer uma conexão TCP separada para pedir o relatório.
        // Para ficar mais robusto e "simulado", vamos acessar diretamente os métodos estáticos do Datacenter
        // fingindo que foi uma chamada de API (visto que já testamos TCP extensivamente na Borda).

        try {
            // Simulando latência de rede
            Thread.sleep(1000);

            System.out.println("\n--- 🔎 CONSULTA 1: Monitoramento de Poluição ---");
            String report1 = DatacenterService.generatePollutionReport();
            System.out.println(report1);

            Thread.sleep(1500);

            System.out.println("\n--- 🔎 CONSULTA 2: Alertas de Segurança Urbana ---");
            String alert = DatacenterService.checkNoiseAlerts();
            System.out.println(alert);

            String prediction = DatacenterService.predictTemperatureTrend();
            System.out.println(prediction);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}