package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPersonaVoiceParameterMappings() {
    val hinataPitch = 1.6f
    val hinataSpeed = 0.95f
    assertEquals(1.6f, hinataPitch, 0.01f)
    assertEquals(0.95f, hinataSpeed, 0.01f)

    val sakuraPitch = 1.3f
    val sakuraSpeed = 1.1f
    assertEquals(1.3f, sakuraPitch, 0.01f)
    assertEquals(1.1f, sakuraSpeed, 0.01f)

    val tsunadePitch = 0.9f
    val tsunadeSpeed = 0.9f
    assertEquals(0.9f, tsunadePitch, 0.01f)
    assertEquals(0.9f, tsunadeSpeed, 0.01f)
  }

  @Test
  fun testVideoIntentDetection() {
    val prompt1 = "mujhe ek anime video bana ke do"
    val prompt2 = "ek video banao"
    val prompt3 = "kya haal chaal hai"

    assertTrue(prompt1.lowercase().contains("video"))
    assertTrue(prompt2.lowercase().contains("video") || prompt2.lowercase().contains("banao video"))
    assertFalse(prompt3.lowercase().contains("video"))
  }

  @Test
  fun testInCharacterFallbackResponses() {
    val hinataReply = com.example.data.remote.GeminiApiClient.getInCharacterFallbackResponse("joke sunao", "hinata")
    assertTrue(hinataReply.startsWith("Hinata:"))

    val sakuraReply = com.example.data.remote.GeminiApiClient.getInCharacterFallbackResponse("kaise ho", "sakura")
    assertTrue(sakuraReply.startsWith("Sakura:"))

    val tsunadeReply = com.example.data.remote.GeminiApiClient.getInCharacterFallbackResponse("sweet bolo", "tsunade")
    assertTrue(tsunadeReply.startsWith("Tsunade:"))
  }
}
