package utils;

import core.SensorData;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.Key;

public class EncryptMessageUtil {
    public static byte[] encryptedHybridMessage(SensorData data, Key publicKey) throws Exception {
        byte[] payloadBytes;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(data);
            payloadBytes = bos.toByteArray();
        }

        SecretKey sessionKey = AESUtil.generateKey();

        byte[] encryptedPayload = AESUtil.encrypt(payloadBytes, sessionKey);
        byte[] encryptedAesKeyBytes = RSAUtil.encrypt(sessionKey.getEncoded(), publicKey);

        if (encryptedAesKeyBytes.length != 256)
            throw new IllegalArgumentException("Erro criptografia RSA: Tamanho inesperado " + encryptedAesKeyBytes.length);

        ByteArrayOutputStream finalMessageStream = new ByteArrayOutputStream();
        finalMessageStream.write(encryptedAesKeyBytes); // Escreve exatamente 256 bytes
        finalMessageStream.write(encryptedPayload);     // Escreve o resto (dados)

        return finalMessageStream.toByteArray();
    }
}
