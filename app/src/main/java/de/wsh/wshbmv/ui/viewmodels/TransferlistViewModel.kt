package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.relations.BelegAndZielort
import de.wsh.wshbmv.other.GlobalVars
import de.wsh.wshbmv.other.TransDir
import de.wsh.wshbmv.other.TransStatus
import de.wsh.wshbmv.repositories.MainRepository
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class TransferlistViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    @JvmField
    @field:[Inject Named("LagerId")]
    var lagerId: String = GlobalVars.myLager?.id ?: ""

    private val mainRepo = mainRepository

    private var belegeToLagerAlle = mainRepo.getBelegeToLagerAlle(lagerId)
    private var belegeToLagerOffen = mainRepo.getBelegeToLagerOffen(lagerId)
    private var belegeToLagerInArbeit = mainRepo.getBelegeToLagerInArbeit(lagerId)
    private var belegeToLagerErledigt = mainRepo.getBelegeToLagerErledigt(lagerId)
    private var belegeVonLagerAlle = mainRepo.getBelegeVonLagerAlle(lagerId)
    private var belegeVonLagerOffen = mainRepo.getBelegeVonLagerOffen(lagerId)
    private var belegeVonLagerInArbeit = mainRepo.getBelegeVonLagerInArbeit(lagerId)
    private var belegeVonLagerErledigt = mainRepo.getBelegeVonLagerErledigt(lagerId)

    val belegliste = MediatorLiveData<List<BelegAndZielort>>()

    var transDir = TransDir.ANMICH
    var transStatus = TransStatus.OFFEN

    init {
        initTransfers()
    }


    fun filterDirection(transDir: TransDir) = when (transStatus) {
        TransStatus.ALLE -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerAlle.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerAlle.value?.let { belegliste.value = it }
            }
        }
        TransStatus.OFFEN -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerOffen.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerOffen.value?.let { belegliste.value = it }
            }
        }
        TransStatus.INARBEIT -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerInArbeit.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerInArbeit.value?.let { belegliste.value = it }
            }
        }
        TransStatus.ERLEDIGT -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerErledigt.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerErledigt.value?.let { belegliste.value = it }
            }
        }
    }.also {
        this.transDir = transDir
    }


    fun filterStatus(transStatus: TransStatus) = when (transStatus) {
        TransStatus.ALLE -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerAlle.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerAlle.value?.let { belegliste.value = it }
            }
        }
        TransStatus.OFFEN -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerOffen.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerOffen.value?.let { belegliste.value = it }
            }
        }
        TransStatus.INARBEIT -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerInArbeit.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerInArbeit.value?.let { belegliste.value = it }
            }
        }
        TransStatus.ERLEDIGT -> {
            if (transDir == TransDir.ANMICH) {
                belegeToLagerErledigt.value?.let { belegliste.value = it }
            } else {
                belegeVonLagerErledigt.value?.let { belegliste.value = it }
            }
        }
    }.also {
        this.transStatus = transStatus
    }

    fun filterBelegliste(newLagerId: String) {
        // lösche die alten Sourcen...
        belegliste.removeSource(belegeToLagerAlle)
        belegliste.removeSource(belegeToLagerOffen)
        belegliste.removeSource(belegeToLagerInArbeit)
        belegliste.removeSource(belegeToLagerErledigt)
        belegliste.removeSource(belegeVonLagerAlle)
        belegliste.removeSource(belegeVonLagerOffen)
        belegliste.removeSource(belegeVonLagerInArbeit)
        belegliste.removeSource(belegeVonLagerErledigt)
        // ..lade die neuen Sourcen..
        belegeToLagerAlle = mainRepo.getBelegeToLagerAlle(newLagerId)
        belegeToLagerOffen = mainRepo.getBelegeToLagerOffen(newLagerId)
        belegeToLagerInArbeit = mainRepo.getBelegeToLagerInArbeit(newLagerId)
        belegeToLagerErledigt = mainRepo.getBelegeToLagerErledigt(newLagerId)
        belegeVonLagerAlle  = mainRepo.getBelegeVonLagerAlle(newLagerId)
        belegeVonLagerOffen = mainRepo.getBelegeVonLagerOffen(newLagerId)
        belegeVonLagerInArbeit = mainRepo.getBelegeVonLagerInArbeit(newLagerId)
        belegeVonLagerErledigt = mainRepo.getBelegeVonLagerErledigt(newLagerId)
        // ..und orden wieder die aktive Liste zu
        initTransfers()
    }


    fun initTransfers() {
        belegliste.addSource(belegeToLagerAlle) { result ->
            if (transDir == TransDir.ANMICH && transStatus == TransStatus.ALLE) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeToLagerOffen) { result ->
            if (transDir == TransDir.ANMICH && transStatus == TransStatus.OFFEN) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeToLagerInArbeit) { result ->
            if (transDir == TransDir.ANMICH && transStatus == TransStatus.INARBEIT) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeToLagerErledigt) { result ->
            if (transDir == TransDir.ANMICH && transStatus == TransStatus.ERLEDIGT) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeVonLagerAlle) { result ->
            if (transDir == TransDir.VONMIR && transStatus == TransStatus.ALLE) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeVonLagerOffen) { result ->
            if (transDir == TransDir.VONMIR && transStatus == TransStatus.OFFEN) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeVonLagerInArbeit) { result ->
            if (transDir == TransDir.VONMIR && transStatus == TransStatus.INARBEIT) {
                result?.let { belegliste.value = it }
            }
        }
        belegliste.addSource(belegeVonLagerErledigt) { result ->
            if (transDir == TransDir.VONMIR && transStatus == TransStatus.ERLEDIGT) {
                result?.let { belegliste.value = it }
            }
        }
    }


}