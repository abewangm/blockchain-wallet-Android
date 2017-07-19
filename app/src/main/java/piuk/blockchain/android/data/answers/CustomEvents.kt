package piuk.blockchain.android.data.answers

import com.crashlytics.android.answers.CustomEvent

class RecoverWalletEvent : CustomEvent("Recover Wallet") {

    fun putSuccess(successful: Boolean): RecoverWalletEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }

}

class PairingEvent : CustomEvent("Wallet Pairing") {

    fun putSuccess(successful: Boolean): PairingEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }

    fun putMethod(pairingMethod: PairingMethod): PairingEvent {
        putCustomAttribute("Pairing method", pairingMethod.name)
        return this
    }

}

@Suppress("UNUSED_PARAMETER")
enum class PairingMethod(name: String) {
    MANUAL("Manual"),
    QR_CODE("Qr code")
}