package de.wsh.wshbmv.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.ContentProviderCompat.requireContext
import de.wsh.wshbmv.databinding.DialogAddBelegBinding
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*

class AddBelegDialog(context: Context, var addDialogListener: AddDialogListener, var mainRepo: MainRepository) :
    AppCompatDialog(context) {

    private lateinit var bind: DialogAddBelegBinding

    private lateinit var lagerListe: List<TbmvLager>
    private var selectedLager: TbmvLager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        bind = DialogAddBelegBinding.inflate(layoutInflater)
        setContentView(bind.root)
        Timber.tag(TAG).d("AddBelegDialog, onCreate")


        // initialisiere die Lager-Auswahlliste
        initZiellagerAuswahl()

        val adapter = LagerAdapter(context, android.R.layout.simple_list_item_1, lagerListe)
        bind.actvAddBelegZiellager.setAdapter(adapter)
        bind.actvAddBelegZiellager.threshold = 2

        bind.actvAddBelegZiellager.setOnItemClickListener() { parent, _, position, _ ->
            selectedLager = parent.adapter.getItem(position) as TbmvLager?
            bind.actvAddBelegZiellager.setText(selectedLager?.matchcode)
        }

        // Reaktion auf Okay-Click
        bind.tvAddBelegOK.setOnClickListener {
            // zuerst prüfen, ob alles ausgefüllt ist für eine Anlage ...
            if (selectedLager == null || bind.actvAddBelegZiellager.text.isEmpty()) {
                Toast.makeText(context,"Bitte einen Zielort eingeben", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // wenn alles okay war, wird hier die Auswahl zurückgegeben
            addDialogListener.onAddButtonClicked(selectedLager!!)
            dismiss()
        }

        // Reaktion auf Cancel-Click
        bind.tvAddBelegCancel.setOnClickListener {
            cancel()
        }


    }


    // lädt alle möglichen Lager in die Auswahlliste
    private fun initZiellagerAuswahl() {
//        mainRepo = MainRepository(tbmvDAO)
        val job = GlobalScope.launch(Dispatchers.IO) {
            lagerListe = mainRepo.getLagerListSorted()
        }
        runBlocking { job.join() }
    }

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

//        override fun getItemId(position: Int): Long {
//            return 0
//            // benötigen wir nicht...
//        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: TextView = convertView as TextView? ?: LayoutInflater.from(context)
                .inflate(layoutResource, parent, false) as TextView
            view.text = "${mLagers[position].typ}: ${mLagers[position].matchcode} "
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

                override fun performFiltering(charSequence: CharSequence?): Filter.FilterResults {
                    val queryString = charSequence?.toString()?.lowercase(Locale.getDefault())

                    val filterResults = Filter.FilterResults()
                    filterResults.values = if (queryString == null || queryString.isEmpty())
                        allLagers
                    else
                        allLagers.filter {
                            it.typ.lowercase(Locale.getDefault())
                                .contains(queryString) || it.matchcode.lowercase(
                                Locale.getDefault()
                            )
                                .contains(queryString)
                        }
                    return filterResults
                }
            }
        }
    }
}


