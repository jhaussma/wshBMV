package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
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
import de.wsh.wshbmv.other.SortType
import de.wsh.wshbmv.ui.viewmodels.OverviewViewModel
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel: OverviewViewModel by viewModels()

    private lateinit var overviewAdapter: OverviewAdapter

    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentOverviewBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentOverviewBinding.bind(view) // initialisiert die Binding zu den Layout-Objekten

        requestPermissions()
        setupRecyclerView()

        when(viewModel.sortType) {
            SortType.MATCHCODE -> bind.spMatFilter.setSelection(0)
            SortType.SCANCODE -> bind.spMatFilter.setSelection(1)
            SortType.SERIENNUMMER -> bind.spMatFilter.setSelection(2)
            SortType.HERSTELLER -> bind.spMatFilter.setSelection(3)
            SortType.MODELL -> bind.spMatFilter.setSelection(4)
            SortType.STATUS -> bind.spMatFilter.setSelection(5)
        }

        bind.spMatFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when(pos) {
                    0 -> viewModel.sortMatliste(SortType.MATCHCODE)
                    1 -> viewModel.sortMatliste(SortType.SCANCODE)
                    2 -> viewModel.sortMatliste(SortType.SERIENNUMMER)
                    3 -> viewModel.sortMatliste(SortType.HERSTELLER)
                    4 -> viewModel.sortMatliste(SortType.MODELL)
                    5 -> viewModel.sortMatliste(SortType.STATUS)
                }
            }
        }

        viewModel.materials.observe(viewLifecycleOwner, Observer {
            overviewAdapter.submitList(it)
        })
    }

    private fun requestPermissions() {
        // todo...
    }

    private  fun setupRecyclerView() = bind.rvOverview.apply {
        overviewAdapter = OverviewAdapter()
        adapter = overviewAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }



}