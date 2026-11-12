
package com.photonam.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import android.net.Uri
import android.widget.*
import android.view.Gravity
import android.text.TextWatcher
import android.text.Editable
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var nom: EditText
    private lateinit var recherche: EditText
    private lateinit var liste: LinearLayout
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        afficherInterface()
    }

    private fun afficherInterface() {
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.setPadding(20, 20, 20, 20)
        page.setBackgroundColor(Color.WHITE)

        val titre = TextView(this)
        titre.text = "📷 PhotoNom"
        titre.textSize = 26f
        titre.gravity = Gravity.CENTER
        page.addView(titre)

        val onglets = LinearLayout(this)
        val prendre = Button(this)
        prendre.text = "Prendre photo"
        val chercher = Button(this)
        chercher.text = "Rechercher"
        onglets.addView(prendre, LinearLayout.LayoutParams(0,-2,1f))
        onglets.addView(chercher, LinearLayout.LayoutParams(0,-2,1f))
        page.addView(onglets)

        nom = EditText(this)
        nom.hint = "Nom de la photo"
        page.addView(nom)

        val bouton = Button(this)
        bouton.text = "OUVRIR APPAREIL PHOTO"
        page.addView(bouton)

        recherche = EditText(this)
        recherche.hint = "Rechercher une photo par son nom"
        page.addView(recherche)

        val scroll = ScrollView(this)
        liste = LinearLayout(this)
        liste.orientation = LinearLayout.VERTICAL
        scroll.addView(liste)
        page.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        prendre.setOnClickListener {
            nom.visibility = android.view.View.VISIBLE
            bouton.visibility = android.view.View.VISIBLE
            recherche.visibility = android.view.View.GONE
            liste.visibility = android.view.View.GONE
        }

        chercher.setOnClickListener {
            nom.visibility = android.view.View.GONE
            bouton.visibility = android.view.View.GONE
            recherche.visibility = android.view.View.VISIBLE
            liste.visibility = android.view.View.VISIBLE
            chargerPhotos()
        }

        bouton.setOnClickListener { prendrePhoto() }

        recherche.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                chargerPhotos()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        recherche.visibility = android.view.View.GONE
        liste.visibility = android.view.View.GONE
        setContentView(page)
    }

    private fun prendrePhoto() {
        val nomPhoto = nom.text.toString().trim()
        if (nomPhoto.isEmpty()) {
            Toast.makeText(this, "Donne un nom à la photo", Toast.LENGTH_SHORT).show()
            return
        }

        val propre = nomPhoto.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val valeurs = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, propre + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoNom")
        }

        photoUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valeurs
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        try {
            startActivityForResult(intent, 100)
        } catch (e: Exception) {
            photoUri?.let { contentResolver.delete(it, null, null) }
            Toast.makeText(this, "Appareil photo indisponible", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Photo enregistrée !", Toast.LENGTH_LONG).show()
                nom.setText("")
            } else {
                photoUri?.let { contentResolver.delete(it, null, null) }
            }
            photoUri = null
        }
    }

    private fun chargerPhotos() {
        liste.removeAllViews()

        val colonnes = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH}=?"
        val args = arrayOf("Pictures/PhotoNom/")

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            colonnes, selection, args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val nomPhoto = cursor.getString(1) ?: ""

                if (!nomPhoto.contains(recherche.text.toString(), true))
                    continue

                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                val ligne = LinearLayout(this)
                ligne.orientation = LinearLayout.HORIZONTAL

                val texte = TextView(this)
                texte.text = nomPhoto
                texte.textSize = 16f
                texte.setPadding(10, 20, 10, 20)
                ligne.addView(texte, LinearLayout.LayoutParams(0,-2,1f))

                val ouvrir = Button(this)
                ouvrir.text = "Voir"
                ouvrir.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "image/jpeg")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                ligne.addView(ouvrir)
                liste.addView(ligne)
            }
        }
    }
}
