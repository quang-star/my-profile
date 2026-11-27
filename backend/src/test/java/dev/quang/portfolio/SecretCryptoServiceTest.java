package dev.quang.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quang.portfolio.security.SecretCryptoService;
import org.junit.jupiter.api.Test;

class SecretCryptoServiceTest {

    @Test
    void encryptAndDecryptRoundTrip() {
        SecretCryptoService service = new SecretCryptoService("my-strong-encryption-key-2026", "jwt-fallback");
        String plain = "sk-test-secret-api-key-987654321";

        String encrypted = service.encrypt(plain);

        assertTrue(service.isEncrypted(encrypted));
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertFalse(encrypted.contains(plain));
        assertEquals(plain, service.decrypt(encrypted));
    }

    @Test
    void encryptUsesRandomIvEachTime() {
        SecretCryptoService service = new SecretCryptoService("my-strong-encryption-key-2026", "");
        String plain = "sk-same-key-twice";

        String first = service.encrypt(plain);
        String second = service.encrypt(plain);

        assertNotEquals(first, second);
        assertEquals(plain, service.decrypt(first));
        assertEquals(plain, service.decrypt(second));
    }

    @Test
    void decryptSupportsLegacyPlaintext() {
        SecretCryptoService service = new SecretCryptoService("my-strong-encryption-key-2026", "");
        String legacy = "sk-legacy-plain";

        assertFalse(service.isEncrypted(legacy));
        assertEquals(legacy, service.decrypt(legacy));
    }

    @Test
    void wrongKeyOrTamperedCiphertextReturnsEmpty() {
        SecretCryptoService keyA = new SecretCryptoService("key-alpha-1234567890", "");
        SecretCryptoService keyB = new SecretCryptoService("key-beta-0987654321", "");

        String encrypted = keyA.encrypt("sk-secret-value");

        assertEquals("", keyB.decrypt(encrypted));
        assertEquals("", keyA.decrypt("enc:v1:invalid-base64!!!"));
        assertEquals("", keyA.decrypt("enc:v1:dG9vLXNob3J0"));
    }

    @Test
    void fallsBackToJwtSecretWhenEncryptionKeyBlank() {
        SecretCryptoService fallbackA = new SecretCryptoService("", "shared-jwt-secret-minimum-32-characters");
        SecretCryptoService fallbackB = new SecretCryptoService("   ", "shared-jwt-secret-minimum-32-characters");
        SecretCryptoService explicit = new SecretCryptoService("dedicated-aes-key-value", "shared-jwt-secret-minimum-32-characters");

        String encrypted = fallbackA.encrypt("sk-fallback-key");

        assertEquals("sk-fallback-key", fallbackB.decrypt(encrypted));
        assertEquals("", explicit.decrypt(encrypted));
    }

    @Test
    void handlesBlankAndNullInputs() {
        SecretCryptoService service = new SecretCryptoService("my-strong-encryption-key-2026", "");

        assertFalse(service.isEncrypted(null));
        assertFalse(service.isEncrypted("plain"));
        assertEquals("", service.encrypt(null));
        assertEquals("", service.encrypt("   "));
        assertEquals("", service.decrypt(null));
        assertEquals("", service.decrypt("   "));
    }
}
