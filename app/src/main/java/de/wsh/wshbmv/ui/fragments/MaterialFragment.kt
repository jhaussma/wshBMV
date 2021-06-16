package de.wsh.wshbmv.ui.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.PatternMatcher
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentMaterialBinding
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.myUser
import de.wsh.wshbmv.ui.viewmodels.MaterialViewModel
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@AndroidEntryPoint
class MaterialFragment : Fragment(R.layout.fragment_material)  {

    private lateinit var bind: FragmentMaterialBinding

    private val viewModel: MaterialViewModel by activityViewModels()

    private var materialId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        materialId = arguments?.getString("materialId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentMaterialBinding.bind(view)

        viewModel.bmDataLive.observe(viewLifecycleOwner, {
            writeUiValues(it)
        })

        bind.btServiceDetails.setOnClickListener {
            Timber.tag(TAG).d("btServiceDetails wurde gedrückt...")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.material_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
//        menu.getItem(R.id.miMatAddPhoto).setVisible(myUser?.bmvW != 0)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miBarcode -> Toast.makeText(requireContext(), "Barcode im Fragment geklickt", Toast.LENGTH_SHORT)
                .show()
            R.id.miSync -> Toast.makeText(requireContext(), "Sync im Fragment geklickt", Toast.LENGTH_SHORT).show()
            R.id.miMatAddPhoto -> {
                Toast.makeText(requireContext(),"Foto importieren", Toast.LENGTH_SHORT)
            }
        }
        return true
    }


    // schreib die Dateninhalte in die Maske
    private fun writeUiValues(bmData: BmData) {
        bind.tvMatTyp.text = bmData.tbmvMat?.typ
        bind.tvMatStatus.text = bmData.tbmvMat?.matStatus
        bind.tvMatMatchcode.text = bmData.tbmvMat?.matchcode
        bind.tvMatScancode.text = bmData.tbmvMat?.scancode
        bind.tvMatServiceDatum.text = bmData.nextServiceDatum?.formatedDateDE()
        bind.tvMatHersteller.text = bmData.tbmvMat?.hersteller
        bind.tvMatModell.text = bmData.tbmvMat?.modell
        bind.tvMatSeriennr.text = bmData.tbmvMat?.seriennummer
        bind.tvMatBeschreibung.text = bmData.tbmvMat?.beschreibung
        bind.tvMatGruppe.text = bmData.tbmvMatGruppe?.matGruppe
        bind.tvMatVerantwortlich.text = bmData.tsysUser?.userKennung
        bind.tvMatLager.text = bmData.matLager?.matchcode
        bind.tvMatHauptlager.text = bmData.matHautpLager?.matchcode

        // wir binden das Bild noch mit ein
        Glide.with(this).load(bmData.tbmvMat?.bildBmp).into(bind.ivMatBild)
    }


    // deutsche Datumsformatierung
    private fun Date.formatedDateDE(): String {
        var simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return simpleDateFormat.format(this)

    }
}