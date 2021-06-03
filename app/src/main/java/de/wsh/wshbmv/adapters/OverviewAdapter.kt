package de.wsh.wshbmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.RecyclerView
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.ActivityMainBinding
import de.wsh.wshbmv.databinding.FragmentOverviewBinding
import de.wsh.wshbmv.databinding.FragmentSetupBinding
import de.wsh.wshbmv.databinding.ItemOverviewBinding
import de.wsh.wshbmv.db.entities.TbmvMat

// Constructor u.U. später wieder rausnehmen, ähnlich RunAdapter
class OverviewAdapter(
    var matList: List<TbmvMat>
) : RecyclerView.Adapter<OverviewAdapter.OverviewViewHolder>()  {

    inner class OverviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private  lateinit var binding: ItemOverviewBinding


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
        holder.itemView.apply {
            binding.tvOvwScancode.text = matList[position].scancode
            binding.tvOvwMatchcode.text = matList[position].matchcode
            binding.tvOvwHersteller.text = matList[position].hersteller
            binding.tvOvwModell.text = matList[position].modell
            binding.tvOvwSeriennummer.text = matList[position].seriennummer
            binding.ivOvwMatBild.setImageBitmap(matList[position].bildBmp)
        }

    }

    override fun getItemCount(): Int {
        return matList.size
    }
}