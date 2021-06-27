package de.wsh.wshbmv.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    private val mainRepo = mainRepository

    private var myMaterialId = MutableLiveData("")

    private val _bmDataLive = MutableLiveData<BmData?>()
    val bmDataLive: LiveData<BmData?> = _bmDataLive

    private val _barcodeNotFound = MutableLiveData<Boolean>(false)
    val barcodeNotFound: LiveData<Boolean> = _barcodeNotFound

    /**
     *  eine neue Material-/Betriebsmittel-ID kann damit definiert werden, die dazugehörgen Daten werden erzeugt
     */
    fun setNewMaterialId(materialId: String) {
        myMaterialId.value = materialId
        viewModelScope.launch {
            val bmData = mainRepo.getBMDatenZuMatID(materialId)
            _bmDataLive.value = bmData
        }
    }

    /**
     *  eine neuer Barcode wurde eingelesen -> BmData neu bestimmen (oder Fehlermeldung auslösen)
     */
    fun setNewMaterialIdByScancode(scancode: String) {
        viewModelScope.launch {
            val tbmvMat  = mainRepo.getAllowedMaterialFromScancode(scancode)
            var bmData: BmData? = null
            if (tbmvMat != null) {
                 bmData = mainRepo.getBMDatenZuMatID(tbmvMat.id)
            }
            if (bmData == null) {
                _barcodeNotFound.value = true // löst Fehlermeldung im UI aus
            } else {
                _bmDataLive.value = bmData
                _barcodeNotFound.value = false
            }
        }
    }

    /**
     *
     */
    fun importNewPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val bmData = bmDataLive.value
            bmData?.tbmvMat?.bildBmp = bitmap
            _bmDataLive.value = bmData
            // speichere die Änderung in die Tabelle TbmvMat
            bmData?.tbmvMat?.let { mainRepo.updateMat(it)}

        }
    }

}