package de.wsh.wshbmv.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentSetupBinding
import de.wsh.wshbmv.other.Constants.KEY_FIRST_TIME
import de.wsh.wshbmv.other.Constants.KEY_LAGER
import de.wsh.wshbmv.other.Constants.KEY_USER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_HASH
import de.wsh.wshbmv.other.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentSetupBinding

    @Inject
    lateinit var sharedPref: SharedPreferences

    @set:Inject
    var isFirstAppOpen = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentSetupBinding.bind(view) // initialisiert das Binding zu den Layout-Objekten

        if (!isFirstAppOpen) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()

            var myLager = sharedPref.getString(KEY_LAGER, "") ?: ""

            if (myLager == "") {
                // wir müssen noch die Lager-Bestimmung angehen
                findNavController().navigate(
                    R.id.action_setupFragment_to_settingsFragment,
                    savedInstanceState,
                    navOptions
                )
            } else {
                // direkt in die Übersichts-Ansicht
                findNavController().navigate(
                    R.id.action_setupFragment_to_overviewFragment,
                    savedInstanceState,
                    navOptions
                )
            }
        }

        bind.tvContinue.setOnClickListener {
            val success = writeUserInfoToSharedPref()
            if (success) {
                findNavController().navigate(R.id.action_setupFragment_to_settingsFragment)
            } else {
                Snackbar.make(requireView(), "Bitte zuerst Username und Passwort eingeben!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun  writeUserInfoToSharedPref(): Boolean {
        val username = bind.etUserName.text.toString()
        val userHash = HashUtils.sha256(bind.etUserPwd.text.toString() )
        // überprüfe die Plausibilität der Eingaben
        if(username.isEmpty() || userHash.isEmpty() || userHash.length < 4) {
            return false
        }
        sharedPref.edit()
            .putString(KEY_USER_NAME, username)
            .putString(KEY_USER_HASH, userHash)
            .putBoolean(KEY_FIRST_TIME, false)
            .apply()
        return true
    }



}