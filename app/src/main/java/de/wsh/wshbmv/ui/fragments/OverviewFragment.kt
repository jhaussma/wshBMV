package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.OverviewAdapter
import de.wsh.wshbmv.databinding.FragmentOverviewBinding
import de.wsh.wshbmv.databinding.FragmentSetupBinding
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.ui.viewmodels.OverviewViewModel
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel: OverviewViewModel by viewModels()

    private lateinit var overviewAdapter: OverviewAdapter
    private lateinit var matListe: List<TbmvMat>

    @Inject
    lateinit var tbmvDAO: TbmvDAO


    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentOverviewBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentOverviewBinding.bind(view) // initialisiert die Binding zu den Layout-Objekten

        setupRecyclerView()

        viewModel.materials.observe(viewLifecycleOwner, {
            overviewAdapter.submitList(it)
        })



    }

    private  fun setupRecyclerView() = bind.rvOverview.apply {
        overviewAdapter = OverviewAdapter()
        adapter = overviewAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }



}