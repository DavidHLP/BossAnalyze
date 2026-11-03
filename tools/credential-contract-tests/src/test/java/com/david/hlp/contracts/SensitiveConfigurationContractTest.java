package com.david.hlp.contracts;

import com.david.hlp.web.common.security.CredentialConfiguration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SensitiveConfigurationContractTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={" ","\t\n","${MISSING}","your_example"," YOUR_example ","synthetic-invalid*"})
    void invalidJwtFailsWithoutValueOrCause(String value) {
        var error=assertThrows(IllegalStateException.class,()->CredentialConfiguration.requireJwtSecret(value));
        assertEquals("JWT configuration requires an externally supplied Base64 key of at least 32 bytes",error.getMessage());
        assertNull(error.getCause());
    }
    @ParameterizedTest
    @ValueSource(ints={0,1,16,31})
    void shortJwtFails(int bytes) {
        assertThrows(IllegalStateException.class,()->CredentialConfiguration.requireJwtSecret(Base64.getEncoder().encodeToString(new byte[bytes])));
    }
    @ParameterizedTest
    @ValueSource(ints={32,48,64})
    void suppliedJwtAcceptedRepeatedly(int bytes) {
        String key=Base64.getEncoder().encodeToString(new byte[bytes]);
        assertDoesNotThrow(()->CredentialConfiguration.requireJwtSecret(key));
        assertDoesNotThrow(()->CredentialConfiguration.requireJwtSecret(key));
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={" ","\t\n","${MISSING}","your_example"," YOUR_example "})
    void smtpMissingConfigurationFails(String value) {
        for (boolean missingPassword:new boolean[]{true,false}) {
            var error=assertThrows(IllegalStateException.class,()->CredentialConfiguration.requireSmtp(
                missingPassword?"synthetic@example.invalid":value,missingPassword?value:"synthetic-only"));
            assertEquals("SMTP configuration is required; configure spring.mail.username and spring.mail.password externally",error.getMessage());
            assertNull(error.getCause());
        }
    }
    @Test void smtpSuppliedAccepted() {
        assertDoesNotThrow(()->CredentialConfiguration.requireSmtp("synthetic@example.invalid","synthetic-only"));
    }
}
