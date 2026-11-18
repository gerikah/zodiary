package com.zodiary.app.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MoonSnapshot(
    val sign: String?,
    val phase: String?,
    val phaseName: String?,
    val illumination: Double?,
)

data class ReadingSection(val title: String?, val body: String)

data class DailyReading(
    val sign: String,
    val signSlug: String,
    val providerDate: LocalDate,
    val weekday: String?,
    val generatedAt: Instant?,
    /** The complete provider text is retained and rendered as plain text. */
    val content: String,
    val closing: String?,
    val moon: MoonSnapshot?,
    val sections: List<ReadingSection>,
)

data class DailyReadingBatch(
    val providerDate: LocalDate,
    val generatedAt: Instant?,
    val readings: Map<String, DailyReading>,
) {
    fun readingFor(sign: String): DailyReading? = CosmyDayResponseAdapter.normalizeSign(sign)?.let(readings::get)
}

object DailyReadingLabels {
    fun title(reading: DailyReading, localToday: LocalDate): String = if (reading.providerDate == localToday) {
        "Today's reading for ${reading.sign}"
    } else {
        val date = reading.providerDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        "Reading for ${reading.sign} · $date"
    }
}

/** Adapts CosmyDay's title-cased all-sign map and lowercase sign slugs to a stable internal model. */
object CosmyDayResponseAdapter {
    val supportedSigns = listOf(
        "aries", "taurus", "gemini", "cancer", "leo", "virgo",
        "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces",
    )

    fun normalizeSign(value: String?): String? {
        val normalized = value?.trim()?.lowercase(Locale.ROOT) ?: return null
        return normalized.takeIf { it in supportedSigns }
    }

    fun parseAllSigns(json: String): DailyReadingBatch {
        val root = JSONObject(json)
        val horoscopes = root.optJSONObject("horoscopes")
            ?: throw IllegalArgumentException("Daily response did not contain a horoscopes object")
        val sharedDate = root.stringOrNull("date")?.let(::parseDateOrNull)
        val sharedGeneratedAt = root.stringOrNull("generated_at")?.let(::parseInstantOrNull)
        val sharedMoon = root.optJSONObject("sky")?.optJSONObject("moon")?.toMoonSnapshot()
        val normalized = linkedMapOf<String, DailyReading>()

        val keys = horoscopes.keys()
        while (keys.hasNext()) {
            val responseKey = keys.next()
            val keySlug = normalizeSign(responseKey) ?: continue
            val item = horoscopes.optJSONObject(responseKey)
                ?: throw IllegalArgumentException("Daily entry for $responseKey was not an object")
            val itemSlug = normalizeSign(item.stringOrNull("sign")) ?: keySlug
            require(itemSlug == keySlug) { "Daily entry sign did not match its response key" }

            val date = item.stringOrNull("date")?.let(::parseDateOrNull) ?: sharedDate
                ?: throw IllegalArgumentException("Daily entry for $responseKey had no valid provider date")
            val content = item.stringOrNull("content")
                ?: throw IllegalArgumentException("Daily entry for $responseKey had no content")
            val signName = displayName(item.stringOrNull("sign") ?: responseKey)
            val moon = item.optJSONObject("moon")?.toMoonSnapshot() ?: sharedMoon
            normalized[keySlug] = DailyReading(
                sign = signName,
                signSlug = keySlug,
                providerDate = date,
                weekday = item.stringOrNull("weekday") ?: root.stringOrNull("weekday"),
                generatedAt = item.stringOrNull("generated_at")?.let(::parseInstantOrNull) ?: sharedGeneratedAt,
                content = content,
                closing = item.stringOrNull("closing"),
                moon = moon,
                sections = ReadingSectionParser.parse(content),
            )
        }

        val missing = supportedSigns - normalized.keys
        require(missing.isEmpty()) { "Daily response was incomplete: ${missing.joinToString()}" }
        val batchDate = sharedDate ?: normalized.values.firstOrNull()?.providerDate
            ?: throw IllegalArgumentException("Daily response had no valid provider date")
        return DailyReadingBatch(batchDate, sharedGeneratedAt, normalized.toMap())
    }

    fun parseSingleSign(json: String): DailyReading {
        val root = JSONObject(json)
        val slug = normalizeSign(root.stringOrNull("sign"))
            ?: throw IllegalArgumentException("Daily response had an unknown sign")
        val date = root.stringOrNull("date")?.let(::parseDateOrNull)
            ?: throw IllegalArgumentException("Daily response had no valid provider date")
        val content = root.stringOrNull("content")
            ?: throw IllegalArgumentException("Daily response had no content")
        val moon = root.optJSONObject("moon")?.toMoonSnapshot()
            ?: root.optJSONObject("sky")?.optJSONObject("moon")?.toMoonSnapshot()
        return DailyReading(
            sign = displayName(root.stringOrNull("sign") ?: slug),
            signSlug = slug,
            providerDate = date,
            weekday = root.stringOrNull("weekday"),
            generatedAt = root.stringOrNull("generated_at")?.let(::parseInstantOrNull),
            content = content,
            closing = root.stringOrNull("closing"),
            moon = moon,
            sections = ReadingSectionParser.parse(content),
        )
    }

    private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    private fun parseInstantOrNull(value: String): Instant? = runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()

    private fun displayName(value: String): String = value.trim().lowercase(Locale.ROOT)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    private fun JSONObject.toMoonSnapshot(): MoonSnapshot? {
        val moon = MoonSnapshot(
            sign = stringOrNull("sign"),
            phase = stringOrNull("phase"),
            phaseName = stringOrNull("phase_name"),
            illumination = numberOrNull("illumination")?.takeIf { it.isFinite() && it in 0.0..100.0 },
        )
        return moon.takeIf { it.sign != null || it.phase != null || it.phaseName != null || it.illumination != null }
    }

    private fun JSONObject.stringOrNull(key: String): String? {
        val value = opt(key)
        if (value == null || value == JSONObject.NULL) return null
        return value.toString().trim().takeIf(String::isNotEmpty)
    }

    private fun JSONObject.numberOrNull(key: String): Double? {
        val value = opt(key)
        return when {
            value is Number -> value.toDouble()
            value is String -> value.toDoubleOrNull()
            else -> null
        }
    }
}

object ReadingSectionParser {
    private val recognizedHeading = Regex(
        "^(Overview|Work\\s*(?:&|and|/)\\s*Goals?|Relationships?|Energy\\s*(?:&|and|/)\\s*Mood|Today['’]?s\\s*Tip|Daily\\s*Tip)\\s*[:：—–.\\-]\\s*(.*)$",
        RegexOption.IGNORE_CASE,
    )

    fun parse(content: String): List<ReadingSection> {
        if (content.isBlank()) return emptyList()

        val sections = mutableListOf<MutableSection>()
        content.replace("\r\n", "\n").replace('\r', '\n').lines().forEach { rawLine ->
            if (rawLine.isBlank()) {
                sections.lastOrNull()?.appendBlankLine()
                return@forEach
            }

            val line = rawLine.trim()
            val withoutDecoration = line.dropWhile { !it.isLetter() }
            val match = recognizedHeading.matchEntire(withoutDecoration)
            if (match != null) {
                sections += MutableSection(canonicalTitle(match.groupValues[1]), match.groupValues[2].trim())
            } else {
                val current = sections.lastOrNull()
                if (current == null) sections += MutableSection(null, line)
                else current.append(line)
            }
        }

        val parsed = sections.mapNotNull { section ->
            section.build()?.takeIf { it.body.isNotBlank() }
        }
        if (parsed.none { it.title != null }) return listOf(ReadingSection(title = null, body = content))
        return parsed.ifEmpty { listOf(ReadingSection(title = null, body = content)) }
    }

    private fun canonicalTitle(value: String): String {
        val normalized = value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        return when {
            normalized.startsWith("overview") -> "Overview"
            normalized.startsWith("work") -> "Work & goals"
            normalized.startsWith("relationship") -> "Relationships"
            normalized.startsWith("energy") -> "Energy & mood"
            else -> "Today's tip"
        }
    }

    private class MutableSection(val title: String?, initial: String) {
        private val body = StringBuilder(initial)

        fun append(value: String) {
            if (body.isNotEmpty()) body.append('\n')
            body.append(value)
        }

        fun appendBlankLine() {
            if (body.isNotEmpty() && !body.endsWith("\n\n")) body.append('\n')
        }

        fun build(): ReadingSection? = if (body.isEmpty()) null else ReadingSection(title, body.toString().trim())
    }
}
