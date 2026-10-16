package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AssistantEngine
import com.example.model.ActionCard
import com.example.model.ContactItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mitra AI", appName)
  }

  @Test
  fun `assistant recognizes calling intent`() {
    val contacts = listOf(ContactItem(name = "Rahul", phone = "9876543210", relation = "Friend"))
    val result = AssistantEngine.processPrompt("Rahul ko call lagao", contacts)
    assertNotNull(result.actionCard)
    assertTrue(result.actionCard is ActionCard.CallAction)
    val callAction = result.actionCard as ActionCard.CallAction
    assertEquals("Rahul", callAction.contactName)
    assertEquals("9876543210", callAction.phoneNumber)
  }

  @Test
  fun `assistant recognizes whatsapp intent`() {
    val contacts = listOf(ContactItem(name = "Mummy", phone = "9876543211", relation = "Mother"))
    val result = AssistantEngine.processPrompt("WhatsApp message bhej do Mummy ko mai late ho jaunga", contacts)
    assertNotNull(result.actionCard)
    assertTrue(result.actionCard is ActionCard.WhatsAppAction)
    val waAction = result.actionCard as ActionCard.WhatsAppAction
    assertEquals("Mummy", waAction.recipient)
    assertTrue(waAction.message.contains("late", ignoreCase = true))
  }

  @Test
  fun `assistant recognizes instagram intent`() {
    val contacts = emptyList<ContactItem>()
    val result = AssistantEngine.processPrompt("Instagram gym reel ke liye comment suggest karo", contacts)
    assertNotNull(result.actionCard)
    assertTrue(result.actionCard is ActionCard.InstagramAction)
    val instaAction = result.actionCard as ActionCard.InstagramAction
    assertTrue(instaAction.text.isNotEmpty())
    assertTrue(instaAction.hashtags.isNotEmpty())
  }
}
