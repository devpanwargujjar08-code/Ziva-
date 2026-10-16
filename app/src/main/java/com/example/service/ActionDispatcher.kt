package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

object ActionDispatcher {

    /**
     * Dials a phone number using the system dialer.
     * Does not require runtime permissions.
     */
    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) {
            Toast.makeText(context, "Phone number is empty", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Directly places a call if CALL_PHONE permission is granted, otherwise dials.
     */
    fun callPhoneNumber(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) {
            Toast.makeText(context, "Phone number is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        try {
            val action = if (hasPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            dialPhoneNumber(context, phoneNumber)
        }
    }

    /**
     * Opens WhatsApp chat with prefilled text and optional phone number.
     * Handles phone numbers with or without country code.
     */
    fun openWhatsApp(context: Context, rawPhoneNumber: String?, message: String) {
        try {
            val cleanPhone = rawPhoneNumber?.replace(Regex("[^0-9]"), "") ?: ""
            // Format phone: if 10 digits (typical India number), prepend 91 for WhatsApp international format
            val formattedPhone = when {
                cleanPhone.length == 10 -> "91$cleanPhone"
                cleanPhone.startsWith("0") && cleanPhone.length == 11 -> "91${cleanPhone.substring(1)}"
                cleanPhone.isNotEmpty() -> cleanPhone
                else -> ""
            }

            if (formattedPhone.isNotEmpty()) {
                val encodedMsg = Uri.encode(message)
                val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback without package restriction (e.g. WhatsApp Business or browser)
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
            } else {
                // No phone number: open WhatsApp with share text
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val shareIntent = Intent.createChooser(intent, "Share via WhatsApp").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(shareIntent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies comment or caption to clipboard and opens Instagram app or web.
     */
    fun openInstagram(context: Context, textToCopy: String? = null) {
        if (!textToCopy.isNullOrBlank()) {
            copyToClipboard(context, textToCopy, "Instagram text copied! Paste it in comments.")
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://instagram.com/")
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Instagram app not installed, open in browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open Instagram: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Copies text to system clipboard and gives user immediate feedback.
     */
    fun copyToClipboard(context: Context, text: String, feedbackMessage: String = "Copied to clipboard!") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Mitra AI", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Copy failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generic share text to any installed app.
     */
    fun shareText(context: Context, text: String, chooserTitle: String = "Share text via") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
