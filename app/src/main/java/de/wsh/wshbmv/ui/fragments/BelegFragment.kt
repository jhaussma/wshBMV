package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil.DiffResult.NO_POSITION
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.BelegposAdapter
import de.wsh.wshbmv.databinding.FragmentBelegBinding
import de.wsh.wshbmv.db.entities.TbmvBelegPos
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BelegposAndMaterialAndLager
import de.wsh.wshbmv.other.BelegStatus
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars
import de.wsh.wshbmv.other.GlobalVars.myUser
import de.wsh.wshbmv.ui.viewmodels.BelegViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class BelegFragment : Fragment(R.layout.fragment_beleg), BelegposAdapter.OnItemClickListener {


    private val belegViewModel: BelegViewModel by activityViewModels()

    private lateinit var belegposAdapter: BelegposAdapter

    private lateinit var bind: FragmentBelegBinding

    private lateinit var delMenuItem: MenuItem
    private lateinit var scanMenuItem: MenuItem
    private lateinit var sendMenuItem: MenuItem

    private lateinit var itemTouchHelper: ItemTouchHelper

    private var belegId: String? = null
    private var ignoreNotizChange = false
    private var belegFragStatus = BelegStatus.UNDEFINED

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
                belegId = it.tbmvBeleg?.id
                writeUiValues(it)
                bind.fabSave.isVisible = false
                bind.fabUndo.isVisible = false
                belegposAdapter.notifyDataSetChanged()
                // wir bestimmen noch den richtigen Bearbeitungsstatus des Belegs
                Handler(Looper.getMainLooper()).postDelayed({
                    setVisiblesToFragment()
                }, 200)
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
            // eine Änderung der Notiz soll eingetragen werden
            belegViewModel.updateBelegNotiz(bind.etBelegNotiz.text.toString())
        }

        bind.fabUndo.setOnClickListener {
            // wir setzen den Inhalt der Notiz auf den Anfangswert zurück
            bind.etBelegNotiz.setText(belegViewModel.belegDataLive.value!!.tbmvBeleg!!.notiz)
        }

        belegViewModel.barcodeErrorResponse.observe(viewLifecycleOwner, Observer {
            if (it != "") {
                Toast.makeText(
                    requireContext(), it,
                    Toast.LENGTH_LONG
                ).show()
            }

        })
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
        scanMenuItem = menu.findItem(R.id.miBarcode)
        scanMenuItem.isVisible = false
        delMenuItem = menu.findItem(R.id.miBelegDel)
        delMenuItem.isVisible = false
        sendMenuItem = menu.findItem(R.id.miBelegRelease)
        sendMenuItem.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miBelegDel -> {
                // lösche einen (Transfer-)Beleg aus der Belegliste, und raus aus dem Fragment
                belegViewModel.deleteBeleg(belegViewModel.belegDataLive.value?.tbmvBeleg!!)
                parentFragmentManager.popBackStack()
            }
            R.id.miBelegRelease -> {
                // wir ändern den Status auf Erfasst und raus aus dem Fragment
                belegViewModel.setBelegStatusToReleased()
                Toast.makeText(requireContext(), "Beleg wurde nun versendet.", Toast.LENGTH_SHORT)
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

        itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if (direction == ItemTouchHelper.LEFT) {
                        // Links-Swipe löscht den Datensatz...
                        if (viewHolder.adapterPosition != NO_POSITION) {
                            val pos = viewHolder.adapterPosition
                            val tbmvBelegpos: TbmvBelegPos =
                                belegposAdapter.belegPosn[pos].tbmvBelegPos
                            if (tbmvBelegpos.ackDatum == null) {
                                belegViewModel.deleteBelegPos(tbmvBelegpos)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Das Betriebsmittel wurde bereits quittiert!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    belegposAdapter.notifyDataSetChanged()
                }
            })
        itemTouchHelper.attachToRecyclerView(this)

    }


    /** ############################################################################################
     *  steuert die Sichtbarkeit / Freigabe der Elemente im Fragement Belege
     */
    private fun setVisiblesToFragment() {
        val belegData = belegViewModel.belegDataLive.value!!
        // kläre den Status...
        val statusBefore = belegFragStatus
        when (belegData.tbmvBeleg?.belegStatus) {
            "In Arbeit" -> {
                belegFragStatus = if (belegData.tbmvBeleg?.belegUserGuid == myUser!!.id) {
                    BelegStatus.USEREDIT
                } else {
                    BelegStatus.READONLY
                }
            }
            "Erfasst" -> {
                belegFragStatus = if (belegData.zielLager?.id == GlobalVars.myLager?.id) {
                    BelegStatus.ZIELACK
                } else if (belegData.tbmvBeleg?.belegUserGuid == myUser!!.id) {
                    BelegStatus.USERDELETE
                } else {
                    BelegStatus.READONLY
                }
            }
            "Fertig" -> belegFragStatus = BelegStatus.READONLY
            "Storniert" -> belegFragStatus = BelegStatus.READONLY
            else -> belegFragStatus = BelegStatus.READONLY
        }
        if (statusBefore != belegFragStatus) {
            when (belegFragStatus) {
                BelegStatus.UNDEFINED -> (activity as AppCompatActivity).supportActionBar?.title =
                    "Beleg"
                BelegStatus.USEREDIT ->  (activity as AppCompatActivity).supportActionBar?.title = "Beleg anlegen"
                BelegStatus.USERDELETE -> (activity as AppCompatActivity).supportActionBar?.title = "Beleg korrigieren"
                BelegStatus.ZIELACK -> (activity as AppCompatActivity).supportActionBar?.title = "Beleg bestätigen"
                BelegStatus.READONLY -> (activity as AppCompatActivity).supportActionBar?.title = "Beleg ansehen"
            }
        }

        // Löschen von Belegpositionen per LEFT-Swipe...
        if (belegFragStatus == BelegStatus.USEREDIT || belegFragStatus == BelegStatus.USERDELETE) {
            itemTouchHelper.attachToRecyclerView(bind.rvBelegPos)
        } else {
            // Belegpositionen löschen darf immer nur der BelegUser selbst und nur im Belegstatus "in Arbeit" oder "Erfasst"
            // (und natürlich, wenn das BM noch nicht bestätigt wurde, das wird aber erst beim Löschen selbst überprüft!)
            itemTouchHelper.attachToRecyclerView(null)
        }

        bind.etBelegNotiz.isVisible =
            (belegFragStatus == BelegStatus.USEREDIT) || (belegFragStatus == BelegStatus.ZIELACK) || (belegFragStatus == BelegStatus.USERDELETE)
        bind.tvBelegNotiz.isVisible = !bind.etBelegNotiz.isVisible

        delMenuItem.isVisible =
            (belegposAdapter.itemCount == 0) && (belegData.tbmvBeleg?.belegUserGuid == myUser!!.id)

        scanMenuItem.isVisible =
            (belegFragStatus == BelegStatus.USEREDIT) || (belegFragStatus == BelegStatus.ZIELACK)

        sendMenuItem.isVisible =
            (belegposAdapter.itemCount > 0) && (belegFragStatus == BelegStatus.USEREDIT)


    }

    /** ############################################################################################
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

    /** ############################################################################################
     *  Import eines Barcodes zur Aufnahme eines Betriebsmittels in die BelegPos-Liste
     */
    fun importNewBarcode(barcode: String) {
        Timber.tag(TAG).d("BelegFragment hat Barcode $barcode empfangen...")
        if (belegFragStatus == BelegStatus.USEREDIT) {
            belegViewModel.setNewMaterialIdByScancode(barcode)
        } else if (belegFragStatus == BelegStatus.USEREDIT) {

        }
    }


}