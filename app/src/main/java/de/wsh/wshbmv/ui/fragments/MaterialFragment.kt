package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentMaterialBinding
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.ui.viewmodels.MaterialViewModel
import de.wsh.wshbmv.ui.viewmodels.OverviewViewModel
import timber.log.Timber

@AndroidEntryPoint
class MaterialFragment : Fragment(R.layout.fragment_material) {

    private lateinit var bind: FragmentMaterialBinding

    private val viewModel: MaterialViewModel by activityViewModels()

    private var materialId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        materialId = arguments?.getString("materialId")
        Timber.tag(TAG).d("Material-ID: $materialId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentMaterialBinding.bind(view)

//        materialId = arguments?.getString("materialId")
//        Timber.tag(TAG).d("Material-ID: $materialId")

        Timber.tag(TAG).d("wir starten das ViewModel")

        val bmData = materialId?.let { viewModel.getBmDataToMaterialId(it) }
        if (bmData != null) {
            Timber.tag(TAG).d("ich sehe Land: ${bmData.tbmvMat.toString()}")
        } else {
            Timber.tag(TAG).d("hier wurde das Material nicht gefunden!!!")
        }

        viewModel.bmData.observe(viewLifecycleOwner, { bmData ->
            Timber.tag(TAG).d("bmData wurden geändert: ${bmData.tbmvMat.toString()}")

        })

        viewModel.country.observe(viewLifecycleOwner, {country ->
            Timber.tag(TAG).d("Land geändert in: $country")
        })

        bind.btServiceDetails.setOnClickListener {
            viewModel.saveCountry("Germany")
        }


    }

}