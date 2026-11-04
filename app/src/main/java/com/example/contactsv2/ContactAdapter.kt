package com.example.contactsv2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.contactsv2.databinding.ItemContactBinding

/** Adaptateur de la ListView personnalisée : une ligne avec la photo, le nom et le téléphone. */
class ContactAdapter(
    context: Context,
    private val contacts: MutableList<Contact>
) : ArrayAdapter<Contact>(context, 0, contacts) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // On réutilise une ligne existante quand c'est possible.
        val binding = if (convertView == null) {
            ItemContactBinding.inflate(LayoutInflater.from(context), parent, false).also { nouvelle ->
                nouvelle.imgPhoto.clipToOutline = true
                nouvelle.root.tag = nouvelle
            }
        } else {
            convertView.tag as ItemContactBinding
        }

        val contact = contacts[position]
        binding.tvNomComplet.text = context.getString(
            R.string.nom_complet_format, contact.nom, contact.prenom
        )
        binding.tvTelephone.text = contact.telephone
        PhotoUtils.afficher(binding.imgPhoto, contact.cheminPhoto, contact.photoRes)

        return binding.root
    }
}
