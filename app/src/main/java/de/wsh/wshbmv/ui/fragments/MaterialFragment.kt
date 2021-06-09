package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentMaterialBinding

@AndroidEntryPoint
class MaterialFragment : Fragment(R.layout.fragment_material) {

    private lateinit var bind: FragmentMaterialBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentMaterialBinding.bind(view)


    }

}