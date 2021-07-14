package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.TransferlistAdapter
import de.wsh.wshbmv.databinding.FragmentTransferlistBinding
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.relations.BelegAndZielort
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.TransDir
import de.wsh.wshbmv.other.BelegFilterStatus
import de.wsh.wshbmv.repositories.MainRepository
import de.wsh.wshbmv.ui.FragCommunicator
import de.wsh.wshbmv.ui.dialog.AddBelegDialog
import de.wsh.wshbmv.ui.dialog.AddDialogListener
import de.wsh.wshbmv.ui.viewmodels.BelegViewModel
import de.wsh.wshbmv.ui.viewmodels.TransferlistViewModel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TransferlistFragment : Fragment(R.layout.fragment_transferlist),
    TransferlistAdapter.OnItemClickListener {

    private val viewModel: TransferlistViewModel by activityViewModels()
    private val belegViewModel: BelegViewModel by activityViewModels()

    private lateinit var tranferlistAdapter: TransferlistAdapter

    private lateinit var fragCommunicator: FragCommunicator

    private lateinit var bind: FragmentTransferlistBinding

    private var newAddBelegId: String = ""

    @Inject
    lateinit var tbmvDAO: TbmvDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG).d("TransferlistFragment, onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // initialisiere das Binding
        Timber.tag(TAG).d("TransferlistFragment, onViewCreated")
        bind = FragmentTransferlistBinding.bind(view)
        fragCommunicator = activity as FragCommunicator
        (activity as AppCompatActivity).supportActionBar?.title = "Belege"

        setupRecyclerView()

        // initialisiere die Spinner
        when (viewModel.transStatus) {
            BelegFilterStatus.OFFEN -> bind.spTfListStatus.setSelection(0)
            BelegFilterStatus.INARBEIT -> bind.spTfListStatus.setSelection(1)
            BelegFilterStatus.ERLEDIGT -> bind.spTfListStatus.setSelection(2)
            BelegFilterStatus.ALLE -> bind.spTfListStatus.setSelection(3)
        }
        when (viewModel.transDir) {
            TransDir.ANMICH -> bind.spTfListDirection.setSelection(0)
            TransDir.VONMIR -> bind.spTfListDirection.setSelection(1)
        }

        // die Listeners für die Spinner einrichten
        bind.spTfListStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when (pos) {
                    0 -> viewModel.filterStatus(BelegFilterStatus.OFFEN)
                    1 -> viewModel.filterStatus(BelegFilterStatus.INARBEIT)
                    2 -> viewModel.filterStatus(BelegFilterStatus.ERLEDIGT)
                    3 -> viewModel.filterStatus(BelegFilterStatus.ALLE)
                }
            }
        }
        bind.spTfListDirection.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    when (pos) {
                        0 -> viewModel.filterDirection(TransDir.ANMICH)
                        1 -> viewModel.filterDirection(TransDir.VONMIR)
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        // Reaktion auf geänderte Beleglistenübersicht einleiten
        viewModel.belegliste.observe(viewLifecycleOwner, Observer {
            tranferlistAdapter.submitList(it)
        })

        // Reaktion auf neu angelegten Transfer-Beleg einleiten
        belegViewModel.newBelegId.observe(viewLifecycleOwner, Observer {
            if (it != "" && newAddBelegId != it) {
                newAddBelegId = it
                belegViewModel.setNewBelegId(newAddBelegId)
                fragCommunicator.passBelegID(newAddBelegId)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transfer_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
        menu.getItem(0).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miTransAddListe -> {
                // starte den Dialog zur Auswahl eines Lagers
                AddBelegDialog(
                    requireContext(),
                    object : AddDialogListener {
                        override fun onAddButtonClicked(clickItem: TbmvLager) {
                            Timber.tag(TAG).d("onOptionsItemSelected meldet Lagerauswahl ${clickItem.matchcode} zurück...")
                            belegViewModel.addNewBeleg(clickItem)
                        }
                    },
                    MainRepository(tbmvDAO)
                ).show()
            }
        }
        return true
    }

    override fun onTransferlistItemClick(belegAndZielort: BelegAndZielort) {
        Timber.tag(TAG)
            .d("TransferlistFragment, onTransferlistItemClick mit Beleg-ID ${belegAndZielort.tmbvBelege.id}")
//        belegViewModel.setNewBelegId(belegAndZielort.tmbvBelege.id)
        fragCommunicator.passBelegID(belegAndZielort.tmbvBelege.id)
    }

    private fun setupRecyclerView() = bind.rvTransferlists.apply {
        tranferlistAdapter = TransferlistAdapter(this@TransferlistFragment)
        adapter = tranferlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

}