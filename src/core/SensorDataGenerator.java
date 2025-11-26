package core;

import java.util.concurrent.ThreadLocalRandom;

public class SensorDataGenerator {

    public static SensorData generate(String deviceId) {
        // Faixas de Valores Reais (Valores de exemplo, baseados em padrões de qualidade do ar)

        // CO2: 300 (Rural) a 1000 (Ambiente Interno/Urbano) ppm
        double co2 = randomRange(350, 800);

        // CO: 0.1 a 50 ppm (geralmente abaixo de 9 ppm)
        double co = randomRange(0.5, 9.0);

        // NO2: 0 a 100 ppb (partes por bilhão)
        double no2 = randomRange(10, 80);

        // SO2: 0 a 50 ppb
        double so2 = randomRange(5, 40);

        // PM2.5: 0 a 35 (boa qualidade) µg/m³
        double pm25 = randomRange(5.0, 30.0);

        // PM10: 0 a 50 (boa qualidade) µg/m³
        double pm10 = randomRange(10.0, 45.0);

        // Umidade: 30% a 90%
        double humidity = randomRange(35.0, 85.0);

        // Temperatura: 15°C a 35°C
        double temperature = randomRange(20.0, 32.0);

        // Ruído Urbano (Decibéis): 40 (calmo) a 90 (tráfego intenso)
        double noiseLevel = randomRange(50.0, 80.0);

        // Radiação UV (Índice): 0 a 11+
        double uvIndex = randomRange(2.0, 8.0);

        return new SensorData(
            deviceId, co2, co, no2, so2, pm25, pm10,
            humidity, temperature, noiseLevel, uvIndex
        );
    }

    private static double randomRange(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}