package sa.factory.warshanowservices1.engine

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

enum class MadaCardScheme(val aid: String, val schemeName: String) {
    MADA("A000000228", "مدى (Mada Contactless)"),
    VISA_MADA("A000000003", "فيزا مدى (Visa Mada)"),
    MC_MADA("A000000004", "ماستركارد مدى (Mastercard Mada)")
}

enum class EmvTransactionStatus {
    APPROVED_OFFLINE,
    APPROVED_ONLINE,
    DECLINED_OFFLINE,
    DECLINED_PIN_REQUIRED,
    DECLINED_EXCEEDED_LIMIT
}

data class EmvTlvTag(val tag: String, val length: Int, val valueHex: String)

data class MadaEmvTransactionRecord(
    val transactionId: String,
    val stan: Long,
    val scheme: MadaCardScheme,
    val maskedPan: String,
    val amountSar: Double,
    val arqcHex: String,
    val status: EmvTransactionStatus,
    val approvalCode: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MadaContactlessEmvL2ProtocolEngine {

    companion object {
        const val MADA_NO_PIN_LIMIT_SAR = 300.0
        const val MAX_OFFLINE_TXN_LIMIT_SAR = 1000.0
    }

    private var stanCounter: Long = 100001
    private val transactionLedger = ConcurrentHashMap<String, MadaEmvTransactionRecord>()

    fun parseEmvTlv(tlvStreamHex: String): List<EmvTlvTag> {
        val clean = tlvStreamHex.replace(" ", "").uppercase()
        val tags = mutableListOf<EmvTlvTag>()
        var idx = 0

        while (idx < clean.length - 3) {
            val tag = clean.substring(idx, idx + 2)
            idx += 2
            val lenHex = clean.substring(idx, idx + 2)
            idx += 2
            val len = lenHex.toIntOrNull(16) ?: break
            val valHexEnd = (idx + (len * 2)).coerceAtMost(clean.length)
            val valHex = clean.substring(idx, valHexEnd)
            idx = valHexEnd
            tags.add(EmvTlvTag(tag, len, valHex))
        }

        return tags
    }

    fun computeArqc(pan: String, amountSar: Double, stan: Long): String {
        val raw = "MADA_EMV_L2|$pan|${String.format("%.2f", amountSar)}|$stan|KEY_AES256"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(raw.toByteArray(StandardCharsets.UTF_8))
        return hash.take(8).joinToString("") { "%02X".format(it) }
    }

    fun maskPan(pan: String): String {
        val clean = pan.replace(" ", "")
        if (clean.length < 10) return "XXXX-XXXX-XXXX"
        val first6 = clean.take(6)
        val last4 = clean.takeLast(4)
        return "$first6******$last4"
    }

    fun processContactlessTransaction(
        pan: String,
        amountSar: Double,
        scheme: MadaCardScheme = MadaCardScheme.MADA,
        pinEntered: Boolean = false
    ): MadaEmvTransactionRecord {
        stanCounter++
        val stan = stanCounter
        val masked = maskPan(pan)
        val arqc = computeArqc(pan, amountSar, stan)

        val status = when {
            amountSar > MAX_OFFLINE_TXN_LIMIT_SAR -> EmvTransactionStatus.DECLINED_EXCEEDED_LIMIT
            amountSar > MADA_NO_PIN_LIMIT_SAR && !pinEntered -> EmvTransactionStatus.DECLINED_PIN_REQUIRED
            amountSar <= MADA_NO_PIN_LIMIT_SAR -> EmvTransactionStatus.APPROVED_OFFLINE
            else -> EmvTransactionStatus.APPROVED_ONLINE
        }

        val approvalCode = if (status == EmvTransactionStatus.APPROVED_OFFLINE || status == EmvTransactionStatus.APPROVED_ONLINE) {
            "MADA-${100000 + (stan % 900000)}"
        } else {
            "REJECT-${status.name.take(6)}"
        }

        val record = MadaEmvTransactionRecord(
            transactionId = "TXN-MADA-$stan",
            stan = stan,
            scheme = scheme,
            maskedPan = masked,
            amountSar = amountSar,
            arqcHex = arqc,
            status = status,
            approvalCode = approvalCode
        )

        transactionLedger[record.transactionId] = record
        return record
    }

    fun getTransaction(txnId: String): MadaEmvTransactionRecord? = transactionLedger[txnId]

    fun getTotalProcessedVolumeSar(): Double = transactionLedger.values
        .filter { it.status == EmvTransactionStatus.APPROVED_OFFLINE || it.status == EmvTransactionStatus.APPROVED_ONLINE }
        .sumOf { it.amountSar }

    fun getApprovedTransactionCount(): Int = transactionLedger.values
        .count { it.status == EmvTransactionStatus.APPROVED_OFFLINE || it.status == EmvTransactionStatus.APPROVED_ONLINE }
}
