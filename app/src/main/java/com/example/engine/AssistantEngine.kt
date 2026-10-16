package com.example.engine

import com.example.model.ActionCard
import com.example.model.ChatMessage
import com.example.model.ContactItem
import java.util.Locale

object AssistantEngine {

    /**
     * Interprets user prompt and generates response with contextual ActionCards.
     */
    fun processPrompt(
        prompt: String,
        savedContacts: List<ContactItem>
    ): ChatMessage {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        // 1. CALLING INTENTS:
        // Examples: "Call Rahul", "Mummy ko call lagao", "Phone karo 9876543210", "Call lagao"
        if (isCallingIntent(lower)) {
            val (name, number) = extractContactOrNumber(prompt, lower, savedContacts)
            val actionCard = ActionCard.CallAction(
                contactName = name.ifEmpty { "Selected Contact" },
                phoneNumber = number.ifEmpty { "Enter Number" },
                note = "Voice command detected: Call $name"
            )
            return ChatMessage(
                text = "Haanji! Maine ${if (name.isNotEmpty()) name else "number"} ke liye calling card taiyaar kar diya hai. Aap 'Call Now' dabakar seedha call kar sakte hain.",
                isFromUser = false,
                actionCard = actionCard,
                suggestedFollowUps = listOf(
                    "Call karne se pehle script batao",
                    "WhatsApp message bhejo",
                    "Insta comment banao"
                )
            )
        }

        // 2. WHATSAPP INTENTS:
        // Examples: "WhatsApp Rahul", "WhatsApp message likho", "Mummy ko bolo late ho jaunga"
        if (isWhatsAppIntent(lower)) {
            val (recipient, phone) = extractRecipientForWhatsApp(prompt, lower, savedContacts)
            val draftedMessage = craftWhatsAppMessage(lower, recipient)
            val actionCard = ActionCard.WhatsAppAction(
                recipient = recipient.ifEmpty { "Friend" },
                phoneNumber = phone,
                message = draftedMessage
            )
            return ChatMessage(
                text = "Zaroor! Maine WhatsApp ke liye badhiya message draft kiya hai. Aap ise padhein aur direct WhatsApp me bhej dein:",
                isFromUser = false,
                actionCard = actionCard,
                suggestedFollowUps = listOf(
                    "Ise thoda funny banao",
                    "Thoda formal tone me likho",
                    "Call lagane ka card do"
                )
            )
        }

        // 3. INSTAGRAM COMMENT / CAPTION INTENTS:
        // Examples: "Instagram comment", "Insta caption", "Gym post pe comment", "Travel photo caption"
        if (isInstagramIntent(lower)) {
            val (comment, hashtags, vibe) = generateInstagramContent(lower)
            val actionCard = ActionCard.InstagramAction(
                text = comment,
                hashtags = hashtags,
                vibe = vibe
            )
            return ChatMessage(
                text = "Ye lijiye aapke Instagram ke liye super trending comment & caption! 1-tap Copy dabayein aur Instagram post par paste karein:",
                isFromUser = false,
                actionCard = actionCard,
                suggestedFollowUps = listOf(
                    "Aur funny options do",
                    "Shayari style me comment chahiye",
                    "Short & savage comment"
                )
            )
        }

        // 4. CALL SCRIPT / TALKING POINTS:
        // Examples: "Boss se baat kaise karu", "Leave request", "Interview tips", "Script"
        if (lower.contains("script") || lower.contains("kaise baat") || lower.contains("leave") || lower.contains("permission")) {
            val scriptAction = generateCallScript(lower)
            return ChatMessage(
                text = "Call par confident baat karne ke liye ye step-by-step guide taiyaar hai:",
                isFromUser = false,
                actionCard = scriptAction,
                suggestedFollowUps = listOf(
                    "Ab call lagao",
                    "WhatsApp text bhej do",
                    "Insta reels ke captions"
                )
            )
        }

        // 5. GREETING & GENERAL ASSISTANT REPLIES IN HINDI/HINGLISH:
        val defaultReply = generateConversationalReply(lower)
        return defaultReply
    }

    private fun isCallingIntent(lower: String): Boolean {
        return lower.startsWith("call") ||
                lower.contains("call lagao") ||
                lower.contains("call karo") ||
                lower.contains("phone lagao") ||
                lower.contains("phone karo") ||
                lower.contains("call milao") ||
                lower.contains("dial")
    }

    private fun isWhatsAppIntent(lower: String): Boolean {
        return lower.contains("whatsapp") ||
                lower.contains("wa message") ||
                lower.contains("message bhej") ||
                lower.contains("msg bhej") ||
                lower.contains("chat karo")
    }

    private fun isInstagramIntent(lower: String): Boolean {
        return lower.contains("insta") ||
                lower.contains("instagram") ||
                lower.contains("caption") ||
                lower.contains("comment") ||
                lower.contains("reel") ||
                lower.contains("post pe kya")
    }

    private fun extractContactOrNumber(
        original: String,
        lower: String,
        savedContacts: List<ContactItem>
    ): Pair<String, String> {
        // Extract raw phone number if present
        val phoneRegex = Regex("[0-9]{10,12}")
        val match = phoneRegex.find(original)
        if (match != null) {
            return Pair("Phone Number", match.value)
        }

        // Check in saved contacts
        for (contact in savedContacts) {
            if (lower.contains(contact.name.lowercase()) || lower.contains(contact.relation.lowercase())) {
                return Pair(contact.name, contact.phone)
            }
        }

        // Common family/friend keywords
        val name = when {
            lower.contains("mummy") || lower.contains("mom") || lower.contains("maa") -> "Mummy"
            lower.contains("papa") || lower.contains("dad") || lower.contains("pitaji") -> "Papa"
            lower.contains("bhai") || lower.contains("brother") -> "Bhai"
            lower.contains("behan") || lower.contains("sister") -> "Sister"
            lower.contains("boss") || lower.contains("sir") -> "Boss"
            lower.contains("priya") -> "Priya"
            lower.contains("rahul") -> "Rahul"
            lower.contains("amit") -> "Amit"
            else -> {
                // Try to take the word after "call"
                val words = original.split(" ")
                val callIndex = words.indexOfFirst { it.equals("call", ignoreCase = true) }
                if (callIndex != -1 && callIndex + 1 < words.size) {
                    words[callIndex + 1].replace(Regex("[^a-zA-Z]"), "")
                } else "Friend"
            }
        }

        // Find if this name matches any saved contact phone
        val matched = savedContacts.find { it.name.equals(name, ignoreCase = true) }
        val phone = matched?.phone ?: ""

        return Pair(name, phone)
    }

    private fun extractRecipientForWhatsApp(
        original: String,
        lower: String,
        savedContacts: List<ContactItem>
    ): Pair<String, String> {
        val phoneRegex = Regex("[0-9]{10,12}")
        val match = phoneRegex.find(original)
        val directPhone = match?.value ?: ""

        for (contact in savedContacts) {
            if (lower.contains(contact.name.lowercase()) || lower.contains(contact.relation.lowercase())) {
                return Pair(contact.name, contact.phone)
            }
        }

        val name = when {
            lower.contains("mummy") || lower.contains("mom") -> "Mummy"
            lower.contains("papa") || lower.contains("dad") -> "Papa"
            lower.contains("boss") -> "Boss"
            lower.contains("rahul") -> "Rahul"
            lower.contains("amit") -> "Amit"
            else -> "Contact"
        }

        return Pair(name, directPhone)
    }

    private fun craftWhatsAppMessage(lower: String, recipient: String): String {
        return when {
            lower.contains("late") || lower.contains("traffic") || lower.contains("deri") ->
                "Hey $recipient, traffic bohot zyada hai to mujhe thoda late ho jayega. Lagbhag 25-30 mins me pahunch raha hu! Sorry for the delay. 🙏"

            lower.contains("birthday") || lower.contains("janamdin") ->
                "Wishing you a very Happy Birthday $recipient! 🎂🎉 May your year ahead be blessed with happiness, good health, and success! Party kab de rahe ho? 😉"

            lower.contains("leave") || lower.contains("chhutti") || lower.contains("urgent work") ->
                "Dear Sir/Ma'am, I have an urgent personal commitment today and will not be able to attend. I will ensure my pending tasks are addressed. Thank you for understanding."

            lower.contains("kaha ho") || lower.contains("plan") || lower.contains("milte") ->
                "Bhai $recipient, kya plan hai aaj ka? Shaam ko milte hain chai/coffee pe? Free ho to batana! ☕✨"

            lower.contains("sorry") || lower.contains("maafi") ->
                "Hey $recipient, really sorry for earlier. Mera bilkul aisa intention nahi tha. Let's talk whenever you are comfortable. Take care! 🤝"

            lower.contains("paisa") || lower.contains("payment") || lower.contains("gpay") ->
                "Hi $recipient, please confirm if you received the payment transferred earlier. Screenshot share kar di hai. Thanks!"

            else ->
                "Hey $recipient, hope you're doing great! Free ho to call karna, zaroori baat karni thi. 👍"
        }
    }

    fun generateInstagramContent(lower: String): Triple<String, List<String>, String> {
        return when {
            lower.contains("gym") || lower.contains("fitness") || lower.contains("workout") || lower.contains("body") -> {
                Triple(
                    "Beast mode on! 🔥 Consistent mehnat ka result screen faad raha hai bro! Keep grinding! 💪⚡",
                    listOf("#GymBro", "#FitnessMotivation", "#BeastMode", "#NoExcuses", "#GrindNeverStops"),
                    "Hype & Fire 🔥"
                )
            }
            lower.contains("travel") || lower.contains("vacation") || lower.contains("mountain") || lower.contains("beach") -> {
                Triple(
                    "Wanderlust vibes at their peak! 🌄 Such breathtaking scenery, adding this spot to my bucket list! ✨🌊",
                    listOf("#TravelDiaries", "#Wanderlust", "#NatureLovers", "#ScenicViews", "#TravelGram"),
                    "Aesthetic & Dreamy ✨"
                )
            }
            lower.contains("funny") || lower.contains("savage") || lower.contains("roast") -> {
                Triple(
                    "Itna smart lagne ka copyright kiske paas hai bhai? Hollywood se call kab aa raha hai? 😂🕶️",
                    listOf("#BhaiKaSwag", "#HeroVibes", "#TooCool", "#ReelKaroFeelKaro"),
                    "Savage & Humorous 😂"
                )
            }
            lower.contains("sunset") || lower.contains("aesthetic") || lower.contains("nature") -> {
                Triple(
                    "Sunsets prove that endings can be beautiful too. Golden hour glow hitting just right! 🌅🧡",
                    listOf("#GoldenHour", "#SunsetVibes", "#AestheticPost", "#PeacefulMoments"),
                    "Poetic & Calming 🌅"
                )
            }
            lower.contains("outfit") || lower.contains("look") || lower.contains("drip") || lower.contains("fashion") -> {
                Triple(
                    "10/10 Drip! Outfit bilkul on point hai bro, pure main character energy! 👔🔥",
                    listOf("#OOTD", "#FashionGram", "#DripCheck", "#MainCharacter", "#StyleInspo"),
                    "Stylish & Chic 💫"
                )
            }
            lower.contains("bestie") || lower.contains("dost") || lower.contains("friend") -> {
                Triple(
                    "Pure sibling vibes with this one! Nazar na lage humari jodi ko! 🧿❤️ Always got your back!",
                    listOf("#BestiesForLife", "#Dosti", "#SquadGoals", "#Brothers"),
                    "Warm & Loving ❤️"
                )
            }
            else -> {
                Triple(
                    "Straight up fire post! 🔥 The vibe is absolutely unmatched. Keep shining! ✨🙌",
                    listOf("#VibeCheck", "#ExplorePage", "#TrendingNow", "#GoodVibesOnly"),
                    "Universal Viral 🚀"
                )
            }
        }
    }

    private fun generateCallScript(lower: String): ActionCard.CallScriptAction {
        return when {
            lower.contains("leave") || lower.contains("chhutti") || lower.contains("boss") -> {
                ActionCard.CallScriptAction(
                    title = "Leave Request to Manager/Boss",
                    openingLine = "Hello Sir/Ma'am, good morning/afternoon! Do you have a quick 2 minutes to speak?",
                    keyPoints = listOf(
                        "State reason politely: 'Sir, due to an unavoidable personal emergency / family function on [Day]...'",
                        "Work handover: 'Maine urgent tasks handover kar diye hain aur email monitor karunga.'",
                        "Clear timeline: 'Mai [Date] ko office resume kar lunga sir.'"
                    ),
                    closingLine = "Thank you so much Sir for your understanding and support. Have a great day!"
                )
            }
            lower.contains("mummy") || lower.contains("permission") || lower.contains("trip") -> {
                ActionCard.CallScriptAction(
                    title = "Trip / Night-out Permission from Parents",
                    openingLine = "Mummy/Papa, ek achhi khabar hai aur aapse ek choti si permission chahiye thi!",
                    keyPoints = listOf(
                        "Introduce safety: 'Hum 4 dost ja rahe hain, sabhi trusted hain aur travel details share kar dunga.'",
                        "Show responsibility: 'Maine apna saara college/office kaam advance me complete kar liya hai.'",
                        "Promise updates: 'Waha pahunchte hi call aur live location bhej dunga.'"
                    ),
                    closingLine = "Aap bolo to sabhi doston ke parents se baat karwa du? Love you and thank you!"
                )
            }
            else -> {
                ActionCard.CallScriptAction(
                    title = "Professional & Confident Calling Script",
                    openingLine = "Namaste/Hello! Hope you are having a productive day.",
                    keyPoints = listOf(
                        "Clarify purpose directly in first 10 seconds.",
                        "Ask if it is a convenient time to speak.",
                        "State your request or offer with concise clarity."
                    ),
                    closingLine = "Thank you for your valuable time. I will send you a quick WhatsApp summary right away!"
                )
            }
        }
    }

    private fun generateConversationalReply(lower: String): ChatMessage {
        val replyText = when {
            lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Namaste! 🙏 Mai hu Mitra AI - aapka personal smart assistant. Mai aapke liye:\n\n" +
                        "📞 Phone calls laga sakta hu\n" +
                        "💬 WhatsApp chats & messages draft karke open kar sakta hu\n" +
                        "📸 Instagram ke liye viral comments & captions likh sakta hu\n" +
                        "🎙️ Voice commands sun sakta hu\n\n" +
                        "Boliye, aaj mai aapki kya madad karu?"

            lower.contains("kaise ho") || lower.contains("how are you") ->
                "Mai bilkul badhiya hu! Aapke phone tasks ko aasan banane ke liye ready hu. Aap bataiye, aaj kisko call ya message karna hai?"

            lower.contains("kya kar sakte ho") || lower.contains("features") || lower.contains("help") ->
                "Mai ye saare kaam चुटकियों me kar sakta hu:\n\n" +
                        "1. **Quick Calling**: 'Rahul ko call lagao' ya number bolo, dialer open ho jayega.\n" +
                        "2. **WhatsApp Smart Sender**: Message generate karke direct 1-tap me WhatsApp bhej do.\n" +
                        "3. **Instagram Comment Crafter**: Kisi bhi post/reel ke liye mast funny ya aesthetic comment banao aur Instagram me paste karo.\n" +
                        "4. **Call Scripts**: Important calls se pehle baat karne ke points sikho."

            else ->
                "Main samajh gaya! Aap chahein to voice mic dabakar bolein, ya neeche diye options me se chunein. Mai turant action card generate kar dunga!"
        }

        return ChatMessage(
            text = replyText,
            isFromUser = false,
            suggestedFollowUps = listOf(
                "Mummy ko call lagao",
                "WhatsApp pe 'Late hu' message bhej do",
                "Instagram gym post comment",
                "Leave application script"
            )
        )
    }
}
