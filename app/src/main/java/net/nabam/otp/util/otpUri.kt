package net.nabam.otp.util

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.lang.RuntimeException

const val OTP_SCHEME = "otpauth"
const val SECRET_PARAM = "secret"
const val COUNTER_PARAM = "counter"
const val TOTP = "totp"
const val HOTP = "hotp"

const val DEFAULT_HOTP_COUNTER = 0

class OtpUriParseException(message: String) : RuntimeException(message)

enum class OtpType constructor(val value: Int) {
    TOTP(0),
    HOTP(1);
}

const val INVALID_URI = "Invalid URI"
const val INVALID_SECRET = "Invalid secret key"

data class OtpInfo (
        val type: OtpType,
        val label: String,
        val counter: Int,
        val secret: String,
        var alias: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
            OtpType.valueOf(parcel.readString()),
            parcel.readString(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
        parcel.writeString(label)
        parcel.writeInt(counter)
        parcel.writeString(secret)
        parcel.writeString(alias)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OtpInfo> {
        override fun createFromParcel(parcel: Parcel): OtpInfo {
            return OtpInfo(parcel)
        }

        override fun newArray(size: Int): Array<OtpInfo?> {
            return arrayOfNulls(size)
        }
    }
}

private fun validateAndGetUserInPath(path: String?): String? {
    if (path == null || !path.startsWith("/")) {
        return null
    }

    val user = path.substring(1).trim(' ')
    return if (user.length == 0) null else user
}

@Throws(OtpUriParseException::class)
fun parseOtpUri(uri: Uri): OtpInfo {
    val scheme = uri.getScheme().toLowerCase()
    val path = uri.getPath()
    val authority = uri.getAuthority()
    val user: String?
    val secret: String?
    val type: OtpType
    val counter: Int?

    if (!OTP_SCHEME.equals(scheme)) {
        throw OtpUriParseException(INVALID_URI)
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
                throw OtpUriParseException(INVALID_URI)
            }

        } else {
            counter = DEFAULT_HOTP_COUNTER
        }
    } else {
        throw OtpUriParseException(INVALID_URI)
    }

    user = validateAndGetUserInPath(path)
    if (user == null) {
        throw OtpUriParseException(INVALID_URI)
    }

    secret = uri.getQueryParameter(SECRET_PARAM)

    if (secret.isNullOrEmpty()) {
        throw OtpUriParseException(INVALID_SECRET)
    }

    return OtpInfo(type, user, counter, secret, "")
}

