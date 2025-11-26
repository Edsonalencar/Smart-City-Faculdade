package core;

import java.io.Serializable;
import java.util.Date;

public class SensorData implements Serializable {

    // Identificação do Dispositivo
    private String deviceId;
    private Date timestamp;

    // Dados Ambientais Requeridos
    private double co2; // Dióxido de Carbono (ppm)
    private double co; // Monóxido de Carbono (ppm)
    private double no2; // Dióxido de Nitrogênio (ppb)
    private double so2; // Dióxido de Enxofre (ppb)
    private double pm25; // Partículas (µg/m³)
    private double pm10; // Partículas (µg/m³)
    private double humidity; // Umidade (%)
    private double temperature; // Temperatura (°C)
    private double noiseLevel; // Ruído Urbano (decibéis)
    private double uvIndex; // Radiação UV (escala)

    public SensorData(
          String deviceId, double co2, double co, double no2, double so2,
          double pm25, double pm10, double humidity, double temperature,
          double noiseLevel, double uvIndex
    ) {
        this.deviceId = deviceId;
        this.co2 = co2;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.humidity = humidity;
        this.temperature = temperature;
        this.noiseLevel = noiseLevel;
        this.uvIndex = uvIndex;
        this.timestamp = new Date(); // Marca de tempo de cada dado
    }

    public String getDeviceId() { return deviceId; }
    public Date getTimestamp() { return timestamp; }
    public double getCo2() { return co2; }
    public double getCo() { return co; }
    public double getNo2() { return no2; }
    public double getSo2() { return so2; }
    public double getPm25() { return pm25; }
    public double getPm10() { return pm10; }
    public double getHumidity() { return humidity; }
    public double getTemperature() { return temperature; }
    public double getNoiseLevel() { return noiseLevel; }
    public double getUvIndex() { return uvIndex; }

    @Override
    public String toString() {
        return String.format(
            "[%s] Device %s: CO2=%.0f ppm, Temp=%.1f °C, PM2.5=%.1f µg/m³, Ruído=%.1f dB",
            new java.text.SimpleDateFormat("HH:mm:ss").format(timestamp),
            deviceId, co2, temperature, pm25, noiseLevel
        );
    }
}
