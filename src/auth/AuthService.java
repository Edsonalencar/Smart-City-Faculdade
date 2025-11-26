package auth;

import java.util.HashMap;
import java.util.Map;

public class AuthService {

    // Simulação de banco de dados de dispositivos autorizados
    private static final Map<String, String> authorizedDevices = new HashMap<>();

    static {
        authorizedDevices.put("DEV_01_POSTE", "secret_token_01");
        authorizedDevices.put("DEV_02_SEMAFORO", "secret_token_02");
        authorizedDevices.put("DEV_03_TOTEM", "secret_token_03");
        authorizedDevices.put("DEV_04_CAIXA", "secret_token_04");
    }

    public static boolean authenticate(String deviceId, String token) {
        if (authorizedDevices.containsKey(deviceId)) {
            String expectedToken = authorizedDevices.get(deviceId);
            return expectedToken.equals(token);
        }
        return false;
    }
}