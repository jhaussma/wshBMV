package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.other.SortType
import de.wsh.wshbmv.repositories.MainRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class OverviewViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    @JvmField
    @field:[Inject Named("LagerId")]
    var lagerId: String = ""


    private val materialSortByMatchcode = mainRepository.getMaterialSortByMatchocde(lagerId)
    private val materialSortByScancode = mainRepository.getMaterialSortByScancode(lagerId)
    private val materialSortBySeriennr = mainRepository.getMaterialSortBySeriennr(lagerId)
    private val materialSortByHersteller = mainRepository.getMaterialSortByHersteller(lagerId)
    private val materialSortByModell = mainRepository.getMaterialSortByModell(lagerId)
    private val materialSortByStatus = mainRepository.getMaterialSortByStatus(lagerId)


    val materials = MediatorLiveData<List<TbmvMat>>()

    var sortType = SortType.MATCHCODE

    init {
        materials.addSource(materialSortByHersteller) { result ->
            if(sortType == SortType.HERSTELLER) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByMatchcode) { result ->
            if(sortType == SortType.MATCHCODE) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByModell) { result ->
            if(sortType == SortType.MODELL) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByScancode) { result ->
            if(sortType == SortType.SCANCODE) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortBySeriennr) { result ->
            if(sortType == SortType.SERIENNUMMER) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByStatus) { result ->
            if(sortType == SortType.STATUS) {
                result?.let { materials.value = it }
            }
        }
    }

    fun sortMatliste(sortType: SortType) = when(sortType) {
        SortType.MATCHCODE -> materialSortByMatchcode.value?.let { materials.value = it }
        SortType.SCANCODE -> materialSortByScancode.value?.let { materials.value = it }
        SortType.SERIENNUMMER -> materialSortBySeriennr.value?.let { materials.value = it }
        SortType.HERSTELLER -> materialSortByHersteller.value?.let { materials.value = it }
        SortType.MODELL -> materialSortByModell.value?.let { materials.value = it }
        SortType.STATUS -> materialSortByStatus.value?.let { materials.value = it }
    }.also {
        this.sortType = sortType
    }



}