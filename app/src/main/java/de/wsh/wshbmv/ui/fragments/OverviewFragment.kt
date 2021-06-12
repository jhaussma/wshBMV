package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.OverviewAdapter
import de.wsh.wshbmv.databinding.FragmentOverviewBinding
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.SortType
import de.wsh.wshbmv.ui.FragCommunicator
import de.wsh.wshbmv.ui.viewmodels.MaterialViewModel
import de.wsh.wshbmv.ui.viewmodels.OverviewViewModel
import timber.log.Timber

@AndroidEntryPoint
class OverviewFragment : Fragment(R.layout.fragment_overview), OverviewAdapter.OnItemClickListener {

    private val listViewModel: OverviewViewModel by activityViewModels()
    private val matViewModel: MaterialViewModel by activityViewModels()

    private lateinit var overviewAdapter: OverviewAdapter

    private lateinit var fragCommunicator: FragCommunicator

    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentOverviewBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentOverviewBinding.bind(view) // initialisiert die Binding zu den Layout-Objekten
        fragCommunicator = activity as FragCommunicator // initialisiert die Kommunikation zwischen den Fragments

        requestPermissions()
        setupRecyclerView()
        overviewAdapter

        when(listViewModel.sortType) {
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
                    0 -> listViewModel.sortMatliste(SortType.MATCHCODE)
                    1 -> listViewModel.sortMatliste(SortType.SCANCODE)
                    2 -> listViewModel.sortMatliste(SortType.SERIENNUMMER)
                    3 -> listViewModel.sortMatliste(SortType.HERSTELLER)
                    4 -> listViewModel.sortMatliste(SortType.MODELL)
                    5 -> listViewModel.sortMatliste(SortType.STATUS)
                }
            }
        }

        listViewModel.materials.observe(viewLifecycleOwner, Observer {
            overviewAdapter.submitList(it)
        })
    }

    private fun requestPermissions() {
        // todo...
    }

    private  fun setupRecyclerView() = bind.rvOverview.apply {
        overviewAdapter = OverviewAdapter(this@OverviewFragment)
        adapter = overviewAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onMaterialItemClick(tbmvMat: TbmvMat) {
        Timber.tag(TAG).d("Item ${tbmvMat.toString()} gecklickt")
        matViewModel.setNewMaterialId(tbmvMat.id)
        fragCommunicator.passBmDataID(tbmvMat.id)
    }

}