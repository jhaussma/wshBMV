package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
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
import de.wsh.wshbmv.other.TransStatus
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
        bind = FragmentTransferlistBinding.bind(view)
        fragCommunicator = activity as FragCommunicator
        (activity as AppCompatActivity).supportActionBar?.title = "Belege"

        setupRecyclerView()

        // initialisiere die Spinner
        when (viewModel.transStatus) {
            TransStatus.OFFEN -> bind.spTfListStatus.setSelection(0)
            TransStatus.INARBEIT -> bind.spTfListStatus.setSelection(1)
            TransStatus.ERLEDIGT -> bind.spTfListStatus.setSelection(2)
            TransStatus.ALLE -> bind.spTfListStatus.setSelection(3)
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
                    0 -> viewModel.filterStatus(TransStatus.OFFEN)
                    1 -> viewModel.filterStatus(TransStatus.INARBEIT)
                    2 -> viewModel.filterStatus(TransStatus.ERLEDIGT)
                    3 -> viewModel.filterStatus(TransStatus.ALLE)
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

        viewModel.belegliste.observe(viewLifecycleOwner, Observer {
            tranferlistAdapter.submitList(it)
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
                Toast.makeText(
                    requireContext(),
                    "Wir fügen eine neue Transferliste hinzu",
                    Toast.LENGTH_LONG
                ).show()
                // starte den Dialog zur Auswahl eines Lagers
                AddBelegDialog(
                    requireContext(),
                    object : AddDialogListener {
                        override fun onAddButtonClicked(clickItem: TbmvLager) {
                            Timber.tag(TAG).d("onOptionsItemSelected meldet Lagerauswahl ${clickItem.matchcode} zurück...")
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
            .d("TransferlistFragment, onTransferlistItemClick mit Beleg-ID ${belegAndZielort.tmbvBeleg.id}")
        belegViewModel.setNewBelegId(belegAndZielort.tmbvBeleg.id)
        fragCommunicator.passBelegID(belegAndZielort.tmbvBeleg.id)
    }

    private fun setupRecyclerView() = bind.rvTransferlists.apply {
        tranferlistAdapter = TransferlistAdapter(this@TransferlistFragment)
        adapter = tranferlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun addNewBeleg() {

    }
}