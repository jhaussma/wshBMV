package de.wsh.wshbmv.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import de.wsh.wshbmv.databinding.ItemTransferlistBinding
import de.wsh.wshbmv.db.entities.TbmvBeleg
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber

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
                val tbmvBeleg = differ.currentList[position]
                listener.onTransferlistItemClick(tbmvBeleg)
            }
        }
    }

    private lateinit var bind: ItemTransferlistBinding

    private val diffCallback = object : DiffUtil.ItemCallback<TbmvBeleg>() {
        override fun areItemsTheSame(oldItem: TbmvBeleg, newItem: TbmvBeleg): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TbmvBeleg, newItem: TbmvBeleg): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    var differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<TbmvBeleg>) = differ.submitList(list)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferlistViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: TransferlistViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    /** ##########################################################################################################
     *   Übergabe des OnClick-Listeners des RecyclerView an ein View-Element (der Adapter selbst ist das ja nicht!)
     */
    interface OnItemClickListener {
        fun onTransferlistItemClick(tbmvMat: TbmvBeleg)
    }


}