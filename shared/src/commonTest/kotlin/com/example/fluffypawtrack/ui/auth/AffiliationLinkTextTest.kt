package com.example.fluffypawtrack.ui.auth

import androidx.compose.ui.text.style.TextDecoration
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import kotlin.test.Test
import kotlin.test.assertEquals

class AffiliationLinkTextTest {

    @Test
    fun onlySolicitaTuAfiliacionIsStyledAsALink() {
        val text = affiliationLinkText()

        assertEquals("¿Eres un albergue o centro de adopción? Solicita tu afiliación", text.text)
        val span = text.spanStyles.single()
        assertEquals("Solicita tu afiliación", text.text.substring(span.start, span.end))
        assertEquals(FluffySemanticColors.Link, span.item.color)
        assertEquals(TextDecoration.Underline, span.item.textDecoration)
    }
}
