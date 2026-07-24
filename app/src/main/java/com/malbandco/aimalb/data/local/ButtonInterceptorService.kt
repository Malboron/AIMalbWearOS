package com.malbandco.aimalb.data.local

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.malbandco.aimalb.presentation.MainActivity

class ButtonInterceptorService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        
        val prefs = PreferencesManager(this)
        if (!prefs.longPressShortcutEnabled) return false

        // KEYCODE_STEM_1 is often the main button, KEYCODE_STEM_2 is secondary
        if (keyCode == KeyEvent.KEYCODE_STEM_1 || keyCode == KeyEvent.KEYCODE_STEM_2) {
            
            // We check for long press
            // Some devices send a flag, but we also check repeatCount
            if (action == KeyEvent.ACTION_DOWN && event.repeatCount >= 1) {
                launchAppWithTrigger()
                return true // Consume to prevent system long-press action
            }
        }

        return super.onKeyEvent(event)
    }

    private fun launchAppWithTrigger() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("trigger_voice", true)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}
}
