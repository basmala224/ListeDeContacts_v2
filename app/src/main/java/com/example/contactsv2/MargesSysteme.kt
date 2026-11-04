package com.example.contactsv2

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Garde le contenu hors des barres système et du clavier.
 * L'en-tête s'étend sous la barre d'état, et son texte reste en dessous.
 */
fun appliquerMargesSysteme(racine: View, entete: View) {
    val margeHautEntete = entete.paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(racine) { _, insets ->
        val marges = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime()
        )
        racine.updatePadding(left = marges.left, right = marges.right, bottom = marges.bottom)
        entete.updatePadding(top = margeHautEntete + marges.top)
        insets
    }
}
