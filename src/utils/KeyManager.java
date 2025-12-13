package utils;

import java.security.*;
import java.io.*;

public class KeyManager {

    // Salva uma chave em um arquivo.
    public static void saveKeyToFile(Key key, String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(key);
        }
    }

    //Carrega uma chave de um arquivo.
    public static Key loadKeyFromFile(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return (Key) ois.readObject();
        }
    }

    public static void generateAndSaveAllKeys() {
        try {
            System.out.println("--- GERANDO CHAVES RSA PARA O SISTEMA ---");
            // Borda
            KeyPair edgeKeys = RSAUtil.generateKeyPair();
            saveKeyToFile(edgeKeys.getPublic(), "edge_public.key");
            saveKeyToFile(edgeKeys.getPrivate(), "edge_private.key");
            System.out.println("Chaves RSA da Borda geradas: edge_public.key e edge_private.key");

            // Datacenter
            KeyPair dcKeys = RSAUtil.generateKeyPair();
            saveKeyToFile(dcKeys.getPublic(), "dc_public.key");
            saveKeyToFile(dcKeys.getPrivate(), "dc_private.key");
            System.out.println("Chaves RSA do Datacenter geradas: dc_public.key e dc_private.key");

            // Dispositivo (Modelo)
            KeyPair deviceKeys = RSAUtil.generateKeyPair();
            saveKeyToFile(deviceKeys.getPublic(), "device_public.key");
            saveKeyToFile(deviceKeys.getPrivate(), "device_private.key");
            System.out.println("Chaves RSA do Dispositivo (modelo) geradas: device_public.key e device_private.key");

        } catch (Exception e) {
            System.err.println("Erro na geração/salvamento de chaves: " + e.getLocalizedMessage());
        }
    }
}