package com.zodiary.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSectionParserTest {
    @Test
    fun recognizesEmojiHeadingsAndHeadingVariationsWithoutDroppingText() {
        val content = """✨ Wednesday — Libra

            🪐 overview : Take a slower look at the day.

            💼 Work and goals — Finish one useful thing.
            💖 Relationships: Say what you mean kindly.
            ⚡ Energy & mood: Notice when you need a pause.
            🔮 Today's tip - Write down what mattered.
        """.trimIndent()

        val sections = ReadingSectionParser.parse(content)

        assertEquals(
            listOf("Overview", "Work & goals", "Relationships", "Energy & mood", "Today's tip"),
            sections.mapNotNull { it.title },
        )
        val renderedBodies = sections.joinToString("\n") { it.body }
        listOf("Wednesday — Libra", "slower look", "one useful thing", "kindly", "need a pause", "what mattered").forEach {
            assertTrue("Missing content: $it", renderedBodies.contains(it))
        }
    }

    @Test
    fun unstructuredContentFallsBackToTheCompleteOriginalText() {
        val content = "A general reflection.\nAnd a second line with every detail intact."

        val section = ReadingSectionParser.parse(content).single()

        assertEquals(null, section.title)
        assertEquals(content, section.body)
    }

    @Test
    fun missingSectionsDoNotPreventContentFromRendering() {
        val content = "✨ Overview: This is the only section returned."

        val section = ReadingSectionParser.parse(content).single()

        assertEquals("Overview", section.title)
        assertEquals("This is the only section returned.", section.body)
    }
}
