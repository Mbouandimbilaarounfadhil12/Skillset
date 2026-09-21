package com.skillset.infrastructure.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Converter(autoApply = false)
@Slf4j
public class EncryptionConverter implements AttributeConverter<String, String> {
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES";
    private static final byte[] KEY = initializeKey();

    private static byte[] initializeKey() {
        String encryptionKey = System.getenv("ENCRYPTION_KEY");
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            encryptionKey = System.getProperty("encryption.key", "default-32-char-key-change-this!");
        }
        byte[] keyBytes = new byte[32];
        byte[] sourceBytes = encryptionKey.getBytes();
        System.arraycopy(sourceBytes, 0, keyBytes, 0, Math.min(sourceBytes.length, 32));
        return keyBytes;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            SecretKey secretKey = new SecretKeySpec(KEY, 0, KEY.length, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error encrypting value", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            SecretKey secretKey = new SecretKeySpec(KEY, 0, KEY.length, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(dbData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Error decrypting value", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
