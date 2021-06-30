package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BelegposAndMaterialAndLager
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BelegViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    private var mainRepo = mainRepository

    private var myBelegId = MutableLiveData("")

    private val _belegDataLive = MutableLiveData<BelegData?>()
    val belegDataLive: LiveData<BelegData?> = _belegDataLive

    fun setNewBelegId(belegId: String) {
        myBelegId.value = belegId
        viewModelScope.launch {
            val belegData = mainRepo.getBelegDatenZuBelegId(belegId)
            _belegDataLive.value = belegData
        }
    }

    /**
     *  wir legen einen neuen Beleg an (Transfer)
     */
    fun addNewBeleg(tbmvLager: TbmvLager) {
        viewModelScope.launch {
            val belegId = mainRepo.insertBelegTransfer(tbmvLager)

        }


    }

    fun getBelegposVonBeleg(belegId: String) = mainRepo.getBelegposVonBeleg(belegId)



}