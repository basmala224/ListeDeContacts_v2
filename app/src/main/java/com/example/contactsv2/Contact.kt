package com.example.contactsv2

import android.os.Bundle
import androidx.annotation.DrawableRes

/**
 * Un contact de la liste.
 *
 * @property photoRes photo par défaut, dans res/drawable
 * @property cheminPhoto fichier de la photo choisie ou prise, ou null s'il n'y en a pas
 */
data class Contact(
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String,
    @DrawableRes val photoRes: Int,
    val cheminPhoto: String? = null
) {
    /** Range le contact dans un Bundle pour le transmettre dans un Intent ou le sauvegarder. */
    fun versBundle() = Bundle().apply {
        putString(CLE_NOM, nom)
        putString(CLE_PRENOM, prenom)
        putString(CLE_TELEPHONE, telephone)
        putString(CLE_EMAIL, email)
        putInt(CLE_PHOTO_RES, photoRes)
        putString(CLE_CHEMIN_PHOTO, cheminPhoto)
    }

    companion object {
        private const val CLE_NOM = "nom"
        private const val CLE_PRENOM = "prenom"
        private const val CLE_TELEPHONE = "telephone"
        private const val CLE_EMAIL = "email"
        private const val CLE_PHOTO_RES = "photo_res"
        private const val CLE_CHEMIN_PHOTO = "chemin_photo"

        fun depuisBundle(bundle: Bundle) = Contact(
            nom = bundle.getString(CLE_NOM).orEmpty(),
            prenom = bundle.getString(CLE_PRENOM).orEmpty(),
            telephone = bundle.getString(CLE_TELEPHONE).orEmpty(),
            email = bundle.getString(CLE_EMAIL).orEmpty(),
            photoRes = bundle.getInt(CLE_PHOTO_RES),
            cheminPhoto = bundle.getString(CLE_CHEMIN_PHOTO)
        )
    }
}
