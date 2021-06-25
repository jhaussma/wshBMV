package de.wsh.wshbmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.ItemTransferlistBinding
import de.wsh.wshbmv.db.entities.TbmvBeleg
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.relations.BelegAndZielort
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class TransferlistAdapter(
    private val listener: TransferlistAdapter.OnItemClickListener
) : RecyclerView.Adapter<TransferlistAdapter.TransferlistViewHolder>() {

    inner class TransferlistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Einbindung des OnclickListeners für das Recyclerview beginnt hier und dann weiter über das Interface ganz unten...
        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val position: Int = adapterPosition
            Timber.tag(TAG).d("TransferlistAdapter, TransferlistViewHolder, onClick wurde aktiviert für Position $position")
            if (position != RecyclerView.NO_POSITION) {
                val belegAndZielort = differ.currentList[position]
                listener.onTransferlistItemClick(belegAndZielort)
            }
        }
    }

    private lateinit var bind: ItemTransferlistBinding

    private val diffCallback = object : DiffUtil.ItemCallback<BelegAndZielort>() {
        override fun areItemsTheSame(oldItem: BelegAndZielort, newItem: BelegAndZielort): Boolean {
            return oldItem.tmbvBeleg.id == newItem.tmbvBeleg.id
        }

        override fun areContentsTheSame(oldItem: BelegAndZielort, newItem: BelegAndZielort): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    var differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<BelegAndZielort>) = differ.submitList(list)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferlistViewHolder {
        // erzeuge die Sicht eines Eintrags aus Layout item_overview...
        return TransferlistViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transferlist,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TransferlistViewHolder, position: Int) {
        // befülle die Sicht eines Datensatz-Eintrags aus der Transferlistenliste
        val myTransferList = differ.currentList[position]
        bind = ItemTransferlistBinding.bind(holder.itemView)
        holder.itemView?.apply {
            // hier erfolgt die Datenübergabe in die Zeile
            when (myTransferList.tmbvBeleg.belegTyp) {
                "Transfer" -> Glide.with(this).load(R.drawable.ic_transfer).into(bind.ivTfListTyp)
                "Korrektur" -> Glide.with(this).load(R.drawable.ic_typ_manuell).into(bind.ivTfListTyp)
                "Inventur" -> Glide.with(this).load(R.drawable.ic_typ_invetur).into(bind.ivTfListTyp)
                else -> Glide.with(this).load("").into(bind.ivTfListTyp)
            }
            when (myTransferList.tmbvBeleg.belegStatus) {
                "In Arbeit" -> Glide.with(this).load(R.drawable.ic_stat_arbeit).into(bind.ivTfListStatus)
                "Erfasst" -> Glide.with(this).load(R.drawable.ic_stat_ready).into(bind.ivTfListStatus)
                "Fertig" -> Glide.with(this).load(R.drawable.ic_stat_done).into(bind.ivTfListStatus)
                "Storniert" -> Glide.with(this).load(R.drawable.ic_stat_abort).into(bind.ivTfListStatus)
                else -> Glide.with(this).load("").into(bind.ivTfListStatus)
            }
            bind.tvTfListName.text = myTransferList.tmbvBeleg.belegDatum?.formatedDateDE()
            bind.tvTfListZielort.text = myTransferList.tbmvLager.matchcode
            bind.tvTfListNotiz.text = myTransferList.tmbvBeleg.notiz
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    /** ##########################################################################################################
     *   Übergabe des OnClick-Listeners des RecyclerView an ein View-Element (der Adapter selbst ist das ja nicht!)
     */
    interface OnItemClickListener {
        fun onTransferlistItemClick(belegAndZielort: BelegAndZielort)
    }

    // deutsche Datumsformatierung
    private fun Date.formatedDateDE(): String {
        var simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return simpleDateFormat.format(this)
    }

}