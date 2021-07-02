package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvBeleg
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BelegposAndMaterialAndLager
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.repositories.MainRepository
import de.wsh.wshbmv.ui.FragCommunicator
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BelegViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    private var mainRepo = mainRepository

    private val _belegDataLive = MutableLiveData<BelegData?>()
    val belegDataLive: LiveData<BelegData?> = _belegDataLive

    private val _newBelegId = MutableLiveData<String>("")
    val newBelegId: LiveData<String> = _newBelegId

    fun clearBelegData() {
        Timber.tag(TAG).d("BelegViewModel, clearBelegData...")
        _belegDataLive.value = null
    }

    fun setNewBelegId(belegId: String) {
        Timber.tag(TAG).d("BelegViewModel, setNewBelegId mit $belegId aufgerufen")
        viewModelScope.launch {
            val belegData = mainRepo.getBelegDatenZuBelegId(belegId)
            _belegDataLive.value = belegData
        }
    }

    /**
     *  wir legen einen neuen Beleg an (Transfer)
     */
    fun addNewBeleg(tbmvLager: TbmvLager) {
        Timber.tag(TAG).d("BelegViewModel, addNewBeleg aufgerufen")
        viewModelScope.launch {
            val belegId = mainRepo.insertBelegTransfer(tbmvLager)
            // wir starten eine neue Beleganzeige mit dem neuen Beleg...
            _newBelegId.value = belegId
        }


    }

    /**
     *  wir löschen einen Beleg (ohne Positionsdaten)
     */
    fun deleteBeleg(tbmvBeleg: TbmvBeleg) {
        viewModelScope.launch {
            mainRepo.deleteBeleg(tbmvBeleg)
        }
    }

    fun getBelegposVonBeleg(belegId: String) = mainRepo.getBelegposVonBeleg(belegId)



}