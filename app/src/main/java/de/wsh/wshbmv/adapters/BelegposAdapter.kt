package de.wsh.wshbmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.ItemBelegposBinding
import de.wsh.wshbmv.databinding.ItemTransferlistBinding
import de.wsh.wshbmv.db.entities.relations.BelegposAndMaterialAndLager
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class BelegposAdapter(
    var belegPosn: List<BelegposAndMaterialAndLager>,
    private val listener: BelegposAdapter.OnItemClickListener
) : RecyclerView.Adapter<BelegposAdapter.BelegposViewHolder>() {

    inner class BelegposViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Einbindung des OnclickListeners für das Recyclerview beginnt hier und dann weiter über das Interface ganz unten...
        init {
            itemView.setOnClickListener(this)
        }
        override fun onClick(view: View?) {
            val position: Int = adapterPosition
            Timber.tag(TAG).d("BelegposAdapter, BelegposViewHolder, onClick wurde aktiviert für Position $position")
            if (position != RecyclerView.NO_POSITION) {
                val belegposMaterialLager = belegPosn[position]
                listener.onBelegposlistItemClick(belegposMaterialLager)
            }
        }

    }

    private lateinit var bind: ItemBelegposBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BelegposViewHolder {
        // erzeuge die Sicht eines Eintrags aus layout item_belegpos...
        return BelegposViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_belegpos,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BelegposViewHolder, position: Int) {
        // befülle die Sicht eines Datensatz-Eintrags aus der Belegpos-Liste
        val myBelegPos = belegPosn[position]
        bind = ItemBelegposBinding.bind(holder.itemView)
        holder.itemView.apply {
            bind.tvBelegposPos.text = myBelegPos.tbmvBelegPos.pos.toString()
            bind.tvBelegposMatName.text = myBelegPos.tbmvMat?.matchcode
            bind.tvBelegposAckDatum.text = myBelegPos.tbmvBelegPos.ackDatum?.formatedDateDE()
            bind.tvBelegposVon.text = myBelegPos.tbmvLager?.matchcode
        }
    }

    override fun getItemCount(): Int {
        return belegPosn.size
    }


    /** ##########################################################################################################
     *   Übergabe des OnClick-Listeners des RecyclerView an ein View-Element (der Adapter selbst ist das ja nicht!)
     */
    interface OnItemClickListener {
        fun onBelegposlistItemClick(belegPosMaterialLager: BelegposAndMaterialAndLager)
    }


    // deutsche Datumsformatierung
    private fun Date.formatedDateDE(): String {
        var simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return simpleDateFormat.format(this)
    }

}