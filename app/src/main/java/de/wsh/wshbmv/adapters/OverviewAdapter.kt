package de.wsh.wshbmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.ActivityMainBinding
import de.wsh.wshbmv.databinding.FragmentOverviewBinding
import de.wsh.wshbmv.databinding.FragmentSetupBinding
import de.wsh.wshbmv.databinding.ItemOverviewBinding
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.relations.MatInLager

// Constructor u.U. später wieder rausnehmen, ähnlich RunAdapter
class OverviewAdapter() : RecyclerView.Adapter<OverviewAdapter.OverviewViewHolder>()  {

    inner class OverviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private  lateinit var binding: ItemOverviewBinding

    val diffCallback = object : DiffUtil.ItemCallback<TbmvMat>() {
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
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_overview,
            parent,
            false
        )
        return OverviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: OverviewViewHolder, position: Int) {
        // befülle die Sicht eines Datensatz-Eintrags mit den Daten aus dem Datensatz
        val material = differ.currentList[position]
        holder.itemView.apply {
            binding.tvOvwScancode.text = material.scancode
            binding.tvOvwMatchcode.text = material.matchcode
            binding.tvOvwHersteller.text = material.hersteller
            binding.tvOvwModell.text = material.modell
            binding.tvOvwSeriennummer.text = material.seriennummer
            binding.ivOvwMatBild.setImageBitmap(material.bildBmp)
        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}