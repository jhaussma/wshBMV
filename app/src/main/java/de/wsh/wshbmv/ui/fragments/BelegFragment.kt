package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.adapters.BelegposAdapter
import de.wsh.wshbmv.databinding.FragmentBelegBinding
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BelegposAndMaterialAndLager
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.ui.viewmodels.BelegViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BelegFragment : Fragment(R.layout.fragment_beleg), BelegposAdapter.OnItemClickListener {


    private val belegViewModel: BelegViewModel by activityViewModels()

    private lateinit var belegposAdapter: BelegposAdapter

    private lateinit var bind: FragmentBelegBinding

    private var belegId: String? = null


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

        setupRecyclerView()

        belegViewModel.belegDataLive.observe(viewLifecycleOwner, {
            if (it != null) {
                writeUiValues(it)
                belegposAdapter.notifyDataSetChanged()
            }
        })

        belegViewModel.getBelegposVonBeleg(belegId!!).observe(viewLifecycleOwner, Observer {
            belegposAdapter.belegPosn = it
            belegposAdapter.notifyDataSetChanged()
        })

        Timber.tag(TAG).d("BelegFragment, onViewCreated mit Parameter Beleg-ID $belegId")
    }

    override fun onBelegposlistItemClick(belegPosMaterialLager: BelegposAndMaterialAndLager) {
        Timber.tag(TAG).d("BelegFragment, onBelegposListItemClick mit BelegposID: ${belegPosMaterialLager.tbmvBelegPos.id}")

    }

    private fun setupRecyclerView() = bind.rvBelegPos.apply {
        belegposAdapter = BelegposAdapter( listOf(),this@BelegFragment)
        adapter = belegposAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }


    private fun writeUiValues(belegData: BelegData) {
        bind.tvBelegTyp.text = belegData.tbmvBeleg?.belegTyp
        bind.tvBelegDatum.text = belegData.tbmvBeleg?.belegDatum?.formatedDateDE()
        bind.tvBelegUser.text = belegData.belegUser?.userKennung
        bind.tvBelegZielort.text = belegData.zielLager?.matchcode
        bind.actvBelegZielort.setText(belegData.zielLager?.matchcode)
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

}