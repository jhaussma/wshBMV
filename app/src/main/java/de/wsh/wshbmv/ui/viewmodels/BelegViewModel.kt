package de.wsh.wshbmv.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.wsh.wshbmv.db.entities.TbmvBelege
import de.wsh.wshbmv.db.entities.TbmvBelegPos
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.relations.BelegData
import de.wsh.wshbmv.db.entities.relations.BmData
import de.wsh.wshbmv.db.entities.relations.TbmvMat_Lager
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
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


    /** ############################################################################################
     * wir löschen initial alle Belegdaten in den LiveData s
     */
    fun clearViewModelLiveData() {
        Timber.tag(TAG).d("BelegViewModel, clearBelegData...")
        _belegDataLive.value = null
        _barcodeErrorResponse.value = ""
    }

    /** ############################################################################################
     *  eine neue BelegId wird aktiviert und die Daten hierzu aktualisiert (...die Observer)
     */
    fun setNewBelegId(belegId: String) {
        viewModelScope.launch {
            val belegData = mainRepo.getBelegDatenZuBelegId(belegId)
            _belegDataLive.value = belegData
        }
    }

    /** ############################################################################################
     *  wir legen einen neuen Beleg an (Transfer)
     */
    fun addNewBeleg(tbmvLager: TbmvLager) {
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
            val tbmvBeleg = _belegDataLive.value!!.tbmvBelege!!
            tbmvBeleg.notiz = newNotiz
            mainRepo.updateBeleg(tbmvBeleg, feldnamen = listOf("Notiz"))
            // nun aktualisieren wir das belegData und somit den Observer im Fragment...
            val belegData = mainRepo.getBelegDatenZuBelegId(tbmvBeleg.id)
            _belegDataLive.value = belegData
        }
    }

    /** ############################################################################################
     *  wir löschen einen Beleg (ohne Positionsdaten)
     */
    fun deleteBeleg(tbmvBelege: TbmvBelege) {
        viewModelScope.launch {
            mainRepo.deleteBeleg(tbmvBelege)
        }
    }

    /**
     *  wir laden die LiveData-BelegPos-Liste zur Überwachung im Fragment...
     */
    fun getBelegposVonBeleg(belegId: String) = mainRepo.getBelegposVonBelegLive(belegId)


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
                if (bmData.matLager?.id == _belegDataLive.value?.tbmvBelege?.zielLagerGuid) {
                    _barcodeErrorResponse.value =
                        "Das Betriebsmittel befindet sich schon im Ziel-Lager!"
                } else {
                    // prüfe auf Doppelanlage
                    var access: Boolean = true
                    val belegId: String = _belegDataLive.value!!.tbmvBelege!!.id
                    var pos = 1
                    val belegPosListe = mainRepo.getBelegposVonBeleg(belegId)
                    if (belegPosListe.isNotEmpty()) {
                        pos = belegPosListe.size + 1
                        // gibt es dieses BM schon in der Liste?
                        belegPosListe.forEach {
                            if (it.matGuid == tbmvMat!!.id) {
                                _barcodeErrorResponse.value =
                                    "Das Betriebsmittel befindet sich schon in der Liste!"
                                access = false
                            }
                        }
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
                    }
                }
            }
        }
    }

    /** ############################################################################################
     *  wir empfangen einen Barcode zur Bestätigung einer Betriebsmittel-Übergabe
     */
    fun acknowledgeMaterialByScancode(scancode: String) {
        Timber.tag(TAG).d("acknowledgeMaterialByScancode...")
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
                var cntNoAck = 0
                var tbmvBelegPos: TbmvBelegPos? = null
                val belegId: String = _belegDataLive.value!!.tbmvBelege!!.id
                val belegPosListe = mainRepo.getBelegposVonBeleg(belegId)
                if (belegPosListe.isNotEmpty()) {
                    // ist dies ein Betriebsmittel des Belegs?
                    belegPosListe.forEach {
                        if (it.ackDatum == null) cntNoAck += 1
                        if (it.matGuid == tbmvMat!!.id) {
                            if (it.ackDatum == null) {
                                tbmvBelegPos = it
                            } else {
                                _barcodeErrorResponse.value =
                                    "Das Betriebsmittel wurde bereits bestätigt!"
                            }
                        }
                    }
                }

                if (tbmvBelegPos == null) {
                    _barcodeErrorResponse.value =
                        "Das Betriebsmittel gehört nicht zur Liste dieses Belegs!"
                } else {
                    tbmvBelegPos!!.ackDatum = Date()
                    Timber.tag(TAG)
                        .d("acknowledgeMaterialByScancode, updateBelegPos mit ${tbmvBelegPos.toString()}")
                    mainRepo.updateBelegPos(tbmvBelegPos!!, feldnamen = listOf("AckDatum"))
                    // aktualisiere die Material-Lager-Einträge (Bestand 1 zuerst...)
                    val zielLagerId = _belegDataLive.value!!.zielLager!!.id
                    var bestandIsPlaced = false
                    val lagersBestand = mainRepo.getLagersBestandOfMaterialID(tbmvMat!!.id)
                    if (lagersBestand.isEmpty()) {
                        // eher untypisch, da fehlte offensichtlich bisher eine Materialzuordnung!!
                        // -> in diesem Falle wird einfach ein Hauptlager angelegt
                        Timber.tag(TAG)
                            .d("acknowledgeMaterialByScancode, kein Lagerbestand gefunden")
                        val tbmvMatLager = TbmvMat_Lager(
                            id = "",
                            matId = tbmvMat.id,
                            lagerId = zielLagerId,
                            isDefault = 1,
                            bestand = 1f
                        )
                        mainRepo.insertMat_Lager(tbmvMatLager)
                        bestandIsPlaced = true
                    } else {
                        // wir entscheiden je nach aktueller Lager-Belegung...
                        // Start mit dem ersten Eintrag (normalerweise der mit Bestand !) -> muss ich löschen oder 0-setzen?
                        var tbmvMatLager = lagersBestand[0]
                        if (tbmvMatLager.bestand == 0f && tbmvMatLager.isDefault == 1) {
                            // es fehlt bisher ein Bestand, d.h. der Materialstatus ändert sich ebenfalls...
                            Timber.tag(TAG)
                                .d("acknowledgeMaterialByScancode, Bestand (im Default) war schon auf 0...")
                            if (tbmvMatLager.id == zielLagerId) {
                                // wir setzen den Bestand auf 1
                                Timber.tag(TAG)
                                    .d("acknowledgeMaterialByScancode, Bestand im Hauptlager auf 1 gesetzt..")
                                tbmvMatLager.bestand = 1f
                                mainRepo.updateMat_Lager(tbmvMatLager)
                                bestandIsPlaced = true
                            }
                            if (tbmvMat.matStatus == "Vermisst") {
                                tbmvMat.matStatus = "Aktiv"
                                mainRepo.updateMat(tbmvMat, feldname = "MatStatus")
                            }

                        } else {
                            // Wenn erster Eintrag = Default-Lager, Bestand auf 0 setzen, ansonsten Eintrag löschen
                            if (tbmvMatLager.isDefault == 1) {
                                Timber.tag(TAG)
                                    .d("acknowledgeMaterialByScancode, Hauptlagerbestand wird 0 gesetzt...")
                                tbmvMatLager.bestand = 0f
                                mainRepo.updateMat_Lager(tbmvMatLager)
                            } else {
                                Timber.tag(TAG)
                                    .d("acknowledgeMaterialByScancode, NICHT-Hauptlager wird gelöscht")
                                mainRepo.deleteMat_Lager(tbmvMatLager)
                            }
                        }
                        // sofern ein 2. Eintrag da ist, sollte das das Hauptlager sein, welches den Bestand 0 hat
                        if (lagersBestand.size > 1) {
                            Timber.tag(TAG)
                                .d("acknowledgeMaterialByScancode, zweites Lager gefunden...")
                            tbmvMatLager = lagersBestand[1]
                            // ist das Lager identisch mit dem Ziellager, wird hier der Bestand korrigiert, ansonsten später ein neuer Datensatz angehängt...
                            if (tbmvMatLager.lagerId == zielLagerId) {
                                Timber.tag(TAG)
                                    .d("acknowledgeMaterialByScancode, 2. Lager ist Ziellager, Bestand auf 1 gesetzt...")
                                tbmvMatLager.bestand = 1f
                                mainRepo.updateMat_Lager(tbmvMatLager)
                                bestandIsPlaced = true
                            }
                        }

                        // ist nun der neue Bestand noch nicht eingetragen, legen wir einen neuen Eintrag an - das kann dann aber nicht das Hauptlager sein!
                        if (!bestandIsPlaced) {
                            Timber.tag(TAG)
                                .d("acknowledgeMaterialByScancode, neuer Eintrag für Ziellager mit Bestand = 1 gesetzt...")
                            val newMatLager = TbmvMat_Lager(
                                id = "",
                                matId = tbmvMat.id,
                                lagerId = zielLagerId,
                                isDefault = 0,
                                bestand = 1f
                            )
                            mainRepo.insertMat_Lager(newMatLager)
                        } else {
                            Timber.tag(TAG).d("Bestand is Placed")
                        }
                    }

                    if (cntNoAck <= 1) {
                        // es wurden alle Positionen bestätigt (die letzte Position mit diesem Vorgang...)
                        // .. wir aktualisieren den BelegStatus
                        val tbmvBeleg = _belegDataLive.value!!.tbmvBelege!!
                        tbmvBeleg.belegStatus = "Fertig"
                        tbmvBeleg.belegDatum = Date()
                        mainRepo.updateBeleg(
                            tbmvBeleg,
                            feldnamen = listOf("BelegStatus", "BelegDatum")
                        )
                        _belegDataLive.value = _belegDataLive.value //initiiert den Observer
                    }
                }
            }
        }
    }

    /** ############################################################################################
     *  wir löschen einen Betriebsmitteleintrag in der BelegPos-Liste raus
     */
    fun deleteBelegPos(tbmvBelegPos: TbmvBelegPos) {
        viewModelScope.launch {
            val pos = tbmvBelegPos.pos
            val belegId = tbmvBelegPos.belegId
            val belegPosListe = mainRepo.getBelegposVonBeleg(belegId)
            // ggf. die nachfolgenden Pos-Nummern 1 runtersetzen...
            belegPosListe.forEach {
                if (it.pos > pos) {
                    it.pos -= 1
                    mainRepo.updateBelegPos(it, feldnamen = listOf("Pos"))
                }
            }

            // und nun den Datensatz löschen
            mainRepo.deleteBelegPos(tbmvBelegPos)
            _belegDataLive.value = _belegDataLive.value //initiiert den Observer
        }
    }

    /**
     *  ändere den Status des aktuellen Belegs von In Arbeit auf Erfasst
     */
    fun setBelegStatusToReleased() {
        viewModelScope.launch {
            val tbmvBeleg = _belegDataLive.value!!.tbmvBelege!!
            tbmvBeleg.belegStatus = "Erfasst"
            tbmvBeleg.belegDatum = Date()
            mainRepo.updateBeleg(tbmvBeleg, feldnamen = listOf("BelegStatus", "BelegDatum"))
        }
    }


}