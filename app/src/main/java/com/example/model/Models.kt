package com.example.model

sealed interface ActionCard {
    data class CallAction(
        val contactName: String,
        val phoneNumber: String,
        val note: String = ""
    ) : ActionCard

    data class WhatsAppAction(
        val recipient: String,
        val phoneNumber: String,
        val message: String
    ) : ActionCard

    data class InstagramAction(
        val text: String,
        val hashtags: List<String> = emptyList(),
        val vibe: String = "Viral"
    ) : ActionCard

    data class CallScriptAction(
        val title: String,
        val openingLine: String,
        val keyPoints: List<String>,
        val closingLine: String
    ) : ActionCard
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionCard: ActionCard? = null,
    val suggestedFollowUps: List<String> = emptyList()
)

data class ContactItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val relation: String,
    val colorHex: Long = 0xFF00E5FF
)

data class InstagramPreset(
    val title: String,
    val vibe: String,
    val text: String,
    val hashtags: List<String>
)

data class WhatsAppTemplate(
    val id: String,
    val title: String,
    val category: String,
    val draftText: String
)
