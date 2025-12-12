package domain.device;

import core.SensorData;

public class MaliciousDevice extends Device {

    public MaliciousDevice(String id, String token) {
        super(id, token);
    }

    @Override
    protected SensorData generateData() {
        SensorData normal = super.generateData();

        return new SensorData(
            normal.getDeviceId(),
            normal.getCo2(),
            normal.getCo(),
            normal.getNo2(),
            normal.getSo2(),
            normal.getPm25(),
            normal.getPm10(),
            normal.getHumidity(),
            500.0, // TEMPERATURA 500 GRAUS (Anomalia)
            normal.getNoiseLevel(),
            normal.getUvIndex()
        );
    }
}
