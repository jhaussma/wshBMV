package de.wsh.wshbmv.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.ui.viewmodels.OverviewViewModel

@AndroidEntryPoint
class MaterialFragment : Fragment(R.layout.fragment_material) {

    private val viewModel: OverviewViewModel by viewModels()

}