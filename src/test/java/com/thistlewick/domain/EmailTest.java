package com.thistlewick.domain;

import com.thistlewick.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Email} Value Object.
 *
 * <p>Uses JUnit 5 with nested test classes to group positive / negative
 * / normalization cases.</p>
 */
class EmailTest {

    @Nested
    @DisplayName("Valid emails")
    class ValidEmails {

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "first.last@example.com",
                "user+tag@example.co.uk",
                "user_name@example.io",
                "u@a.io",
                "a1b2c3@sub.domain.org",
                "USER@EXAMPLE.COM"
        })
        @DisplayName("accepts common valid formats")
        void acceptsValidFormats(String raw) {
            assertDoesNotThrow(() -> new Email(raw));
        }

        @Test
        @DisplayName("exposes local and domain parts")
        void exposesParts() {
            Email email = new Email("first.last@example.com");
            assertEquals("first.last", email.localPart());
            assertEquals("example.com", email.domain());
        }
    }

    @Nested
    @DisplayName("Invalid emails")
    class InvalidEmails {

        @ParameterizedTest
        @ValueSource(strings = {
                "plainaddress",           // no @
                "@example.com",           // no local part
                "user@",                  // no domain
                "user@.com",              // domain starts with dot
                "user@example",           // no TLD
                "user@example.c",         // TLD too short
                "user@@example.com",      // double @
                "user name@example.com",  // space
                "user@exam ple.com",      // space in domain
                "user@exam!ple.com"       // invalid char in domain
        })
        @DisplayName("rejects malformed formats")
        void rejectsMalformed(String raw) {
            assertThrows(InvalidEmailException.class, () -> new Email(raw));
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            InvalidEmailException ex =
                    assertThrows(InvalidEmailException.class, () -> new Email(null));
            assertNull(ex.getRejectedValue());
        }

        @Test
        @DisplayName("rejects blank")
        void rejectsBlank() {
            InvalidEmailException ex =
                    assertThrows(InvalidEmailException.class, () -> new Email("   "));
            assertEquals("   ", ex.getRejectedValue());
        }

        @Test
        @DisplayName("rejects emails longer than 254 chars")
        void rejectsTooLong() {
            String local = "a".repeat(250);
            String tooLong = local + "@example.com";
            assertThrows(InvalidEmailException.class, () -> new Email(tooLong));
        }
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("trims whitespace")
        void trimsWhitespace() {
            Email email = new Email("  user@example.com  ");
            assertEquals("user@example.com", email.value());
        }

        @Test
        @DisplayName("lower-cases the value")
        void lowerCases() {
            Email email = new Email("USER@EXAMPLE.COM");
            assertEquals("user@example.com", email.value());
        }

        @Test
        @DisplayName("treats differently-cased inputs as equal")
        void caseInsensitiveEquality() {
            Email a = new Email("User@Example.COM");
            Email b = new Email("user@example.com");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("treats whitespace-padded inputs as equal")
        void whitespaceInsensitiveEquality() {
            Email a = new Email("  user@example.com  ");
            Email b = new Email("user@example.com");
            assertEquals(a, b);
        }
    }
}