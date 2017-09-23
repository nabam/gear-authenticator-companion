package net.nabam.otp.util

import android.net.Uri

const val OTP_SCHEME = "otpauth"
const val SECRET_PARAM = "secret"
const val COUNTER_PARAM = "counter"
const val TOTP = "totp"
const val HOTP = "hotp"

const val DEFAULT_HOTP_COUNTER = 0

enum class OtpType constructor(val value: Int) {
    TOTP(0),
    HOTP(1);
}

const val INVALID_URI = "Invalid URI"
const val INVALID_SECRET = "Invalid secret key"

data class OtpInfo (
        val type: OtpType,
        val user: String,
        val counter: Int,
        val secret: String
)

private fun validateAndGetUserInPath(path: String?): String? {
    if (path == null || !path.startsWith("/")) {
        return null
    }

    val user = path.substring(1).trim(' ')
    return if (user.length == 0) null else user
}

@Throws(RuntimeException::class)
fun parseOtpUri(uri: Uri): OtpInfo {
    val scheme = uri.getScheme().toLowerCase()
    val path = uri.getPath()
    val authority = uri.getAuthority()
    val user: String?
    val secret: String?
    val type: OtpType
    val counter: Int?

    if (!OTP_SCHEME.equals(scheme)) {
        throw RuntimeException(INVALID_URI)
    }

    if (TOTP == authority) {
        type = OtpType.TOTP
        counter = DEFAULT_HOTP_COUNTER
    } else if (HOTP == authority) {
        type = OtpType.HOTP
        val counterParameter = uri.getQueryParameter(COUNTER_PARAM)
        if (counterParameter != null) {
            try {
                counter = Integer.parseInt(counterParameter)
            } catch (e: NumberFormatException) {
                throw RuntimeException(INVALID_URI)
            }

        } else {
            counter = DEFAULT_HOTP_COUNTER
        }
    } else {
        throw RuntimeException(INVALID_URI)
    }

    user = validateAndGetUserInPath(path)
    if (user == null) {
        throw RuntimeException(INVALID_URI)
    }

    secret = uri.getQueryParameter(SECRET_PARAM)

    if (secret.isNullOrEmpty()) {
        throw RuntimeException(INVALID_SECRET)
    }

    return OtpInfo(type, user, counter, secret)
}