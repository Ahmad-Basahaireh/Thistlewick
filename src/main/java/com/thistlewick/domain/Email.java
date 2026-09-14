package com.thistlewick.domain;

import com.thistlewick.exception.InvalidEmailException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * {@code Email} — an immutable Value Object representing a validated
 * email address.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Value Object:</b> two {@code Email}s are equal iff their
 *       normalized values are equal. There is no identity (no id field).</li>
 *   <li><b>Self-validating:</b> every instance is guaranteed valid by
 *       the constructor. There is no way to hold an invalid {@code Email}.</li>
 *   <li><b>Normalized:</b> inputs are trimmed and lower-cased before
 *       validation. This makes equality robust against
 *       {@code "  User@Example.COM  "} vs {@code "user@example.com"}.</li>
 *   <li><b>Regex hand-written:</b> no external validation libraries.</li>
 * </ul>
 *
 * <h2>Regex rationale</h2>
 * <p>The pattern is stricter than the minimal {@code .+@.+} check but
 * intentionally simpler than full RFC 5322. It enforces:</p>
 * <ul>
 *   <li>Local part: letters, digits, and {@code . _ % + -} (RFC 5322 atext subset).</li>
 *   <li>Exactly one {@code @}.</li>
 *   <li>Domain: letters, digits, and {@code . -}.</li>
 *   <li>TLD: at least two letters ({@code com}, {@code io}, {@code dev}, ...).</li>
 * </ul>
 *
 * <p>This is a deliberate trade-off: it rejects technically-valid but
 * exotic addresses (e.g. quoted local parts) in exchange for simplicity
 * and predictable behavior. For a task manager this is the right call —
 * we prefer to be strict on write.</p>
 */
//It cannot be inherited; this prevents manipulation.
public final class Email {

    /**
     * Regex breakdown:
     * <pre>
     *   ^                          start of string
     *   [A-Za-z0-9._%+-]+          local part: 1+ allowed chars
     *   @                          exactly one at-sign
     *   [A-Za-z0-9.-]+             domain: 1+ allowed chars
     *   \.                         a literal dot before TLD
     *   [A-Za-z]{2,}               TLD: at least 2 letters
     *   $                          end of string
     * </pre>
     * The pattern is compiled once (static final) — {@link Pattern} is
     * thread-safe and reusing it avoids recompilation per validation.
     */
    // تعريف نمط الـ Regex المكون من شكل الإيميل (يحتوي على @ ودومين مثل .com)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /** RFC 5321 hard limit for an email address. */
    private static final int MAX_LENGTH = 254;
    // المتغير الذي يخزن قيمة الإيميل النصية النص الحقيقي
    private final String value;

    /**
     * Creates a validated, normalized {@code Email}.
     *
     * @param raw the raw input; may be null → rejected
     * @throws InvalidEmailException if null, blank, too long, or malformed
     */
    // الـ Constructor
    public Email(String raw) {
        if (raw == null) {
            throw new InvalidEmailException("Email must not be null");
        }
        //trim(): إزالة المسافات من البداية والنهاية.
        String normalized = raw.trim().toLowerCase();

        if (normalized.isEmpty()) {
            throw new InvalidEmailException("Email must not be blank", raw);
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidEmailException(
                    "Email exceeds " + MAX_LENGTH + " characters", raw);
        }
        // رفع خطأ إذا كان الشكل غير صحيح
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailException(
                    "Email does not match the expected format: '" + raw + "'", raw);
        }
        //التخزين: إذا نجح كل شي، نخزن القيمة في value.
        this.value = normalized;
    }

    /** @return the normalized (trimmed, lower-cased) email string.(getter) */
    public String value() {
        return value;
    }

    /**
     * @return the local part (before  @).ex(ahmad.bassam2001)
     * Useful for greetings, logging, or building display names.
     */
    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    /** @return the domain (after @).ex(gmail.com) */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email other)) return false;
        return value.equals(other.value);
    }

    //ترجع hashCode للكائن.
    //ليش مهمة؟ لكي تقدر تستخدم البريد الإلكتروني كمفتاح في HashMap
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

}