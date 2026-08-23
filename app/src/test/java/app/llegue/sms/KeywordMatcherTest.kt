package app.llegue.sms

import app.llegue.sessions.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {

    @Test
    fun matchesRegardlessOfKeyboardCase() {
        assertTrue(KeywordMatcher.isKeyword("Dado", "dado"))
        assertTrue(KeywordMatcher.isKeyword("dado", "Dado"))
        assertTrue(KeywordMatcher.isKeyword("Dado", "DADO"))
        assertTrue(KeywordMatcher.isKeyword("Dado", " Dado "))
        assertTrue(KeywordMatcher.isKeyword("Dado", "dado\n"))
    }

    @Test
    fun rejectsDifferentWords() {
        assertFalse(KeywordMatcher.isKeyword("Dado", "Dad"))
        assertFalse(KeywordMatcher.isKeyword("Dado", "Dados"))
        assertFalse(KeywordMatcher.isKeyword("Dado", "clave"))
        assertFalse(KeywordMatcher.isKeyword("", "dado"))
        assertFalse(KeywordMatcher.isKeyword("Dado", "Dado!"))
    }

    @Test
    fun findsActiveSessionForTrustedContact() {
        val samePhone: (String, String) -> Boolean = { left, right -> left == right }
        val session = session(codeWord = "Dado", phone = "+5491112345678", active = true)
        val other = session(id = 2, codeWord = "Dado", phone = "+5491188888888", active = true)

        val match = KeywordMatcher.findSession(
                listOf(other, session),
                "+5491112345678",
                "dado",
                samePhone)

        assertSame(session, match)
    }

    @Test
    fun ignoresInactiveSessionAndWrongSender() {
        val samePhone: (String, String) -> Boolean = { left, right -> left == right }
        val inactive = session(codeWord = "Dado", phone = "+5491112345678", active = false)
        val stranger = session(id = 2, codeWord = "Dado", phone = "+5491188888888", active = true)

        assertNull(KeywordMatcher.findSession(listOf(inactive), "+5491112345678", "dado", samePhone))
        assertNull(KeywordMatcher.findSession(listOf(stranger), "+5491112345678", "dado", samePhone))
        assertNull(KeywordMatcher.findSession(listOf(stranger), "+5491188888888", "otra", samePhone))
    }

    private fun session(
            id: Long = 1,
            codeWord: String,
            phone: String,
            active: Boolean
    ) = Session(
            id = id,
            name = "viaje",
            contactName = "Ana",
            contactPhone = phone,
            codeWord = codeWord,
            intervalMinutes = null,
            startedAt = 0L,
            endsAt = null,
            active = active)
}
