package dk.schulz.quiettype.accessibility

object FocusedFieldSensitivity {
    private val sensitiveTerms = listOf(
        "password",
        "passwd",
        "passcode",
        "pin",
        "otp",
        "one-time",
        "one time",
        "2fa",
        "mfa",
        "totp",
        "verification code",
        "security code",
        "card number",
        "credit card",
        "debit card",
        "cvv",
        "cvc",
        "expiry",
        "expiration",
        "ssn",
        "social security",
        "bank account",
        "routing number",
    )

    fun isSensitive(snapshot: FocusedFieldSnapshot): Boolean {
        if (snapshot.isPassword) return true
        val metadata = listOfNotNull(
            snapshot.className,
            snapshot.viewIdResourceName,
            snapshot.hintText,
        ).joinToString(separator = " ").lowercase()
        return sensitiveTerms.any { term -> metadata.contains(term) }
    }
}
