package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MaterialViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    private var mainRepo = mainRepository

    private var myMaterialId = MutableLiveData("")

    var bmDataLive: MutableLiveData<BmData> = MutableLiveData()


    /**
     *  eine neue Material-/Betriebsmittel-ID kann damit definiert werden, die dazugehörgen Daten werden erzeugt
     */
    fun setNewMaterialId(materialId: String) {
        myMaterialId.value = materialId
        Timber.tag(TAG).d("setNewMaterialId gestartet im ViewModel")
        viewModelScope.launch {
            val bmData = mainRepo.getBMDatenZuMatID(materialId)
            Timber.tag(TAG).d("im ViewModel angekommen: ${bmData?.tbmvMat.toString()}")
            bmDataLive.value = bmData
        }
    }

    /**
     *  mit Hilfe von getMaterialID() kann eine Anwendung die gerade aktive Material-ID der Materialsicht
     */
    fun getMaterialId() = myMaterialId

}