package de.wsh.wshbmv.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.ui.viewmodels.MainViewModel

@AndroidEntryPoint
class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel: MainViewModel by viewModels()
}