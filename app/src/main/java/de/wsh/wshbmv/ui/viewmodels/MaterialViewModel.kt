package de.wsh.wshbmv.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            val bmData = mainRepo.getBMDatenZuMatID(materialId)
            bmDataLive.value = bmData
        }
    }

    /**
     *
     */
    fun importNewPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val bmData = bmDataLive.value
            bmData?.tbmvMat?.bildBmp = bitmap
            bmDataLive.value = bmData
            // speichere die Änderung in die Tabelle TbmvMat
            bmData?.tbmvMat?.let { mainRepo.updateMat(it)}

        }
    }

    /**
     *  mit Hilfe von getMaterialID() kann eine Anwendung die gerade aktive Material-ID der Materialsicht
     */
    fun getMaterialId() = myMaterialId

}