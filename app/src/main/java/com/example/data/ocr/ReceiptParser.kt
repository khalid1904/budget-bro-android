package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ReceiptScanResult(
    val amount: Double?,
    val merchant: String?,
    val suggestedCategoryName: String?,
    val paymentMethod: String?,
    val referenceId: String?,
    val transactionDate: Long?,
    val notes: String?,
    val rawText: String? = null,
    val source: String = "ON_DEVICE_OCR" // "GEMINI_AI" or "ON_DEVICE_OCR"
)

class ReceiptParser(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ReceiptParser"
    }

    /**
     * Parses a UPI receipt image. Attempts Gemini AI Vision first if a valid API key is present
     * and device is online; seamlessly falls back to high-accuracy on-device ML Kit OCR parser.
     */
    suspend fun parseReceiptImage(imageUri: Uri): ReceiptScanResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            null
        }

        val hasValidGeminiKey = !apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidGeminiKey) {
            try {
                val geminiResult = parseWithGeminiVision(imageUri, apiKey!!)
                if (geminiResult != null && geminiResult.amount != null && geminiResult.amount > 0) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Vision attempt failed, falling back to ML Kit OCR", e)
            }
        }

        // On-Device ML Kit OCR fallback (works 100% offline, zero latency)
        return@withContext parseWithMLKit(imageUri)
    }

    /**
     * On-Device text recognition with Google ML Kit.
     */
    private suspend fun parseWithMLKit(imageUri: Uri): ReceiptScanResult = withContext(Dispatchers.IO) {
        val inputImage = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating InputImage from URI", e)
            return@withContext ReceiptScanResult(
                amount = null,
                merchant = null,
                suggestedCategoryName = null,
                paymentMethod = "UPI",
                referenceId = null,
                transactionDate = System.currentTimeMillis(),
                notes = "Failed to load receipt image"
            )
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val fullText = suspendCancellableCoroutine<String> { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { exc ->
                    Log.e(TAG, "ML Kit OCR failed", exc)
                    continuation.resume("")
                }
        }

        return@withContext extractUPIFields(fullText)
    }

    /**
     * Intelligent heuristic and regex parser tailored for Indian UPI apps:
     * Google Pay, PhonePe, Paytm, CRED, BHIM, Amazon Pay.
     */
    fun extractUPIFields(rawText: String): ReceiptScanResult {
        if (rawText.isBlank()) {
            return ReceiptScanResult(
                amount = null,
                merchant = null,
                suggestedCategoryName = "Food & Dining",
                paymentMethod = "UPI",
                referenceId = null,
                transactionDate = System.currentTimeMillis(),
                notes = null,
                rawText = rawText,
                source = "ON_DEVICE_OCR"
            )
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Detect Payment Method App
        val paymentMethod = detectPaymentApp(rawText)

        // 2. Extract Amount
        val amount = extractAmount(rawText, lines)

        // 3. Extract Merchant / Payee
        val merchant = extractMerchant(rawText, lines)

        // 4. Extract Reference ID / UTR
        val refId = extractRefId(rawText)

        // 5. Suggest Category based on merchant name and keywords
        val categoryName = suggestCategory(merchant, rawText)

        // 6. Build contextual notes
        val notesBuilder = StringBuilder()
        if (!refId.isNullOrBlank()) {
            notesBuilder.append("UPI Ref: $refId")
        }
        val upiId = extractUpiId(rawText)
        if (!upiId.isNullOrBlank()) {
            if (notesBuilder.isNotEmpty()) notesBuilder.append(" • ")
            notesBuilder.append("UPI ID: $upiId")
        }

        return ReceiptScanResult(
            amount = amount,
            merchant = merchant ?: "UPI Merchant",
            suggestedCategoryName = categoryName,
            paymentMethod = paymentMethod,
            referenceId = refId,
            transactionDate = System.currentTimeMillis(),
            notes = if (notesBuilder.isNotEmpty()) notesBuilder.toString() else "Receipt scanned via Budget Bro",
            rawText = rawText,
            source = "ON_DEVICE_OCR"
        )
    }

    private fun detectPaymentApp(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("google pay") || lower.contains("gpay") || lower.contains("google transaction id") -> "Google Pay"
            lower.contains("phonepe") || lower.contains("ybl") || lower.contains("ibl") -> "PhonePe"
            lower.contains("paytm") || lower.contains("paytm payments bank") -> "Paytm"
            lower.contains("cred") -> "CRED"
            lower.contains("amazon pay") || lower.contains("apl") -> "Amazon Pay"
            lower.contains("bhim") -> "BHIM UPI"
            else -> "UPI"
        }
    }

    private fun extractAmount(rawText: String, lines: List<String>): Double? {
        // Pattern A: ₹ 1,250.00 or ₹450 or Rs. 500 or INR 300
        val patternA = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcherA = patternA.matcher(rawText)
        val candidates = mutableListOf<Double>()

        while (matcherA.find()) {
            val numStr = matcherA.group(1)?.replace(",", "")
            numStr?.toDoubleOrNull()?.let {
                if (it > 0 && it < 10000000) candidates.add(it)
            }
        }

        if (candidates.isNotEmpty()) {
            // Usually the primary transaction amount is the largest or first prominent amount
            return candidates.maxOrNull()
        }

        // Pattern B: Look for standalone numbers after "Paid", "Amount", "Total"
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("paid", ignoreCase = true) || line.contains("amount", ignoreCase = true) || line.contains("total", ignoreCase = true)) {
                val numMatcher = Pattern.compile("([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?)").matcher(line)
                if (numMatcher.find()) {
                    val num = numMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
                    if (num != null && num > 0) return num
                }
            }
        }

        return null
    }

    private fun extractMerchant(rawText: String, lines: List<String>): String? {
        // Pattern 1: "Paid to [Merchant]"
        val paidToRegex = Regex("""(?:Paid to|To|Payment to|Paying)\s+([A-Za-z0-9\s&.'\-]+)""", RegexOption.IGNORE_CASE)
        val paidToMatch = paidToRegex.find(rawText)
        if (paidToMatch != null) {
            val rawMerchant = paidToMatch.groupValues[1].lines().firstOrNull()?.trim()
            if (!rawMerchant.isNullOrBlank() && !rawMerchant.startsWith("₹") && !rawMerchant.startsWith("Rs")) {
                return cleanMerchantName(rawMerchant)
            }
        }

        // Pattern 2: Known Indian brands check
        val knownMerchants = listOf(
            "Swiggy", "Zomato", "Blinkit", "Zepto", "Instamart", "BigBasket",
            "Starbucks", "Chai Point", "McDonald's", "Domino's", "KFC", "Burger King",
            "Uber", "Ola", "Rapido", "IRCTC", "MakeMyTrip", "Indigo",
            "Amazon", "Flipkart", "Myntra", "Ajio", "Zara", "H&M", "Nykaa", "Croma",
            "Apollo Pharmacy", "Medplus", "1mg", "Netmeds",
            "Airtel", "Jio", "Vodafone", "Bescom", "Tata Power", "Indane", "HPCL", "BPCL", "Indian Oil", "Shell"
        )
        for (brand in knownMerchants) {
            if (rawText.contains(brand, ignoreCase = true)) {
                return brand
            }
        }

        // Pattern 3: Look for UPI ID handle (e.g., starbucks@okhdfcbank -> Starbucks)
        val upiHandle = extractUpiId(rawText)
        if (!upiHandle.isNullOrBlank()) {
            val handlePrefix = upiHandle.substringBefore("@").replace(".", " ").replace("_", " ")
            if (handlePrefix.length > 2 && !handlePrefix.all { it.isDigit() }) {
                return handlePrefix.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
            }
        }

        // Pattern 4: Inspect lines right after "Paid to" or "Completed"
        for (i in lines.indices) {
            val line = lines[i]
            if (line.equals("Paid to", ignoreCase = true) || line.equals("To", ignoreCase = true)) {
                if (i + 1 < lines.size) {
                    val next = lines[i + 1]
                    if (next.isNotBlank() && !next.contains("₹") && next.length < 40) {
                        return cleanMerchantName(next)
                    }
                }
            }
        }

        return null
    }

    private fun cleanMerchantName(raw: String): String {
        var clean = raw.trim()
        val removeKeywords = listOf("successful", "completed", "using", "from", "debit", "upi", "ref", "bank", "account")
        for (kw in removeKeywords) {
            val idx = clean.indexOf(" $kw", ignoreCase = true)
            if (idx > 0) {
                clean = clean.substring(0, idx).trim()
            }
        }
        return if (clean.length > 30) clean.take(30) else clean
    }

    private fun extractRefId(text: String): String? {
        val patterns = listOf(
            Regex("""(?:UPI (?:Ref|transaction) ID|UPI Ref No\.?|UTR|Transaction ID)\s*[:\-]?\s*([0-9A-Za-z]{10,24})""", RegexOption.IGNORE_CASE),
            Regex("""\b([0-9]{12})\b""") // 12-digit standard Indian UPI RRN / UTR
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractUpiId(text: String): String? {
        val upiRegex = Regex("""\b([a-zA-Z0-9.\-_]+@(okhdfcbank|okicici|oksbi|okaxis|ybl|ibl|axl|paytm|apl|barodampay|upi))\b""", RegexOption.IGNORE_CASE)
        val match = upiRegex.find(text)
        return match?.groupValues?.get(1)
    }

    fun suggestCategory(merchant: String?, rawText: String): String {
        val combined = "${merchant ?: ""} $rawText".lowercase()

        return when {
            combined.contains("swiggy") || combined.contains("zomato") || combined.contains("starbucks") ||
                    combined.contains("cafe") || combined.contains("coffee") || combined.contains("restaurant") ||
                    combined.contains("chai") || combined.contains("burger") || combined.contains("pizza") ||
                    combined.contains("biryani") || combined.contains("bakery") || combined.contains("food") -> "Food & Dining"

            combined.contains("blinkit") || combined.contains("zepto") || combined.contains("instamart") ||
                    combined.contains("bigbasket") || combined.contains("supermarket") || combined.contains("grocery") ||
                    combined.contains("dmart") || combined.contains("kirana") || combined.contains("provision") ||
                    combined.contains("fruits") || combined.contains("vegetables") || combined.contains("milk") -> "Groceries"

            combined.contains("uber") || combined.contains("ola") || combined.contains("rapido") ||
                    combined.contains("metro") || combined.contains("petrol") || combined.contains("fuel") ||
                    combined.contains("shell") || combined.contains("hpcl") || combined.contains("bpcl") ||
                    combined.contains("toll") || combined.contains("fastag") || combined.contains("parking") ||
                    combined.contains("irctc") || combined.contains("flight") || combined.contains("indigo") -> "Travel & Commute"

            combined.contains("amazon") || combined.contains("flipkart") || combined.contains("myntra") ||
                    combined.contains("ajio") || combined.contains("zara") || combined.contains("h&m") ||
                    combined.contains("shopping") || combined.contains("mall") || combined.contains("store") ||
                    combined.contains("retail") || combined.contains("croma") -> "Shopping"

            combined.contains("electricity") || combined.contains("bescom") || combined.contains("water") ||
                    combined.contains("airtel") || combined.contains("jio") || combined.contains("vi") ||
                    combined.contains("broadband") || combined.contains("wifi") || combined.contains("gas") ||
                    combined.contains("indane") || combined.contains("recharge") || combined.contains("dth") -> "Bills & Utilities"

            combined.contains("apollo") || combined.contains("medplus") || combined.contains("pharmacy") ||
                    combined.contains("hospital") || combined.contains("doctor") || combined.contains("clinic") ||
                    combined.contains("1mg") || combined.contains("netmeds") || combined.contains("medical") -> "Healthcare"

            combined.contains("netflix") || combined.contains("spotify") || combined.contains("bookmyshow") ||
                    combined.contains("pvr") || combined.contains("inox") || combined.contains("cinema") ||
                    combined.contains("movie") || combined.contains("youtube") || combined.contains("hotstar") -> "Entertainment"

            else -> "Food & Dining"
        }
    }

    /**
     * Gemini AI Vision Multimodal API call.
     */
    private suspend fun parseWithGeminiVision(imageUri: Uri, apiKey: String): ReceiptScanResult? = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
        val outputStream = ByteArrayOutputStream()
        // Compress bitmap to reasonable size for vision API
        val scaled = if (bitmap.width > 1200 || bitmap.height > 1200) {
            val scale = 1200f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            Analyze this Indian UPI payment receipt screenshot (e.g. Google Pay, PhonePe, Paytm, CRED, BHIM).
            Extract the exact payment details and return a JSON object with:
            - amount: number (e.g. 250.0)
            - merchant: string (clean merchant or recipient name, e.g. "Starbucks", "Swiggy", "Chai Point")
            - suggestedCategory: string (one of: "Food & Dining", "Groceries", "Shopping", "Travel & Commute", "Bills & Utilities", "Entertainment", "Healthcare", "Personal Care")
            - paymentMethod: string (e.g. "Google Pay", "PhonePe", "Paytm", "CRED", "UPI")
            - referenceId: string (UPI transaction ID or UTR number if present)
            - notes: string (brief note including UPI ID or context)
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        }))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val respBody = response.body?.string() ?: return@withContext null

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API error: ${response.code} $respBody")
            return@withContext null
        }

        val rootJson = JSONObject(respBody)
        val candidates = rootJson.optJSONArray("candidates") ?: return@withContext null
        if (candidates.length() == 0) return@withContext null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return@withContext null
        val parts = content.optJSONArray("parts") ?: return@withContext null
        if (parts.length() == 0) return@withContext null
        val resultText = parts.getJSONObject(0).optString("text") ?: return@withContext null

        val parsed = JSONObject(resultText)
        val amount = parsed.optDouble("amount", 0.0).takeIf { it > 0 }
        val merchant = parsed.optString("merchant", "").takeIf { it.isNotBlank() }
        val suggestedCat = parsed.optString("suggestedCategory", "Food & Dining")
        val pMethod = parsed.optString("paymentMethod", "UPI")
        val refId = parsed.optString("referenceId", "").takeIf { it.isNotBlank() }
        val notes = parsed.optString("notes", "").takeIf { it.isNotBlank() }

        return@withContext ReceiptScanResult(
            amount = amount,
            merchant = merchant,
            suggestedCategoryName = suggestedCat,
            paymentMethod = pMethod,
            referenceId = refId,
            transactionDate = System.currentTimeMillis(),
            notes = notes,
            source = "GEMINI_AI"
        )
    }
}
