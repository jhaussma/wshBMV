package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.MyApplication
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.BelegposAdapter
import de.wsh.wshbmv.databinding.FragmentBelegBinding
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BelegposAndMaterialAndLager
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.myUser
import de.wsh.wshbmv.ui.viewmodels.BelegViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class BelegFragment : Fragment(R.layout.fragment_beleg), BelegposAdapter.OnItemClickListener {


    private val belegViewModel: BelegViewModel by activityViewModels()

    private lateinit var belegposAdapter: BelegposAdapter

    private lateinit var bind: FragmentBelegBinding

    private lateinit var delMenuItem: MenuItem
    private lateinit var addMenuItem: MenuItem
    private lateinit var saveMenuItem: MenuItem

    private var belegId: String? = null
    private var ignoreNotizChange = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG).d("BelegFragment, onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // initialisiere das Binding
        bind = FragmentBelegBinding.bind(view)

        (activity as AppCompatActivity).supportActionBar?.title = "Beleg"
        belegId = arguments?.getString("belegId")
        Timber.tag(TAG).d("BelegFragment, onViewCreated mit Parameter Beleg-ID $belegId")

        belegViewModel.clearBelegData()

        setupRecyclerView()
        bind.fabSave.isVisible = false
        bind.fabUndo.isVisible = false

        belegViewModel.belegDataLive.observe(viewLifecycleOwner, {
            if (it != null) {
                Timber.tag(TAG)
                    .d("belegViewModel.belegDataLive.observer meldet neue Anzeigedaten: ${it.tbmvBeleg?.id}")
                belegId = it.tbmvBeleg?.id
                writeUiValues(it)
                belegposAdapter.notifyDataSetChanged()
                // wir bestimmen noch den richtigen Bearbeitungsstatus des Belegs
                setVisiblesToFragment()
            }
        })

        belegViewModel.getBelegposVonBeleg(belegId!!).observe(viewLifecycleOwner, Observer {
            belegposAdapter.belegPosn = it
            belegposAdapter.notifyDataSetChanged()
        })

        bind.etBelegNotiz.doAfterTextChanged {
            if (ignoreNotizChange) {
                ignoreNotizChange = false
            } else {
                bind.fabSave.isVisible =
                    (bind.etBelegNotiz.text.toString() != belegViewModel.belegDataLive.value?.tbmvBeleg?.notiz)
                bind.fabUndo.isVisible = bind.fabSave.isVisible
            }
        }

        bind.fabSave.setOnClickListener {
            Timber.tag(TAG).d("fabSave wurde gedrückt -> implementieren")
        }

        bind.fabUndo.setOnClickListener {
            Timber.tag(TAG).d("fabUndo gedrückt -> implementieren...")
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).d("BelegFragment, onResume...")
        // nun aktivieren wir die Anzeige..
        belegViewModel.setNewBelegId(belegId!!)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.beleg_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
        Timber.tag(TAG).d("BelegFragment, OnCreateOptionsMenu...")
        menu.getItem(0).isVisible = false
        addMenuItem = menu.findItem(R.id.miBarcode)
        addMenuItem.isVisible = false
        delMenuItem = menu.findItem(R.id.miBelegDel)
        delMenuItem.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miBelegDel -> {
                // lösche einen (Transfer-)Beleg aus der Belegliste, und raus aus dem Fragment
                belegViewModel.deleteBeleg(belegViewModel.belegDataLive.value?.tbmvBeleg!!)
                parentFragmentManager.popBackStack()
            }
        }
        return true
    }

    override fun onBelegposlistItemClick(belegPosMaterialLager: BelegposAndMaterialAndLager) {
        Timber.tag(TAG)
            .d("BelegFragment, onBelegposListItemClick mit BelegposID: ${belegPosMaterialLager.tbmvBelegPos.id}")

    }

    private fun setupRecyclerView() = bind.rvBelegPos.apply {
        belegposAdapter = BelegposAdapter(listOf(), this@BelegFragment)
        adapter = belegposAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }


    /**
     *  steuert die Sichtbarkeit / Freigabe der Elemente im Fragement Belege
     */
    private fun setVisiblesToFragment() {
        val belegData = belegViewModel.belegDataLive.value!!

        bind.etBelegNotiz.isVisible =
            (belegData.tbmvBeleg?.belegStatus == "In Arbeit" && belegData.tbmvBeleg?.belegUserGuid == myUser!!.id)
                    || (belegData.tbmvBeleg?.belegStatus == "Erfasst" && belegData.tbmvBeleg?.belegUserGuid == myUser!!.id)
                    || (belegData.tbmvBeleg?.belegStatus == "Erfasst" && belegData.zielUser?.id == myUser!!.id)
        bind.tvBelegNotiz.isVisible = !bind.etBelegNotiz.isVisible
        delMenuItem.isVisible = (belegposAdapter.itemCount == 0)
        addMenuItem.isVisible =
            (belegData.tbmvBeleg?.belegStatus == "In Arbeit" && belegData.tbmvBeleg?.belegUserGuid == myUser!!.id)


    }

    /**
     *  aktualisiert die Anzeigenfelder im Fragment Belege
     */
    private fun writeUiValues(belegData: BelegData) {
        ignoreNotizChange = true
        bind.tvBelegTyp.text = belegData.tbmvBeleg?.belegTyp
        bind.tvBelegDatum.text = belegData.tbmvBeleg?.belegDatum?.formatedDateDE()
        bind.tvBelegUser.text = belegData.belegUser?.userKennung
        bind.tvBelegZielort.text = belegData.zielLager?.matchcode
        bind.tvBelegZielUser.text = belegData.zielUser?.userKennung
        bind.tvBelegNotiz.text = belegData.tbmvBeleg?.notiz
        bind.etBelegNotiz.setText(belegData.tbmvBeleg?.notiz)
        bind.tvBelegStatus.text = belegData.tbmvBeleg?.belegStatus
    }


    // deutsche Datumsformatierung
    private fun Date.formatedDateDE(): String {
        var simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return simpleDateFormat.format(this)
    }

}