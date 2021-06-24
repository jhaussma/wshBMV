package de.wsh.wshbmv.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.OverviewAdapter
import de.wsh.wshbmv.databinding.FragmentOverviewBinding
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.Constants
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.hasNewBarcode
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.other.GlobalVars.myLagers
import de.wsh.wshbmv.other.SortType
import de.wsh.wshbmv.ui.FragCommunicator
import de.wsh.wshbmv.ui.viewmodels.MaterialViewModel
import de.wsh.wshbmv.ui.viewmodels.OverviewViewModel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class OverviewFragment : Fragment(R.layout.fragment_overview), OverviewAdapter.OnItemClickListener  {

    @Inject
    lateinit var sharedPref: SharedPreferences

    @JvmField
    @field:[Inject Named("LagerId")]
    var lagerId: String = ""


    private val listViewModel: OverviewViewModel by activityViewModels()
    private val matViewModel: MaterialViewModel by activityViewModels()

    private lateinit var overviewAdapter: OverviewAdapter

    private lateinit var fragCommunicator: FragCommunicator

    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentOverviewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG).d("OverviewFragment, onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag(TAG).d("OverviewFragment, onViewCreated")
        // initialisiere die Binding zu den Layout-Objekten
        bind = FragmentOverviewBinding.bind(view)
        // initialisiere die Kommunikation zwischen den Fragments
        fragCommunicator = activity as FragCommunicator
        (activity as AppCompatActivity).supportActionBar?.title = "Betriebsmittel(BM)"

        setupRecyclerView()
        setupLagerFilter()

        when (listViewModel.sortType) {
            SortType.MATCHCODE -> bind.spMatFilter.setSelection(0)
            SortType.SCANCODE -> bind.spMatFilter.setSelection(1)
            SortType.SERIENNUMMER -> bind.spMatFilter.setSelection(2)
            SortType.HERSTELLER -> bind.spMatFilter.setSelection(3)
            SortType.MODELL -> bind.spMatFilter.setSelection(4)
            SortType.STATUS -> bind.spMatFilter.setSelection(5)
        }

        bind.spMatFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when (pos) {
                    0 -> listViewModel.sortMatliste(SortType.MATCHCODE)
                    1 -> listViewModel.sortMatliste(SortType.SCANCODE)
                    2 -> listViewModel.sortMatliste(SortType.SERIENNUMMER)
                    3 -> listViewModel.sortMatliste(SortType.HERSTELLER)
                    4 -> listViewModel.sortMatliste(SortType.MODELL)
                    5 -> listViewModel.sortMatliste(SortType.STATUS)
                }
            }
        }

        bind.spLager.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                Timber.tag(TAG).d("OverviewFragment, spLager -> onItemSelected mit Lager-Position: $pos")
                myLager = myLagers[pos]
                lagerId = myLager!!.id
                listViewModel.filterMatListe(lagerId)
                writeLagerInfoToSharedPref(lagerId, myLager!!.matchcode)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Timber.tag(TAG).d("OverviewFragment, spLager -> onNothingSelected")
            }
        }

        listViewModel.materials.observe(viewLifecycleOwner, Observer {
            overviewAdapter.submitList(it)
        })

        listViewModel.newTbmvMatFromBarcode.observe(viewLifecycleOwner, Observer {
            Timber.tag(TAG).d("OverviewFragment, listViewModel.newTbmvMatFromBarcode.observe...")
            if (hasNewBarcode) {
                if (it == null) {
                    Toast.makeText(requireContext(),"Barcode ist unbekannt oder fehlende Berechtigung!",Toast.LENGTH_LONG).show()
                } else {
                    onMaterialItemClick(it)
                    hasNewBarcode = false
                }
            }
        })
    }


    private fun setupRecyclerView() = bind.rvOverview.apply {
        overviewAdapter = OverviewAdapter(this@OverviewFragment)
        adapter = overviewAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupLagerFilter() {
        val spLager: Spinner = bind.spLager
        var lagerNames = arrayListOf<String>()
        var myLagerPos = -1
        myLagers.forEach() { item ->
            lagerNames.add("${item.typ.first()}: ${item.matchcode}")
            if (item.id == lagerId) {
                myLagerPos = lagerNames.size - 1
            }
        }
        val arrayAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, lagerNames)
        spLager.adapter = arrayAdapter
        if (lagerNames.size > 1) {
            // wir wählen das aktuell eingestellte Lager aus
            spLager.setSelection(myLagerPos)
        }
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Import eines Barcodes aus der ScanActivity/MainActivity
     */
    fun importNewBarcode(barcode: String) {
        Timber.tag(TAG).d("OverviewFragment, importNewBarcode hat Barcode $barcode empfangen...")
        //sofern der Barcode einem Lager zugeordnet ist, das wir sehen dürfen, zeigen wir die Details im MaterialFragment an...
        listViewModel.getAllowedMaterialFromScancode(barcode)
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  klick auf einen Betriebsmitteleintrag verarbeiten
     */
    override fun onMaterialItemClick(tbmvMat: TbmvMat) {
        Timber.tag(TAG).d("OverviewFragment, onMaterialItemClick mit Mat-ID ${tbmvMat.id}")
        matViewModel.setNewMaterialId(tbmvMat.id)
        fragCommunicator.passBmDataID(tbmvMat.id)
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   speichert eine Änderung der Lager-Voreinstellung in den Preferences der App
     */
    private fun writeLagerInfoToSharedPref(
        lagerId: String,
        lagerName: String
    ) {
        sharedPref.edit()
            .putString(Constants.KEY_LAGER_ID, lagerId)
            .putString(Constants.KEY_LAGER_NAME, lagerName)
            .apply()
        return
    }



}