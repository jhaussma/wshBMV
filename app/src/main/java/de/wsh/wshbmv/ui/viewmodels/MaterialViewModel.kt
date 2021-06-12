package de.wsh.wshbmv.ui.viewmodels

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.other.Constants
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MaterialViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    private var mainRepo = mainRepository

    private var myMaterialId = MutableLiveData("")

    var bmData: MutableLiveData<BmData> = MutableLiveData()



    // nur für Testzwecke
    private var _country = MutableLiveData("Serbia")
    val country: LiveData<String> = _country

    fun saveCountry(newCountry: String) {
        _country.value = newCountry
    }


    // wir übernehmen die materialId und laden den passenden Datensatz mit Betriebsmittel-Daten
    fun getBmDataToMaterialId(materialId: String): BmData? {
        var bmData: BmData? = null
        viewModelScope.launch {
            val _bmData = mainRepo.getBMDatenZuMatID(materialId)
            Timber.tag(TAG).d("im ViewModel angekommen: ${_bmData.tbmvMat.toString()}")
            bmData = _bmData
        }
        Timber.tag(TAG).d("Viewmodel gibt zurück: ${bmData?.tbmvMat.toString()}")
        return bmData
    }

    private fun getNewBmData(materialId: String): BmData? {
        var bmData: BmData? = null
        viewModelScope.launch {
            val _bmData = mainRepo.getBMDatenZuMatID(materialId)
            Timber.tag(TAG).d("im ViewModel angekommen: ${_bmData.tbmvMat.toString()}")
            bmData = _bmData
        }
        Timber.tag(TAG).d("Viewmodel gibt zurück: ${bmData?.tbmvMat.toString()}")
        return bmData
    }

    fun setNewMaterialId(materialId: String) {
        myMaterialId.value = materialId
        Timber.tag(TAG).d("setNewMaterialId gestartet im ViewModel")
        bmData.value = getNewBmData(materialId)
    }

    fun getMaterialId() = myMaterialId

}