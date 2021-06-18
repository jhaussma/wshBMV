package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.other.GlobalVars.myLagers
import de.wsh.wshbmv.other.SortType
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class OverviewViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    @JvmField
    @field:[Inject Named("LagerId")]
    var lagerId: String = myLager?.id ?: ""

    private val mainRepo = mainRepository

    private var materialSortByMatchcode = mainRepository.getMaterialSortByMatchocde(lagerId)
    private var materialSortByScancode = mainRepository.getMaterialSortByScancode(lagerId)
    private var materialSortBySeriennr = mainRepository.getMaterialSortBySeriennr(lagerId)
    private var materialSortByHersteller = mainRepository.getMaterialSortByHersteller(lagerId)
    private var materialSortByModell = mainRepository.getMaterialSortByModell(lagerId)
    private var materialSortByStatus = mainRepository.getMaterialSortByStatus(lagerId)

    val materials = MediatorLiveData<List<TbmvMat>>()

    var sortType = SortType.MATCHCODE

    init {
        /*
            initialisiert die Listen der Betriebsmittel (alle Sortierungen)
            aktuell wird die Initialisierung nicht benötigt, da die Zuordnung sowieso automatisch beim Ersteinstellen der Spinner (Lager und Sortierung) neu durchgeführt wird (doppelter Aufruf!)
         */
        // initMaterials()
    }

    fun sortMatliste(sortType: SortType) = when (sortType) {
        SortType.MATCHCODE -> materialSortByMatchcode.value?.let { materials.value = it }
        SortType.SCANCODE -> materialSortByScancode.value?.let { materials.value = it }
        SortType.SERIENNUMMER -> materialSortBySeriennr.value?.let { materials.value = it }
        SortType.HERSTELLER -> materialSortByHersteller.value?.let { materials.value = it }
        SortType.MODELL -> materialSortByModell.value?.let { materials.value = it }
        SortType.STATUS -> materialSortByStatus.value?.let { materials.value = it }
    }.also {
        this.sortType = sortType
    }

    fun filterMatListe(newLagerId: String) {
        // lösche die alten Sourcen...
        materials.removeSource(materialSortByMatchcode)
        materials.removeSource(materialSortByScancode)
        materials.removeSource(materialSortBySeriennr)
        materials.removeSource(materialSortByHersteller)
        materials.removeSource(materialSortByModell)
        materials.removeSource(materialSortByStatus)
        // lade die neuen Sourcen...
        materialSortByMatchcode = mainRepo.getMaterialSortByMatchocde(newLagerId)
        materialSortByScancode = mainRepo.getMaterialSortByScancode(newLagerId)
        materialSortBySeriennr = mainRepo.getMaterialSortBySeriennr(newLagerId)
        materialSortByHersteller = mainRepo.getMaterialSortByHersteller(newLagerId)
        materialSortByModell = mainRepo.getMaterialSortByModell(newLagerId)
        materialSortByStatus = mainRepo.getMaterialSortByStatus(newLagerId)
        // ..und ordne die wieder dem materials zu
        initMaterials()
    }

    fun initMaterials() {
        Timber.tag(TAG).d("lädt Materials mit lagerId: $lagerId")
        materials.addSource(materialSortByHersteller) { result ->
            if (sortType == SortType.HERSTELLER) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByMatchcode) { result ->
            if (sortType == SortType.MATCHCODE) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByModell) { result ->
            if (sortType == SortType.MODELL) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByScancode) { result ->
            if (sortType == SortType.SCANCODE) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortBySeriennr) { result ->
            if (sortType == SortType.SERIENNUMMER) {
                result?.let { materials.value = it }
            }
        }
        materials.addSource(materialSortByStatus) { result ->
            if (sortType == SortType.STATUS) {
                result?.let { materials.value = it }
            }
        }
    }

    /**
     *  wir suchen den Material-Datensatz eines Betriebsmittels über den Scancode (inkl. Berechtigungsprüfung)
     */
    fun getMaterialFromScancode(scancode: String): TbmvMat? {
        // zuerst das Material suchen
        var tbmvMat: TbmvMat? = null
        viewModelScope.launch {
            tbmvMat = mainRepo.getMaterialByScancode(scancode)
            Timber.tag(TAG).d("getMaterialFromScancode, tbmvMat = ${tbmvMat.toString()}")
            if (tbmvMat != null) {
                // Material gefunden, nun muss es noch in unserer Lagerliste-Berechtigung drin sein
                val lagers = mainRepo.getLagersWithMaterialId(tbmvMat!!.id)
                Timber.tag(TAG).d("getMaterialFromScancode, Lagerliste = ${lagers.toString()}")
                if (lagers.isEmpty()) {
                    // wir haben kein Lager zum Betriebsmittel gefunden...
                    tbmvMat = null
                } else {
                    val resultLagers = lagers.intersect(myLagers)
                    Timber.tag(TAG).d("getMaterialFromScancode, myLagers = ${myLagers.toString()}")
                    if (resultLagers.isEmpty()) {
                        // wir haben keine Berechtigung zu einem dieser Lager...
                        tbmvMat = null
                        Timber.tag(TAG).d("getMaterialFromScancode, keine Lager-Übereinstimmung gefunden")
                    }
                }
            }
        }
        // das Ergebnis ist tbmvMat, wenn wir es sehen dürfen, ansonsten null
        return tbmvMat
    }
}