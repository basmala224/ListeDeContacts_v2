package com.example.contactsv2

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.contactsv2.databinding.ActivityDetailBinding
import java.io.File

/** Écran de détail : modification du contact, appel, e-mail et photo. */
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var contactInitial: Contact

    /** Photo en cours de modification : elle n'est transmise qu'avec le bouton Enregistrer. */
    private var cheminPhoto: String? = null

    /** Fichier temporaire où l'application caméra enregistre la photo. */
    private var uriPhotoCamera: Uri? = null

    private val lanceurGalerie = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            afficherMessage(R.string.msg_photo_annulee)
        } else {
            importerPhoto(uri)
        }
    }

    private val lanceurCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { photoPrise ->
        val uri = uriPhotoCamera
        if (photoPrise && uri != null) {
            importerPhoto(uri)
        } else {
            afficherMessage(R.string.msg_photo_annulee)
        }
        // La photo a été copiée : le fichier temporaire ne sert plus.
        uri?.let { contentResolver.delete(it, null, null) }
        uriPhotoCamera = null
    }

    private val lanceurPermissionCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { autorisee ->
        if (autorisee) {
            lancerCamera()
        } else {
            afficherMessage(R.string.msg_camera_refusee)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appliquerMargesSysteme(binding.racine, binding.entete)
        binding.imgPhoto.clipToOutline = true

        val donneesContact = intent.getBundleExtra(EXTRA_CONTACT)
        if (donneesContact == null) {
            finish()
            return
        }
        contactInitial = Contact.depuisBundle(donneesContact)

        if (savedInstanceState == null) {
            remplirChamps()
            cheminPhoto = contactInitial.cheminPhoto
        } else {
            // Android restaure lui-même le texte des EditText.
            cheminPhoto = savedInstanceState.getString(CLE_CHEMIN_PHOTO)
            uriPhotoCamera = savedInstanceState.getString(CLE_URI_CAMERA)?.let { Uri.parse(it) }
        }
        afficherPhoto()

        binding.btnAppeler.setOnClickListener { appeler() }
        binding.btnEmail.setOnClickListener { envoyerEmail() }
        binding.btnGalerie.setOnClickListener { choisirPhoto() }
        binding.btnCamera.setOnClickListener { prendrePhoto() }
        binding.btnEnregistrer.setOnClickListener { enregistrer() }
        // Sans setResult, MainActivity reçoit RESULT_CANCELED et ne change rien.
        binding.btnAnnuler.setOnClickListener { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CLE_CHEMIN_PHOTO, cheminPhoto)
        outState.putString(CLE_URI_CAMERA, uriPhotoCamera?.toString())
    }

    private fun remplirChamps() {
        binding.etNom.setText(contactInitial.nom)
        binding.etPrenom.setText(contactInitial.prenom)
        binding.etTelephone.setText(contactInitial.telephone)
        binding.etEmail.setText(contactInitial.email)
    }

    private fun afficherPhoto() {
        PhotoUtils.afficher(binding.imgPhoto, cheminPhoto, contactInitial.photoRes)
    }

    private fun importerPhoto(uri: Uri) {
        val nouveauChemin = PhotoUtils.importer(this, uri)
        if (nouveauChemin == null) {
            afficherMessage(R.string.msg_photo_illisible)
        } else {
            cheminPhoto = nouveauChemin
            afficherPhoto()
        }
    }

    /** Intent implicite : ouvre le composeur avec le numéro, sans lancer l'appel. */
    private fun appeler() {
        if (!telephoneValide()) return
        val numero = binding.etTelephone.text.toString().trim()
        lancerIntentImplicite(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numero, null)))
    }

    /** Intent implicite : ouvre une messagerie avec l'adresse du contact préremplie. */
    private fun envoyerEmail() {
        if (!emailValide()) return
        val adresse = binding.etEmail.text.toString().trim()
        lancerIntentImplicite(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$adresse")))
    }

    private fun lancerIntentImplicite(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            afficherMessage(R.string.msg_aucune_application)
        }
    }

    private fun choisirPhoto() {
        val demande = PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
            .build()
        try {
            lanceurGalerie.launch(demande)
        } catch (e: ActivityNotFoundException) {
            afficherMessage(R.string.msg_aucune_application)
        }
    }

    /** Vérifie la présence d'une caméra et l'autorisation avant de l'ouvrir. */
    private fun prendrePhoto() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            afficherMessage(R.string.msg_camera_indisponible)
            return
        }
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            lancerCamera()
        } else {
            lanceurPermissionCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun lancerCamera() {
        val dossier = File(cacheDir, "camera").apply { mkdirs() }
        val fichier = File(dossier, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", fichier)
        uriPhotoCamera = uri
        try {
            lanceurCamera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            uriPhotoCamera = null
            afficherMessage(R.string.msg_camera_indisponible)
        }
    }

    /** Valide les champs, puis renvoie le contact modifié à MainActivity. */
    private fun enregistrer() {
        // « and » vérifie tous les champs, pour signaler toutes les erreurs en une fois.
        val champsValides = champRempli(binding.etNom) and
            champRempli(binding.etPrenom) and
            telephoneValide() and
            emailValide()
        if (!champsValides) return

        val contactModifie = contactInitial.copy(
            nom = binding.etNom.text.toString().trim(),
            prenom = binding.etPrenom.text.toString().trim(),
            telephone = binding.etTelephone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            cheminPhoto = cheminPhoto
        )
        val resultat = Intent()
            .putExtra(EXTRA_POSITION, intent.getIntExtra(EXTRA_POSITION, -1))
            .putExtra(EXTRA_CONTACT, contactModifie.versBundle())
        setResult(RESULT_OK, resultat)
        finish()
    }

    private fun champRempli(champ: EditText): Boolean {
        if (champ.text.toString().isBlank()) {
            champ.error = getString(R.string.erreur_champ_obligatoire)
            return false
        }
        return true
    }

    private fun telephoneValide(): Boolean {
        if (!champRempli(binding.etTelephone)) return false
        val numero = binding.etTelephone.text.toString().trim()
        if (!Patterns.PHONE.matcher(numero).matches()) {
            binding.etTelephone.error = getString(R.string.erreur_telephone_invalide)
            return false
        }
        return true
    }

    private fun emailValide(): Boolean {
        if (!champRempli(binding.etEmail)) return false
        val adresse = binding.etEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(adresse).matches()) {
            binding.etEmail.error = getString(R.string.erreur_email_invalide)
            return false
        }
        return true
    }

    private fun afficherMessage(idMessage: Int) {
        Toast.makeText(this, idMessage, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_POSITION = "com.example.contactsv2.POSITION"
        const val EXTRA_CONTACT = "com.example.contactsv2.CONTACT"

        private const val CLE_CHEMIN_PHOTO = "chemin_photo"
        private const val CLE_URI_CAMERA = "uri_camera"
    }
}
