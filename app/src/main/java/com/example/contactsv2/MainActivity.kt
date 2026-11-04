package com.example.contactsv2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.contactsv2.databinding.ActivityMainBinding

/** Écran de la liste des contacts. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ContactAdapter

    private val contacts = mutableListOf(
        Contact("Ben Ali", "Ahmed", "+216 20 000 001", "ahmed.benali@example.com", R.drawable.avatar_1),
        Contact("Trabelsi", "Sarra", "+216 20 000 002", "sarra.trabelsi@example.com", R.drawable.avatar_2),
        Contact("Gharbi", "Mohamed", "+216 20 000 003", "mohamed.gharbi@example.com", R.drawable.avatar_3),
        Contact("Jaziri", "Ines", "+216 20 000 004", "ines.jaziri@example.com", R.drawable.avatar_4),
        Contact("Mansouri", "Youssef", "+216 20 000 005", "youssef.mansouri@example.com", R.drawable.avatar_5),
        Contact("Chaabane", "Amira", "+216 20 000 006", "amira.chaabane@example.com", R.drawable.avatar_6)
    )

    /** Reçoit le résultat de DetailActivity. Après Annuler, le code résultat n'est pas RESULT_OK. */
    private val lanceurDetail = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultat ->
        val donnees = resultat.data
        if (resultat.resultCode == RESULT_OK && donnees != null) {
            val position = donnees.getIntExtra(DetailActivity.EXTRA_POSITION, -1)
            val contactModifie = donnees.getBundleExtra(DetailActivity.EXTRA_CONTACT)
            if (position in contacts.indices && contactModifie != null) {
                contacts[position] = Contact.depuisBundle(contactModifie)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, R.string.msg_contact_enregistre, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appliquerMargesSysteme(binding.racine, binding.entete)

        savedInstanceState?.getBundle(CLE_CONTACTS)?.let { restaurerContacts(it) }

        adapter = ContactAdapter(this, contacts)
        binding.lvContacts.adapter = adapter
        binding.lvContacts.setOnItemClickListener { _, _, position, _ ->
            ouvrirDetail(position)
        }
    }

    /** Ouvre le détail avec un Intent explicite, en transmettant le contact sélectionné. */
    private fun ouvrirDetail(position: Int) {
        val intent = Intent(this, DetailActivity::class.java)
            .putExtra(DetailActivity.EXTRA_POSITION, position)
            .putExtra(DetailActivity.EXTRA_CONTACT, contacts[position].versBundle())
        lanceurDetail.launch(intent)
    }

    /** Conserve les modifications si Android recrée l'activité, par exemple après une rotation. */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val sauvegarde = Bundle()
        contacts.forEachIndexed { indice, contact ->
            sauvegarde.putBundle(indice.toString(), contact.versBundle())
        }
        outState.putBundle(CLE_CONTACTS, sauvegarde)
    }

    private fun restaurerContacts(sauvegarde: Bundle) {
        for (indice in contacts.indices) {
            sauvegarde.getBundle(indice.toString())?.let {
                contacts[indice] = Contact.depuisBundle(it)
            }
        }
    }

    private companion object {
        const val CLE_CONTACTS = "contacts"
    }
}
