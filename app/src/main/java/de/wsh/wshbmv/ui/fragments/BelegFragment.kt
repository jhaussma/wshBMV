package de.wsh.wshbmv.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentBelegBinding
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber

@AndroidEntryPoint
class BelegFragment : Fragment(R.layout.fragment_beleg) {

    private lateinit var bind: FragmentBelegBinding

    private var belegId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG).d("BelegFragment, onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_beleg, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // initialisiere das Binding
        bind = FragmentBelegBinding.bind(view)

        (activity as AppCompatActivity).supportActionBar?.title = "Beleg"

        belegId = arguments?.getString("belegId")
        Timber.tag(TAG).d("BelegFragment, onViewCreated mit Parameter Beleg-ID $belegId")

    }
}