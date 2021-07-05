package de.wsh.wshbmv.ui.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MaterialFragment : Fragment(R.layout.fragment_material)  {

    private lateinit var bind: FragmentMaterialBinding

    private val viewModel: MaterialViewModel by activityViewModels()

    private var materialId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG).d("MaterialFragment, onCreate")

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentMaterialBinding.bind(view)
        (activity as AppCompatActivity).supportActionBar?.title = "BM-Details"
        viewModel.bmDataLive.observe(viewLifecycleOwner, {
            if (it != null) {
                writeUiValues(it)
            }
        })

        viewModel.barcodeNotFound.observe(viewLifecycleOwner, {
            if (it == true) {
                Toast.makeText(requireContext(),"Barcode ist unbekannt oder fehlende Berechtigung!",Toast.LENGTH_LONG).show()
            }
        })

        bind.btServiceDetails.setOnClickListener {
            Timber.tag(TAG).d("btServiceDetails wurde gedrückt, NOCH NICHT IMPlEMENTIERT!!!")
        }

        materialId = arguments?.getString("materialId")
        Timber.tag(TAG).d("MaterialFragment, onViewCreated mit Parameter Material-ID: $materialId")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.material_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.miAddMaterial).isVisible = false
        menu.findItem(R.id.miMatAddPhoto).isVisible = (myUser?.bmvAdmin != 0)
        menu.findItem(R.id.miBarcode).isVisible = false

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miBarcode -> Toast.makeText(requireContext(), "Barcode im Fragment geklickt", Toast.LENGTH_SHORT)
                .show()
            R.id.miMatAddPhoto -> {
                // wird schon im MainActivity abgehandelt!!!
                Toast.makeText(requireContext(),"Foto importieren", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Import einer neuen Photo-Datei aus dem MainActivity heraus
     */
    fun importNewPhoto(bitmap: Bitmap) {viewModel.importNewPhoto(bitmap)}

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Import eines Barcodes aus der ScanActivity/MainActivity
     */
    fun importNewBarcode(barcode: String) {
        Timber.tag(TAG).d("MaterialFragment hat Barcode $barcode empfangen...")
        viewModel.setNewMaterialIdByScancode(barcode)
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Aktualisiert alle Objekte mit den Werten aus dem Datensatz bmData (MaterialViewModel-Observer)
     */
    // schreib die Dateninhalte in die Maske
    private fun writeUiValues(bmData: BmData) {
        bind.tvMatTyp.text = bmData.tbmvMat?.typ
        bind.tvMatStatus.text = bmData.tbmvMat?.matStatus
        bind.tvMatMatchcode.text = bmData.tbmvMat?.matchcode
        bind.tvMatScancode.text = bmData.tbmvMat?.scancode
        if (bmData.nextServiceDatum == null) {
            bind.tvMatServiceDatum.text = ""
        }else {
            bind.tvMatServiceDatum.text = bmData.nextServiceDatum?.formatedDateDE()
        }
        bind.tvMatHersteller.text = bmData.tbmvMat?.hersteller
        bind.tvMatModell.text = bmData.tbmvMat?.modell
        bind.tvMatSeriennr.text = bmData.tbmvMat?.seriennummer
        bind.tvMatBeschreibung.text = bmData.tbmvMat?.beschreibung
        bind.tvMatGruppe.text = bmData.tbmvMatGruppe?.matGruppe
        bind.tvMatVerantwortlich.text = bmData.tsysUser?.userKennung
        bind.tvMatLager.text = bmData.matLager?.matchcode
        bind.tvMatHauptlager.text = bmData.matHautpLager?.matchcode

        // bei Serviceinfos ggf. durch Farbe auf kritischen Zustand hinweisen
        bind.tvMatServiceDatum.setBackgroundColor(Color.TRANSPARENT)
        if (bmData.nextServiceDatum != null) {
            if (bmData.nextServiceDatum!! < Date()) {
                bind.tvMatServiceDatum.setBackgroundColor(Color.RED)
            }
        }
        // wir binden das Bild noch mit ein
        Glide.with(this).load(bmData.tbmvMat?.bildBmp).into(bind.ivMatBild)
    }


    // deutsche Datumsformatierung
    private fun Date.formatedDateDE(): String {
        var simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return simpleDateFormat.format(this)
    }
}