package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.other.MyColor
import de.wsh.wshbmv.repositories.MainRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {


}