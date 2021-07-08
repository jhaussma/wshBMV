package de.wsh.wshbmv.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentEditmaterialBinding
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.TbmvMatGruppe
import de.wsh.wshbmv.db.entities.TsysUser
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.ui.viewmodels.MaterialViewModel
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class EditMaterialFragment : Fragment(R.layout.fragment_editmaterial) {

    private lateinit var bind: FragmentEditmaterialBinding

    private val viewModel: MaterialViewModel by activityViewModels()

    private var materialId: String = ""

    private var inInit = true
    private var selLager: TbmvLager? = null
    private var selUser: TsysUser? = null
    private var selMatGruppe: TbmvMatGruppe? = null
    private var selBild: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG).d("EditMaterialFragment, onCreate")
        // initialisiere die Listen für die Auswahlfelder
        viewModel.initSelectsForEditMaterialFragment()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentEditmaterialBinding.bind(view)
        arguments?.let { materialId = it.getString("materialId").toString() }
        if (materialId == "") {
            // Neuanlage, hier wird neu ausgefüllt...
            (activity as AppCompatActivity).supportActionBar?.title = "BM anlegen"
            bind.tvMatTyp.text = "BM"
            bind.tvMatStatus.text = ""
            bind.etMatScancode.setText("")
            bind.etMatScancode.isEnabled = false

        } else {
            // Ändern eines Eintrags...
            (activity as AppCompatActivity).supportActionBar?.title = "BM ändern"
            viewModel.bmDataLive.observe(viewLifecycleOwner, {
                if (it != null) {
                    writeUiValues(it)
                }
            })
        }

        // Auswahllisten laden für Materialgruppenauswahl...
        viewModel.editMatGruppen.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val adapter = MatGruppenAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                viewModel.editMatGruppen.value!!
            )
            bind.actvMatGruppe.setAdapter(adapter)
            bind.actvMatGruppe.threshold = 2
            bind.actvMatGruppe.setOnItemClickListener() { parent, _, position, _ ->
                selMatGruppe = parent.adapter.getItem(position) as TbmvMatGruppe?
                bind.actvMatGruppe.setText(selMatGruppe?.matGruppe)
                bind.actvMatGruppe.error = null
            }
        })
        // ... für Auswahl des Verantwortlichen...
        viewModel.userListe.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val adapter = UserAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                viewModel.userListe.value!!
            )
            bind.actvMatVerantwortlich.setAdapter(adapter)
            bind.actvMatVerantwortlich.threshold = 2
            bind.actvMatVerantwortlich.setOnItemClickListener() { parent, _, position, _ ->
                selUser = parent.adapter.getItem(position) as TsysUser?
                bind.actvMatVerantwortlich.setText(selUser?.userKennung)
                bind.actvMatVerantwortlich.error = null
            }
        })
        // ... und für Hauptlagerauswahl
        viewModel.lagerListe.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val adapter = LagerAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                viewModel.lagerListe.value!!
            )
            bind.actvMatHauptlager.setAdapter(adapter)
            bind.actvMatHauptlager.threshold = 2
            bind.actvMatHauptlager.setOnItemClickListener() { parent, _, position, _ ->
                selLager = parent.adapter.getItem(position) as TbmvLager?
                bind.actvMatHauptlager.setText(selLager?.matchcode)
                bind.actvMatHauptlager.error = null
            }
        })
        inInit = true
        viewModel.newBarcode.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (viewModel.newBarcode.value == "") {
                if (inInit) {
                    inInit = false
                } else {
                    // dieser Barcode darf nicht verwendet werden, wir geben eine Fehlermeldung raus
                    Snackbar.make(
                        requireView(),
                        "Der Scancode ist bereits belegt!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                bind.etMatScancode.setText(viewModel.newBarcode.value)
            }
        })

        // Speichern-/Abbrechen- Reaktionen abnehmen
        bind.tvMatSaveOK.setOnClickListener {
            // wir verifizieren die Eingaben und legen ggf. den Datensatz an
            if (materialId == "") {
                // eine Neuanlage wird durchgeführt
                if (verifyNewUiValues()) {
                    Timber.tag(TAG).d("verifyNewUiValues war erfolgreich")
                    var newBmData = BmData(
                        tbmvMat = TbmvMat(
                            "",
                            scancode = bind.etMatScancode.text.toString(),
                            typ = "BM",
                            matchcode = bind.etMatMatchcode.text.toString(),
                            matGruppeGuid = selMatGruppe!!.id,
                            beschreibung = bind.etMatBeschreibung.text.toString(),
                            hersteller = bind.etMatHersteller.text.toString(),
                            modell = bind.etMatModell.text.toString(),
                            seriennummer = bind.etMatSeriennr.text.toString(),
                            userGuid = selUser!!.id,
                            matStatus = if (selLager == null) {
                                "Vermisst"
                            } else {
                                "Aktiv"
                            },
                            bildBmp = selBild
                        ),
                        matLager = selLager,
                        matHautpLager = selLager
                    )
                    viewModel.insertNewBM(newBmData)
                    // und raus aus dem Fragment
                    getActivity()?.supportFragmentManager?.popBackStack()
                }

            } else {
                // ein Betriebsmittel soll geändert werden
                // aktuell nicht implementiert...

            }

        }


        bind.tvMatSaveCancel.setOnClickListener {
            // wir verwerfen die komplette Eingaben
            getActivity()?.supportFragmentManager?.popBackStack()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.material_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.miBarcode).isVisible = true
        menu.findItem(R.id.miAddMaterial).isVisible = false
        menu.findItem(R.id.miMatAddPhoto).isVisible = true
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Aktualisiert alle Objekte mit den Werten aus dem Datensatz bmData (MaterialViewModel-Observer)
     */
    // schreib die Dateninhalte in die Maske
    private fun writeUiValues(bmData: BmData) {
        bind.tvMatTyp.text = bmData.tbmvMat?.typ
        bind.tvMatStatus.text = bmData.tbmvMat?.matStatus
        bind.etMatMatchcode.setText(bmData.tbmvMat?.matchcode)
        bind.etMatScancode.setText(bmData.tbmvMat?.scancode)
        bind.etMatHersteller.setText(bmData.tbmvMat?.hersteller)
        bind.etMatModell.setText(bmData.tbmvMat?.modell)
        bind.etMatSeriennr.setText(bmData.tbmvMat?.seriennummer)
        bind.etMatBeschreibung.setText(bmData.tbmvMat?.beschreibung)
        bind.actvMatGruppe.setText(bmData.tbmvMatGruppe?.matGruppe)
        bind.actvMatVerantwortlich.setText(bmData.tsysUser?.userKennung)
        bind.actvMatHauptlager.setText(bmData.matHautpLager?.matchcode)

        // wir binden das Bild noch mit ein
        Glide.with(this).load(bmData.tbmvMat?.bildBmp).into(bind.ivMatBild)
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  prüfe für eine Neuanlage, ob alle Mindestdaten ausgefüllt und plausibel sind
     */
    private fun verifyNewUiValues(): Boolean {
        var returnVal = true
        // matchcode ist Pflicht!
        if (bind.etMatMatchcode.text.toString() == "") {
            bind.etMatMatchcode.error = "Matchcode ist Pflichtfeld"
            returnVal = false
        } else {
            bind.etMatMatchcode.error = null
        }
        // Scancode ist Pflicht!
        if (bind.etMatScancode.text.toString() == "") {
            bind.etMatScancode.error = "Scancode ist Pflichtfeld (Barcode)"
            returnVal = false
        } else {
            bind.etMatScancode.error = null
        }
        // Betriebsmittelgruppe ist Pflicht!
        if (selMatGruppe == null) {
            bind.actvMatGruppe.error ="Betriebsmittelgruppe ist Pflichauswahl"
            returnVal = false
        } else {
            bind.actvMatGruppe.error = null
        }
        if (selUser == null) {
            bind.actvMatVerantwortlich.error = "Verantwortliche Person ist Pflichtauswahl"
            returnVal = false
        } else {
            bind.actvMatVerantwortlich.error = null
        }
        // ohne Hauptlagerauswahl wird das Betriebsmittel im Status vermisst angelegt, ist aber nicht Pflicht!
        if (selLager == null) {
            bind.actvMatHauptlager.error = "Ohne Hauptlagerauswahl wird das BM als 'vermisst' angelegt!"
        } else {
            bind.actvMatHauptlager.error = null
        }

        if (!returnVal) {
            Snackbar.make(
                requireView(),
                "Bitte fehlende Daten eingeben...",
                Snackbar.LENGTH_LONG
            ).show()
        }
        return returnVal
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Adapter für die AutoCompleteTextView Materialgruppen (Auswahlliste)
     */
    inner class MatGruppenAdapter(
        context: Context,
        @LayoutRes private val layoutResource: Int,
        private val allMatGroups: List<TbmvMatGruppe>
    ) : ArrayAdapter<TbmvMatGruppe>(context, layoutResource, allMatGroups), Filterable {
        private var mMatGroups: List<TbmvMatGruppe> = allMatGroups

        override fun getCount(): Int {
            return mMatGroups.size
        }

        override fun getItem(position: Int): TbmvMatGruppe? {
            return mMatGroups[position]
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: TextView = convertView as TextView? ?: LayoutInflater.from(context)
                .inflate(layoutResource, parent, false) as TextView
            view.text = mMatGroups[position].matGruppe
            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {

                override fun publishResults(
                    charSequence: CharSequence?,
                    filterResults: Filter.FilterResults
                ) {
                    mMatGroups = filterResults.values as List<TbmvMatGruppe>
                    notifyDataSetChanged()
                }

                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    val queryString = charSequence?.toString()?.lowercase(Locale.getDefault())
                    val filterResults = Filter.FilterResults()
                    filterResults.values = if (queryString == null || queryString.isEmpty())
                        allMatGroups
                    else
                        allMatGroups.filter {
                            it.matGruppe.lowercase(Locale.getDefault()).contains(queryString)
                        }
                    return filterResults
                }
            }
        }
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Adapter für die AutoCompleteTextView User (Auswahlliste)
     */
    inner class UserAdapter(
        context: Context,
        @LayoutRes private val layoutResource: Int,
        private val allUsers: List<TsysUser>
    ) : ArrayAdapter<TsysUser>(context, layoutResource, allUsers), Filterable {
        private var mUsers: List<TsysUser> = allUsers

        override fun getCount(): Int {
            return mUsers.size
        }

        override fun getItem(position: Int): TsysUser? {
            return mUsers[position]
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: TextView = convertView as TextView? ?: LayoutInflater.from(context)
                .inflate(layoutResource, parent, false) as TextView
            view.text = "${mUsers[position].userKennung} (${mUsers[position].nachname})"
            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {

                override fun publishResults(
                    charSequence: CharSequence?,
                    filterResults: Filter.FilterResults
                ) {
                    mUsers = filterResults.values as List<TsysUser>
                    notifyDataSetChanged()
                }

                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    val queryString = charSequence?.toString()?.lowercase(Locale.getDefault())
                    val filterResults = Filter.FilterResults()
                    filterResults.values = if (queryString == null || queryString.isEmpty())
                        allUsers
                    else
                        allUsers.filter {
                            it.userKennung.lowercase(Locale.getDefault())
                                .contains(queryString) || it.nachname.lowercase(
                                Locale.getDefault()
                            )
                                .contains(queryString)
                        }
                    return filterResults
                }
            }
        }
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Adapter für die AutoCompleteTextView Hauptlager (Auswahlliste)
     */
    inner class LagerAdapter(
        context: Context,
        @LayoutRes private val layoutResource: Int,
        private val allLagers: List<TbmvLager>
    ) : ArrayAdapter<TbmvLager>(context, layoutResource, allLagers), Filterable {
        private var mLagers: List<TbmvLager> = allLagers

        override fun getCount(): Int {
            return mLagers.size
        }

        override fun getItem(position: Int): TbmvLager? {
            return mLagers[position]
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: TextView = convertView as TextView? ?: LayoutInflater.from(context)
                .inflate(layoutResource, parent, false) as TextView
            view.text = mLagers[position].matchcode
            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {

                override fun publishResults(
                    charSequence: CharSequence?,
                    filterResults: Filter.FilterResults
                ) {
                    mLagers = filterResults.values as List<TbmvLager>
                    notifyDataSetChanged()
                }

                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    val queryString = charSequence?.toString()?.lowercase(Locale.getDefault())
                    val filterResults = Filter.FilterResults()
                    filterResults.values = if (queryString == null || queryString.isEmpty())
                        allLagers
                    else
                        allLagers.filter {
                            it.matchcode.lowercase(Locale.getDefault()).contains(queryString)
                        }
                    return filterResults
                }
            }
        }
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Import eines Barcodes aus der ScanActivity/MainActivity
     */
    fun importNewBarcode(barcode: String) {
        Timber.tag(TAG).d("EditMaterialFragment hat Barcode $barcode empfangen...")
        viewModel.checkNewMaterialIdByScancode(barcode)
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Import einer neuen Photo-Datei aus dem MainActivity heraus
     */
    fun importNewPhoto(bitmap: Bitmap) {
        if (bitmap != null) {
            selBild = bitmap
            // wir zeigen das Bild gleich an...
            Glide.with(this).load(selBild).into(bind.ivMatBild)
        }
    }


}