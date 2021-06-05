package de.wsh.wshbmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.ItemOverviewBinding
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber


class OverviewAdapter : RecyclerView.Adapter<OverviewAdapter.OverviewViewHolder>() {

    inner class OverviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var bind: ItemOverviewBinding

    private val diffCallback = object : DiffUtil.ItemCallback<TbmvMat>() {
        override fun areItemsTheSame(oldItem: TbmvMat, newItem: TbmvMat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TbmvMat, newItem: TbmvMat): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    var differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<TbmvMat>) = differ.submitList(list)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewViewHolder {
        // erzeuge die Sicht eines Eintrags aus Layout item_overview...
        return OverviewViewHolder(
            LayoutInflater.from(parent.context).inflate(
            R.layout.item_overview,
            parent,
            false
            )
        )
    }

    override fun onBindViewHolder(holder: OverviewViewHolder, position: Int) {
        // befülle die Sicht eines Datensatz-Eintrags mit den Daten aus dem Datensatz
        val myMaterial = differ.currentList[position]
        bind = ItemOverviewBinding.bind(holder.itemView)
        holder.itemView.apply {
            // den Status anzeigen
            when (myMaterial.matStatus) {
                "Aktiv" -> Glide.with(this).load(R.drawable.ic_stat_okay).into(bind.ivOvwStatus)
                "Defekt" -> Glide.with(this).load(R.drawable.ic_stat_defekt).into(bind.ivOvwStatus)
                "Service" -> Glide.with(this).load(R.drawable.ic_stat_service)
                    .into(bind.ivOvwStatus)
                "Vermisst" -> Glide.with(this).load(R.drawable.ic_stat_missed)
                    .into(bind.ivOvwStatus)
                else -> Glide.with(this).load("").into(bind.ivOvwStatus)
            }
            Glide.with(this).load(myMaterial.bildBmp).into(bind.ivOvwMatBild)
            // die Datenfelder
            bind.tvOvwScancode.text = myMaterial.scancode
            bind.tvOvwMatchcode.text = myMaterial.matchcode
            bind.tvOvwHersteller.text = myMaterial.hersteller
            bind.tvOvwModell.text = myMaterial.modell
            bind.tvOvwSeriennummer.text = myMaterial.seriennummer
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

}