package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.repositories.MainRepository
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {

    private val matInLager = mainRepository.getMatlistOfLager(myLager!!.id)
    private val matLagers = matInLager

    val materials = MediatorLiveData<List<TbmvMat>>()

    init {
        // Hier gehts noch nicht so richtig mit der Initialisierung!!!
        materials.addSource()
    }

}