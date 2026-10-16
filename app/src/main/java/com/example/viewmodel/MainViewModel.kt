package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AssistantEngine
import com.example.model.ActionCard
import com.example.model.ChatMessage
import com.example.model.ContactItem
import com.example.model.InstagramPreset
import com.example.model.WhatsAppTemplate
import com.example.service.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val voiceManager = VoiceManager(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Chat, 1: Calling, 2: WhatsApp, 3: Instagram
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _savedContacts = MutableStateFlow<List<ContactItem>>(defaultContacts())
    val savedContacts: StateFlow<List<ContactItem>> = _savedContacts.asStateFlow()

    // Calling Screen state
    private val _dialpadNumber = MutableStateFlow("")
    val dialpadNumber: StateFlow<String> = _dialpadNumber.asStateFlow()

    // WhatsApp Screen state
    private val _waPhoneNumber = MutableStateFlow("")
    val waPhoneNumber: StateFlow<String> = _waPhoneNumber.asStateFlow()

    private val _waMessageText = MutableStateFlow("")
    val waMessageText: StateFlow<String> = _waMessageText.asStateFlow()

    private val _waSelectedCategory = MutableStateFlow("Late/Traffic")
    val waSelectedCategory: StateFlow<String> = _waSelectedCategory.asStateFlow()

    // Instagram Screen state
    private val _instaSubject = MutableStateFlow("")
    val instaSubject: StateFlow<String> = _instaSubject.asStateFlow()

    private val _instaSelectedVibe = MutableStateFlow("Savage 🔥")
    val instaSelectedVibe: StateFlow<String> = _instaSelectedVibe.asStateFlow()

    private val _instaGeneratedResults = MutableStateFlow<List<InstagramPreset>>(defaultInstagramPresets())
    val instaGeneratedResults: StateFlow<List<InstagramPreset>> = _instaGeneratedResults.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    init {
        // Initial friendly greeting message from Mitra AI
        val initialGreeting = ChatMessage(
            text = "Namaste! 🙏 Mai hu Mitra AI - aapka personal smart assistant.\n\nAap mujhe bolkar ya likhkar command de sakte hain jaise:\n• \"Rahul ko call lagao\"\n• \"WhatsApp message likho mummy ke liye ki late ho jaunga\"\n• \"Gym photo ke liye viral Insta comment do\"\n\nBoliye, aaj mai aapki kya madad karu?",
            isFromUser = false,
            suggestedFollowUps = listOf(
                "Mummy ko call lagao",
                "WhatsApp message draft karo",
                "Instagram photo ke liye funny comment",
                "Boss se leave lene ki call script"
            )
        )
        _messages.value = listOf(initialGreeting)
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setDialpadNumber(number: String) {
        _dialpadNumber.value = number
    }

    fun appendDialpadDigit(digit: String) {
        _dialpadNumber.value += digit
    }

    fun deleteDialpadDigit() {
        if (_dialpadNumber.value.isNotEmpty()) {
            _dialpadNumber.value = _dialpadNumber.value.dropLast(1)
        }
    }

    fun setWaPhoneNumber(phone: String) {
        _waPhoneNumber.value = phone
    }

    fun setWaMessageText(message: String) {
        _waMessageText.value = message
    }

    fun setWaSelectedCategory(category: String) {
        _waSelectedCategory.value = category
    }

    fun setInstaSubject(subject: String) {
        _instaSubject.value = subject
    }

    fun setInstaSelectedVibe(vibe: String) {
        _instaSelectedVibe.value = vibe
    }

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

    fun sendMessage(userText: String? = null) {
        val query = (userText ?: _inputText.value).trim()
        if (query.isBlank()) return

        val userMessage = ChatMessage(text = query, isFromUser = true)
        val updatedList = _messages.value + userMessage
        _messages.value = updatedList
        _inputText.value = ""

        viewModelScope.launch {
            val response = AssistantEngine.processPrompt(query, _savedContacts.value)
            _messages.value = _messages.value + response
            // Speak the response aloud via TTS
            voiceManager.speak(response.text)
        }
    }

    fun startVoiceInput() {
        voiceManager.startListening(
            onResult = { spokenText ->
                sendMessage(spokenText)
            },
            onError = { errMsg ->
                _statusNotice.value = errMsg
            }
        )
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
    }

    fun toggleSpeechVoice() {
        voiceManager.toggleSpeechVoice()
    }

    fun addContact(name: String, phone: String, relation: String) {
        if (name.isBlank() || phone.isBlank()) return
        val colors = listOf(0xFF00E5FF, 0xFF8B5CF6, 0xFFEC4899, 0xFF22C55E, 0xFFF59E0B, 0xFF3B82F6)
        val randomColor = colors.random()
        val newContact = ContactItem(
            name = name.trim(),
            phone = phone.trim(),
            relation = relation.ifBlank { "Friend" },
            colorHex = randomColor
        )
        _savedContacts.value = _savedContacts.value + newContact
        _statusNotice.value = "Contact '$name' saved successfully!"
    }

    fun deleteContact(id: String) {
        _savedContacts.value = _savedContacts.value.filterNot { it.id == id }
    }

    fun generateWhatsAppPreset(category: String, tone: String = "Casual") {
        _waSelectedCategory.value = category
        val draft = when (category) {
            "Late/Traffic" -> "Hey! Traffic bohot zyada hai, isliye 20-25 mins late ho jaunga. Really sorry for the delay! 🙏"
            "Chai/Coffee" -> "Bhai free ho to batao, shaam ko ek kadak chai aur gup-shup ka plan banate hain! ☕✨"
            "Birthday Wish" -> "Wishing you the happiest birthday! 🎂🎉 May all your dreams come true this year. Big party due hai! 😉"
            "Meeting/Work" -> "Hello, just following up on our discussion earlier today. Please let me know when you are free to sync up. Thanks!"
            "Urgent Call" -> "Urgent: Please call me back as soon as you see this message! 📞"
            "Apology" -> "Hey, extremely sorry about earlier. I did not mean to upset you. Let me know whenever you're free to talk."
            else -> "Hey! Kaise ho? Free ho to reply karna, zaroori baat karni thi."
        }
        _waMessageText.value = draft
    }

    fun generateInstagramOptions(subject: String, vibe: String) {
        val topic = if (subject.isNotBlank()) subject else "Photoshoot & Vibe"
        val results = mutableListOf<InstagramPreset>()

        when {
            vibe.contains("Savage") || vibe.contains("Funny") -> {
                results.add(
                    InstagramPreset(
                        title = "Hero Entry 😂",
                        vibe = vibe,
                        text = "Itna smart lagne ka license kahan se banwaya bhai? Hollywood walon ko tag karu kya? 🕶️🔥",
                        hashtags = listOf("#TooCool", "#MainCharacter", "#BhaiKaSwag", "#DapperLook")
                    )
                )
                results.add(
                    InstagramPreset(
                        title = "Confidence 100%",
                        vibe = vibe,
                        text = "Proof that perfection exists! Stop looking so illegal bro! 🚨💯",
                        hashtags = listOf("#SavageVibes", "#Trending", "#FirePost", "#Swag")
                    )
                )
                results.add(
                    InstagramPreset(
                        title = "Meme Roast",
                        vibe = vibe,
                        text = "Ye banda phone ke screen se seedha ramp walk par utar aaya hai! 🔥😂",
                        hashtags = listOf("#ModelLife", "#ViralGram", "#ReelsIndia")
                    )
                )
            }
            vibe.contains("Aesthetic") || vibe.contains("Poetic") -> {
                results.add(
                    InstagramPreset(
                        title = "Golden Hue ✨",
                        vibe = vibe,
                        text = "Capturing peace in chaotic moments. Golden glow and soft thoughts. 🌅🤍",
                        hashtags = listOf("#AestheticMoments", "#GoldenHour", "#SoftAesthetic", "#Serenity")
                    )
                )
                results.add(
                    InstagramPreset(
                        title = "Poetic Soul 📖",
                        vibe = vibe,
                        text = "Har tasveer ek kahani kehti hai, aur ye wali sukoon bayan kar rahi hai. 🍂✨",
                        hashtags = listOf("#Sukoon", "#PoeticVibes", "#NatureLover", "#Peaceful")
                    )
                )
            }
            vibe.contains("Gym") || vibe.contains("Hype") -> {
                results.add(
                    InstagramPreset(
                        title = "Pure Beast Mode 💪",
                        vibe = vibe,
                        text = "Hard work in the shadows, shine under the spotlight! Consistent energy bro! ⚡💥",
                        hashtags = listOf("#GymMotivation", "#BeastMode", "#NoDaysOff", "#FitnessJourney")
                    )
                )
                results.add(
                    InstagramPreset(
                        title = "Gains On Point 🔥",
                        vibe = vibe,
                        text = "Screen faad physique! Dedication level 1000%! Keep crushing it champion! 🏆💪",
                        hashtags = listOf("#FitFam", "#Bodybuilding", "#GrindNeverStops", "#PumpCover")
                    )
                )
            }
            else -> {
                results.add(
                    InstagramPreset(
                        title = "Universal Fire 🔥",
                        vibe = vibe,
                        text = "Straight up 10/10 vibe! Outstanding capture bro! 📸✨",
                        hashtags = listOf("#VibeCheck", "#ExplorePage", "#TrendingReels", "#InstaDaily")
                    )
                )
                results.add(
                    InstagramPreset(
                        title = "Iconic Mood 💫",
                        vibe = vibe,
                        text = "Serving looks and good energy only! Forever unmatched. 👑🌟",
                        hashtags = listOf("#StayIconic", "#BestOfTheDay", "#PureVibes")
                    )
                )
            }
        }
        _instaGeneratedResults.value = results
    }

    fun clearChat() {
        val initialGreeting = ChatMessage(
            text = "Chat cleared! Naya conversation shuru karte hain. Kya kaam karna hai aapko?",
            isFromUser = false,
            suggestedFollowUps = listOf(
                "Rahul ko call lagao",
                "WhatsApp message bhej do",
                "Instagram comment suggest karo"
            )
        )
        _messages.value = listOf(initialGreeting)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }

    private fun defaultContacts(): List<ContactItem> {
        return listOf(
            ContactItem(name = "Mummy", phone = "9876543210", relation = "Mother", colorHex = 0xFFEC4899),
            ContactItem(name = "Papa", phone = "9876543211", relation = "Father", colorHex = 0xFF3B82F6),
            ContactItem(name = "Rahul Bestie", phone = "9876543212", relation = "Friend", colorHex = 0xFF00E5FF),
            ContactItem(name = "Priya", phone = "9876543213", relation = "Friend", colorHex = 0xFF8B5CF6),
            ContactItem(name = "Office / Boss", phone = "9876543214", relation = "Work", colorHex = 0xFFF59E0B),
            ContactItem(name = "Emergency Help", phone = "112", relation = "Emergency", colorHex = 0xFFEF4444)
        )
    }

    private fun defaultInstagramPresets(): List<InstagramPreset> {
        return listOf(
            InstagramPreset(
                title = "Hero Swag 🔥",
                vibe = "Savage",
                text = "Itna smart lagne ka license kahan se banwaya bhai? Hollywood se call kab aa raha hai? 😂🕶️",
                hashtags = listOf("#BhaiKaSwag", "#TooCool", "#HeroVibes")
            ),
            InstagramPreset(
                title = "Aesthetic Sunset 🌅",
                vibe = "Aesthetic",
                text = "Sunsets prove that endings can be beautiful too. Golden hour glow hitting just right! ✨🧡",
                hashtags = listOf("#GoldenHour", "#SunsetVibes", "#AestheticPost")
            ),
            InstagramPreset(
                title = "Beast Mode 💪",
                vibe = "Hype",
                text = "Screen faad physique! Pure hard work and consistency speaking! Keep crushing it! 🔥⚡",
                hashtags = listOf("#GymBro", "#FitnessMotivation", "#BeastMode")
            )
        )
    }
}
