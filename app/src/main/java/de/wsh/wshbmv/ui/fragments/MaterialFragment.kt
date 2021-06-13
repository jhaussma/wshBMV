package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.os.PatternMatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentMaterialBinding
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.ui.viewmodels.MaterialViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@AndroidEntryPoint
class MaterialFragment : Fragment(R.layout.fragment_material) {

    private lateinit var bind: FragmentMaterialBinding

    private val viewModel: MaterialViewModel by activityViewModels()

    private var materialId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        materialId = arguments?.getString("materialId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentMaterialBinding.bind(view)
        Timber.tag(TAG).d("wir starten das ViewModel")

        viewModel.bmDataLive.observe(viewLifecycleOwner, {
            Timber.tag(TAG).d("bmData wurden geändert: ${it.tbmvMat.toString()}")
            writeUiValues(it)
        })

        bind.btServiceDetails.setOnClickListener {
            Timber.tag(TAG).d("btServiceDetails wurde gedrückt...")
        }
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