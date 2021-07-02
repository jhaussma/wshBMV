package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvBeleg
import de.wsh.wshbmv.db.entities.TbmvBelegPos
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BelegViewModel @Inject constructor(
    mainRepository: MainRepository
) : ViewModel() {

    private var mainRepo = mainRepository

    private val _belegDataLive = MutableLiveData<BelegData?>()
    val belegDataLive: LiveData<BelegData?> = _belegDataLive

    private val _newBelegId = MutableLiveData<String>("")
    val newBelegId: LiveData<String> = _newBelegId

    // Fehlerrückmeldungen für die Barcode-Übertragung:
    private val _barcodeErrorResponse = MutableLiveData<String>("")
    val barcodeErrorResponse: LiveData<String> = _barcodeErrorResponse


    fun clearBelegData() {
        Timber.tag(TAG).d("BelegViewModel, clearBelegData...")
        _belegDataLive.value = null
    }

    fun setNewBelegId(belegId: String) {
        Timber.tag(TAG).d("BelegViewModel, setNewBelegId mit $belegId aufgerufen")
        viewModelScope.launch {
            val belegData = mainRepo.getBelegDatenZuBelegId(belegId)
            _belegDataLive.value = belegData
        }
    }

    /** ############################################################################################
     *  wir legen einen neuen Beleg an (Transfer)
     */
    fun addNewBeleg(tbmvLager: TbmvLager) {
        Timber.tag(TAG).d("BelegViewModel, addNewBeleg aufgerufen")
        viewModelScope.launch {
            val belegId = mainRepo.insertBelegTransfer(tbmvLager)
            // wir starten eine neue Beleganzeige mit dem neuen Beleg...
            _newBelegId.value = belegId
        }


    }

    /** ############################################################################################
     *  wir ändern die Notiz in einem Beleg
     */
    fun updateBelegNotiz(newNotiz: String) {
        viewModelScope.launch {
            val tbmvBeleg = _belegDataLive.value!!.tbmvBeleg!!
            tbmvBeleg.notiz = newNotiz
            mainRepo.updateBeleg(tbmvBeleg)
            // nun aktualisieren wir das belegData und somit den Observer im Fragment...
            val belegData = mainRepo.getBelegDatenZuBelegId(tbmvBeleg.id)
            _belegDataLive.value = belegData
        }
    }

    /** ############################################################################################
     *  wir löschen einen Beleg (ohne Positionsdaten)
     */
    fun deleteBeleg(tbmvBeleg: TbmvBeleg) {
        viewModelScope.launch {
            mainRepo.deleteBeleg(tbmvBeleg)
        }
    }

    fun getBelegposVonBeleg(belegId: String) = mainRepo.getBelegposVonBeleg(belegId)


    /** ############################################################################################
     *  wir empfangen einen Barcode für ein Betriebsmittel zum Anhängen an die BelegPos-Liste
     */
    fun setNewMaterialIdByScancode(scancode: String) {
        viewModelScope.launch {
            val tbmvMat = mainRepo.getAllowedMaterialFromScancode(scancode)
            var bmData: BmData? = null
            if (tbmvMat != null) {
                bmData = mainRepo.getBMDatenZuMatID(tbmvMat.id)
            }
            if (bmData == null) {
                _barcodeErrorResponse.value =
                    "Barcode $scancode ist unbekannt oder fehlende Berechtigung!" // löst Fehlermeldung im UI aus
            } else {
                // nun prüfen wir, ob der Barcode verwendet werden kann
                if (bmData.matLager?.id == _belegDataLive.value?.tbmvBeleg?.zielLagerGuid) {
                    _barcodeErrorResponse.value =
                        "Das Betriebsmittel befindet sich schon im Ziel-Lager!"
                } else {
                    // prüfe auf Doppelanlage
                    var access: Boolean = true
                    val belegId: String = _belegDataLive.value!!.tbmvBeleg!!.id
                    var pos = 1
                    val belegPosListe = mainRepo.getBelegposVonBeleg(belegId).value
                    if (belegPosListe != null) {
                        if (belegPosListe.isNotEmpty()) {
                            pos = belegPosListe.size +1
                            // gibt es dieses BM schon in der Liste?
                            belegPosListe.forEach {
                                Timber.tag(TAG).d("Liste: ${it.tbmvBelegPos.matGuid}, Barcode: ${tbmvMat!!.id}")
                                if (it.tbmvBelegPos.matGuid == tbmvMat!!.id) {
                                    _barcodeErrorResponse.value =
                                        "Das Betriebsmittel befindet sich schon in der Liste!"
                                    access = false
                                }
                            }
                        }
                        Timber.tag(TAG).d("BelegViewModel: es gibt noch keine BelegPos-Liste!")
                        access = true
                    }

                    if (access) {
                        // nun wird angelegt!!
                        var tbmvBelegPos = TbmvBelegPos(
                            belegId = belegId,
                            pos = pos,
                            matGuid = tbmvMat!!.id,
                            menge = 1f,
                            vonLagerGuid = bmData.matLager?.id
                        )
                        mainRepo.insertBelegPos(tbmvBelegPos)

                        // wir müssen noch den Adapter aktualisieren


                    }
                }
            }

        }

    }

}