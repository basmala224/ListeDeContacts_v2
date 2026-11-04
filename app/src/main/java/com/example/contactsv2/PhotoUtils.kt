package com.example.contactsv2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Fonctions communes pour enregistrer et afficher les photos des contacts. */
object PhotoUtils {

    private const val TAILLE_MAX_PIXELS = 1024
    private const val QUALITE_JPEG = 90

    /**
     * Copie une photo dans le stockage privé de l'application, réduite et bien orientée.
     * Les photos de 12 mégapixels ou plus peuvent sinon faire planter l'affichage.
     *
     * @return le chemin du nouveau fichier, ou null si la photo est illisible
     */
    fun importer(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver

            val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, dimensions) }
            if (dimensions.outWidth <= 0 || dimensions.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculerReduction(dimensions.outWidth, dimensions.outHeight)
            }
            val image = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val rotation = lireRotation(context, uri)
            val imageFinale = tourner(image, rotation)

            val dossier = File(context.filesDir, "photos").apply { mkdirs() }
            val fichier = File(dossier, "contact_${System.currentTimeMillis()}.jpg")
            FileOutputStream(fichier).use {
                imageFinale.compress(Bitmap.CompressFormat.JPEG, QUALITE_JPEG, it)
            }
            fichier.absolutePath
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Affiche la photo enregistrée, ou la photo par défaut s'il n'y en a pas. */
    fun afficher(imageView: ImageView, cheminPhoto: String?, @DrawableRes photoRes: Int) {
        val image = cheminPhoto?.let { BitmapFactory.decodeFile(it) }
        if (image != null) {
            imageView.setImageBitmap(image)
        } else {
            imageView.setImageResource(photoRes)
        }
    }

    private fun calculerReduction(largeur: Int, hauteur: Int): Int {
        var reduction = 1
        while (maxOf(largeur, hauteur) / reduction > TAILLE_MAX_PIXELS) {
            reduction *= 2
        }
        return reduction
    }

    /** Les appareils photo enregistrent souvent l'orientation à part, dans les données EXIF. */
    private fun lireRotation(context: Context, uri: Uri): Float {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun tourner(image: Bitmap, rotation: Float): Bitmap {
        if (rotation == 0f) return image
        val matrice = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrice, true)
    }
}
