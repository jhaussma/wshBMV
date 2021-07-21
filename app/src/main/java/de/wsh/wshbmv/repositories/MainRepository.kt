package de.wsh.wshbmv.repositories

import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import de.wsh.wshbmv.other.Constants.DB_AKTION_ADD_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_DELETE_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_UPDATE_DS
import de.wsh.wshbmv.other.GlobalVars
import de.wsh.wshbmv.other.GlobalVars.myUser
import de.wsh.wshbmv.other.GlobalVars.sqlSynchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class MainRepository @Inject constructor(
    val tbmvDao: TbmvDAO
) {

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * die User-Zugriffe
     */
    suspend fun insertUser(tsysUser: TsysUser) = tbmvDao.insertUser(tsysUser)
    suspend fun updateUser(tsysUser: TsysUser) = tbmvDao.updateUser(tsysUser)

    suspend fun deleteUserById(satzId: String) {
        withContext(Dispatchers.IO) {
            val tsysUser = tbmvDao.getUserById(satzId)
            if (tsysUser != null) tbmvDao.deleteUser(tsysUser)
        }
    }

    suspend fun insertUserGruppe(tsysUserGruppe: TsysUserGruppe) =
        tbmvDao.insertUserGruppe(tsysUserGruppe)

    suspend fun deleteUserGruppeById(satzId: String) {
        withContext(Dispatchers.IO) {
            val tsysUserGruppe = tbmvDao.getUserGruppeByID(satzId)
            if (tsysUserGruppe != null) tbmvDao.deleteUserGruppe(tsysUserGruppe)
        }
    }

    suspend fun insertUserInGruppe(tsysUserInGruppe: TsysUserToGruppe) =
        tbmvDao.insertUserInGruppe(tsysUserInGruppe)

    suspend fun deleteUserInGruppeById(satzId: String) {
        withContext(Dispatchers.IO) {
            val tsysUserInGruppe = tbmvDao.getUserInGruppeById(satzId)
            if (tsysUserInGruppe != null) tbmvDao.deleteUserInGruppe(tsysUserInGruppe)
        }
    }



    suspend fun getUserByID(userID: String) = tbmvDao.getUserById(userID)
    fun getUserByLogName(userLogName: String) = tbmvDao.getUserByLogName(userLogName)
    suspend fun getUsersOfLagersAll() = tbmvDao.getUsersOfLagersAll()

    fun getUsersActive() = tbmvDao.getUsersActive()

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Dokumente
     */
    suspend fun insertDokument(tbmvDokument: TbmvDokument) = tbmvDao.insertDokument(tbmvDokument)

    suspend fun deleteDokumentById(satzId: String) {
        withContext(Dispatchers.IO) {
            val tbmvDokument = tbmvDao.getDokumentByID(satzId)
            if (tbmvDokument != null) tbmvDao.deleteDokument(tbmvDokument)
        }
    }

    suspend fun updateDokument(tbmvDokument: TbmvDokument) = tbmvDao.updateDokument(tbmvDokument)

    suspend fun getDokumentById(dokId: String) = tbmvDao.getDokumentByID(dokId)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Lager-Basis
     */
    suspend fun insertLager(tbmvLager: TbmvLager) = tbmvDao.insertLager(tbmvLager)

    suspend fun deleteLagerById(lagerId: String) {
        withContext(Dispatchers.IO) {
            val tbmvLager = tbmvDao.getLagerByID(lagerId)
            if (tbmvLager != null) tbmvDao.deleteLager(tbmvLager)
        }
    }

    suspend fun updateLager(tbmvLager: TbmvLager) = tbmvDao.updateLager(tbmvLager)

    suspend fun getLagerById(lagerId: String) = tbmvDao.getLagerByID(lagerId)

    suspend fun getLagerListAktivSorted() = tbmvDao.getLagerListAktivSorted()

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Material und -Gruppen
     */
    suspend fun insertMat(tbmvMat: TbmvMat, noProtokoll: Boolean = false) {
        tbmvDao.upsertMat(tbmvMat)
        if (!noProtokoll) {
            val chgProtokoll = TappChgProtokoll(
                timeStamp = System.currentTimeMillis(),
                datenbank = "TbmvMat",
                satzId = tbmvMat.id,
                aktion = DB_AKTION_ADD_DS
            )
            tbmvDao.insertChgProtokoll(chgProtokoll)
            sqlSynchronized = false
        }
    }

    suspend fun updateMat(
        tbmvMat: TbmvMat,
        feldname: String? = null,
        noProtokoll: Boolean = false
    ) {
        tbmvDao.upsertMat(tbmvMat)
        if (!noProtokoll) {
            val chgProtokoll = TappChgProtokoll(
                timeStamp = System.currentTimeMillis(),
                datenbank = "TbmvMat",
                satzId = tbmvMat.id,
                feldname = feldname,
                aktion = DB_AKTION_UPDATE_DS
            )
            tbmvDao.insertChgProtokoll(chgProtokoll)
            sqlSynchronized = false
        }
    }

    suspend fun deleteMatById(matId: String) {
        withContext(Dispatchers.IO) {
            val tbmvMat = tbmvDao.getMaterialByMatID(matId)
            if (tbmvMat != null) tbmvDao.deleteMat(tbmvMat)
        }
    }

    suspend fun getMaterialByMatID(materialGuid: String) = tbmvDao.getMaterialByMatID(materialGuid)

    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe) =
        tbmvDao.insertMatGruppe(tbmvMatGruppe)

    suspend fun deleteMatGruppeById(matGruppeId: String) {
        withContext(Dispatchers.IO) {
            val tbmvMatGruppe = tbmvDao.getMatGruppeByGruppeID(matGruppeId)
            if (tbmvMatGruppe != null) tbmvDao.deleteMatGruppe(tbmvMatGruppe)
        }
    }

    suspend fun getMatGruppeAlle() = tbmvDao.getMatGruppeAll()

    // Materialdatensatz zu einem Scancode/Barcode (alle ohne Berechtigungsprüfung)
    suspend fun getMaterialByScancode(scancode: String) = tbmvDao.getMaterialByScancode(scancode)

    //  Material-Datensatz eines Betriebsmittels zu einem Scancode (inkl. Berechtigungsprüfung)
    suspend fun getAllowedMaterialFromScancode(scancode: String): TbmvMat? {
        // zuerst das Material suchen
        var tbmvMat: TbmvMat?
        withContext(Dispatchers.IO) {
            tbmvMat = tbmvDao.getMaterialByScancode(scancode)
            if (tbmvMat != null) {
                // Material gefunden, nun muss es noch in unserer (Lagerliste-)Berechtigung drin sein
                if (myUser!!.bmvAdmin == 0) {
                    val lagers = tbmvDao.getLagersOfMaterialID(tbmvMat!!.id)
                    if (lagers.isEmpty()) {
                        // wir haben kein Lager zum Betriebsmittel gefunden...
                        tbmvMat = null
                    } else {
                        val resultLagers = lagers.intersect(GlobalVars.myLagers)
                        if (resultLagers.isEmpty()) {
                            // wir haben keine Berechtigung zu einem dieser Lager...
                            tbmvMat = null
                        }
                    }
                }
            }
        }
        // das Ergebnis ist tbmvMat, wenn wir es sehen dürfen, ansonsten null
        return tbmvMat
    }

    // Material-Datensatz eines Betriebsmittels zu einem Scancode (ohne Berechtigungsprüfung)
    suspend fun getMaterialFromScancode(scancode: String): TbmvMat? {
        var tbmvMat: TbmvMat?
        withContext(Dispatchers.IO) {
            tbmvMat = tbmvDao.getMaterialByScancode(scancode)
        }
        return tbmvMat
    }


    // Betriebsmittel Datensatz
    suspend fun getBMDatenZuMatID(materialID: String): BmData? {
        var bmData: BmData
        withContext(Dispatchers.IO) {
            val material = tbmvDao.getMaterialByMatID(materialID)
            val matGruppe =
                material.let { it?.let { it1 -> tbmvDao.getMatGruppeByGruppeID(it1.matGruppeGuid) } }
            val user = material.let { it?.let { it1 -> tbmvDao.getUserById(it1.userGuid) } }
            var lagerListe = tbmvDao.getLagerWithMatInStore(materialID)?.lager
            val lager = if (lagerListe?.isEmpty() != true) {
                lagerListe?.first()
            } else {
                null
            }
            lagerListe = tbmvDao.getHauptLagerVonMaterial(materialID)?.lager
            val hauptLager = if (lagerListe?.isEmpty() != true) {
                lagerListe?.first()
            } else {
                null
            }
            val services = tbmvDao.getServiceOfMaterial(materialID)
            var nextServiceDatum: Date? = null
            if (services.size > 0) {
                run loop@{
                    services.forEach() {
                        if (it.nextServiceDatum != null) {
                            nextServiceDatum = it.nextServiceDatum
                            return@loop
                        }
                    }
                }
            }
            bmData = BmData(
                tbmvMat = material,
                tbmvMatGruppe = matGruppe,
                tsysUser = user,
                matLager = lager,
                matHautpLager = hauptLager,
                nextServiceDatum = nextServiceDatum
            )
        }
        return bmData
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material - Lager
     */
    suspend fun insertMat_Lager(tbmvMat_Lager: TbmvMat_Lager, noProtokoll: Boolean = false) {
        val matLagerId: String = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            if (!noProtokoll) tbmvMat_Lager.id = matLagerId
            tbmvDao.insertMat_Lager(tbmvMat_Lager)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvMat_Lager",
                    satzId = matLagerId,
                    aktion = DB_AKTION_ADD_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
    }

    suspend fun updateMat_Lager(tbmvMat_Lager: TbmvMat_Lager, noProtokoll: Boolean = false) {
        withContext(Dispatchers.IO) {
            tbmvDao.updateMat_Lager(tbmvMat_Lager)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvMat_Lager",
                    satzId = tbmvMat_Lager.id,
                    feldname = "Bestand",
                    aktion = DB_AKTION_UPDATE_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
    }

    suspend fun deleteMat_Lager(tbmvMat_Lager: TbmvMat_Lager, noProtokoll: Boolean = false) {
        withContext(Dispatchers.IO) {
            tbmvDao.deleteMat_Lager(tbmvMat_Lager)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvMat_Lager",
                    satzId = tbmvMat_Lager.id,
                    aktion = DB_AKTION_DELETE_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
    }

    suspend fun deleteMat_LagerById(matLagerId: String) {
        withContext(Dispatchers.IO) {
            val tbmvMat_Lager = tbmvDao.getMat_LagerByID(matLagerId)
            if (tbmvMat_Lager != null) tbmvDao.deleteMat_Lager(tbmvMat_Lager)
        }
    }

    suspend fun getMat_LagerByID(matLagerId: String) = tbmvDao.getMat_LagerByID(matLagerId)

    suspend fun getLagersBestandOfMaterialID(materialID: String) =
        tbmvDao.getLagersBestandOfMaterialID(materialID)

    suspend fun getLagerListSorted() = tbmvDao.getLagerListSorted()


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  lade sortierte und gefilterte Listen der Betriebsmittel/Materialien eines Lagers
     */
    fun getMaterialSortByMatchocde(lagerId: String) = tbmvDao.getMaterialSortByMatchocde(lagerId)
    fun getMaterialSortByScancode(lagerId: String) = tbmvDao.getMaterialSortByScancode(lagerId)
    fun getMaterialSortBySeriennr(lagerId: String) = tbmvDao.getMaterialSortBySeriennr(lagerId)
    fun getMaterialSortByHersteller(lagerId: String) = tbmvDao.getMaterialSortByHersteller(lagerId)
    fun getMaterialSortByModell(lagerId: String) = tbmvDao.getMaterialSortByModell(lagerId)
    fun getMaterialSortByStatus(lagerId: String) = tbmvDao.getMaterialSortByStatus(lagerId)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Service
     */
    suspend fun insertService(tbmvServices: TbmvServices) = tbmvDao.insertService(tbmvServices)

    suspend fun deleteServiceById(serviceId: String) {
        withContext(Dispatchers.IO) {
            val tbmvService = tbmvDao.getServiceById(serviceId)
            if (tbmvService != null) tbmvDao.deleteService(tbmvService)
        }
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Belege Transfers
     */
    // rudimentäre Neuanlagen (z.B. für Synchronisierungen)
    suspend fun insertBeleg(tbmvBelege: TbmvBelege) = tbmvDao.insertBeleg(tbmvBelege)
    suspend fun getBelegZuBelegId(belegId: String) = tbmvDao.getBelegZuBelegId(belegId)
    suspend fun getBelegPosZuBelegPosId(belegPosId: String) =
        tbmvDao.getBelegPosZuBelegPosId(belegPosId)

    // lade alle Belege abhängig von den Filtereinstellungen
    fun getBelegeToLagerAlle(lagerId: String) = tbmvDao.getBelegeToLagerAlle(lagerId)
    fun getBelegeToLagerOffen(lagerId: String) = tbmvDao.getBelegeToLagerOffen(lagerId)
    fun getBelegeToLagerInArbeit(lagerId: String) = tbmvDao.getBelegeToLagerInArbeit(lagerId)
    fun getBelegeToLagerErledigt(lagerId: String) = tbmvDao.getBelegeToLagerErledigt(lagerId)
    fun getBelegeVonLagerAlle(lagerId: String) = tbmvDao.getBelegeVonLagerAlle(lagerId)
    fun getBelegeVonLagerOffen(lagerId: String) = tbmvDao.getBelegeVonLagerOffen(lagerId)
    fun getBelegeVonLagerInArbeit(lagerId: String) = tbmvDao.getBelegeVonLagerInArbeit(lagerId)
    fun getBelegeVonLagerErledigt(lagerId: String) = tbmvDao.getBelegeVonLagerErledigt(lagerId)

    // lade alle Daten zu einem Beleg
    suspend fun getBelegDatenZuBelegId(belegId: String): BelegData? {
        var belegData: BelegData
        withContext(Dispatchers.IO) {
            val beleg = tbmvDao.getBelegZuBelegId(belegId)
            val belegUser = beleg.let { it?.let { it1 -> tbmvDao.getUserById(it1.belegUserGuid) } }
            var zielLager = beleg.let { it?.let { it1 -> tbmvDao.getLagerByID(it1.zielLagerGuid) } }
            var zielUser = beleg.let { it?.let { it1 -> tbmvDao.getUserById(it1.zielUserGuid) } }
            belegData = BelegData(
                tbmvBelege = beleg,
                belegUser = belegUser,
                zielLager = zielLager,
                zielUser = zielUser
            )
        }
        return belegData
    }

    // lade alle Belegpositionen zu einer BelegId, sortiert nach Pos (einmal als Livedata für Anzeige, einmal zu Abfragen als Pure-Liste)
    fun getBelegposVonBelegLive(belegId: String) = tbmvDao.getBelegposVonBelegLive(belegId)
    suspend fun getBelegposVonBeleg(belegId: String) = tbmvDao.getBelegposVonBeleg(belegId)


    // Neuanlage eines Transfer-Belegs mit Ziellager
    suspend fun insertBelegTransfer(tbmvLager: TbmvLager, noProtokoll: Boolean = false): String {
        val belegId: String = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            val belegTyp = "Transfer"
            val belegDatum = Date()
            val belegUserId = myUser!!.id
            val zielLagerGuid = tbmvLager.id
            val zielUserGuid = tbmvLager.userGuid
            val belegStatus = "In Arbeit"
            var tbmvBeleg = TbmvBelege(
                belegId,
                belegTyp,
                belegDatum,
                belegUserId,
                zielLagerGuid,
                zielUserGuid,
                belegStatus
            )
            tbmvDao.insertBeleg(tbmvBeleg)

            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvBelege",
                    satzId = belegId,
                    aktion = DB_AKTION_ADD_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
        return belegId
    }

    // Neuanlage eines Betriebsmittels in BelegPos
    suspend fun insertBelegPos(tbmvBelegPos: TbmvBelegPos, noProtokoll: Boolean = false) {
        val belegPosId: String = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            if (!noProtokoll) tbmvBelegPos.id = belegPosId
            tbmvDao.insertBelegPos(tbmvBelegPos)

            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvBelegPos",
                    satzId = belegPosId,
                    aktion = DB_AKTION_ADD_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
    }

    // Änderung eines Belegs mit opt. Protokollierung
    suspend fun updateBeleg(
        tbmvBelege: TbmvBelege,
        feldnamen: List<String> = listOf<String>(),
        noProtokoll: Boolean = false
    ) {
        val belegId = tbmvBelege.id
        withContext(Dispatchers.IO) {
            tbmvDao.updateBeleg(tbmvBelege)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvBelege",
                    satzId = belegId,
                    aktion = DB_AKTION_UPDATE_DS
                )
                if (feldnamen.isEmpty()) {
                    tbmvDao.insertChgProtokoll(chgProtokoll)
                } else {
                    feldnamen.forEach {
                        // für jedes geänderte Feld einen Eintrag
                        chgProtokoll.feldname = it
                        tbmvDao.insertChgProtokoll(chgProtokoll)
                    }
                }
                sqlSynchronized = false
            }
        }
    }

    suspend fun updateBelegPos(
        tbmvBelegPos: TbmvBelegPos,
        feldnamen: List<String> = listOf<String>(),
        noProtokoll: Boolean = false
    ) {
        withContext(Dispatchers.IO) {
            tbmvDao.updateBelegPos(tbmvBelegPos)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvBelegPos",
                    satzId = tbmvBelegPos.id,
                    aktion = DB_AKTION_UPDATE_DS
                )
                if (feldnamen.isEmpty()) {
                    tbmvDao.insertChgProtokoll(chgProtokoll)
                } else {
                    feldnamen.forEach {
                        // für jedes geänderte Feld einen Eintrag
                        chgProtokoll.feldname = it
                        tbmvDao.insertChgProtokoll(chgProtokoll)
                    }
                }
                sqlSynchronized = false
            }
        }
    }

    // Löschbefehle mit opt. Protokollierung
    suspend fun deleteBeleg(tbmvBelege: TbmvBelege, noProtokoll: Boolean = false) {
        val belegId = tbmvBelege.id
        withContext(Dispatchers.IO) {
            tbmvDao.deleteBeleg(tbmvBelege)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvBelege",
                    satzId = belegId,
                    aktion = DB_AKTION_DELETE_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
    }

    suspend fun deleteBelegById(satzId: String) {
        withContext(Dispatchers.IO) {
            val tbmvBeleg = tbmvDao.getBelegZuBelegId(satzId)
            if (tbmvBeleg != null) tbmvDao.deleteBeleg(tbmvBeleg)
        }
    }

    suspend fun deleteBelegPos(tbmvBelegPos: TbmvBelegPos, noProtokoll: Boolean = false) {
        val belegPosId = tbmvBelegPos.id
        withContext(Dispatchers.IO) {
            tbmvDao.deleteBelegPos(tbmvBelegPos)
            if (!noProtokoll) {
                val chgProtokoll = TappChgProtokoll(
                    timeStamp = System.currentTimeMillis(),
                    datenbank = "TbmvBelegPos",
                    satzId = belegPosId,
                    aktion = DB_AKTION_DELETE_DS
                )
                tbmvDao.insertChgProtokoll(chgProtokoll)
                sqlSynchronized = false
            }
        }
    }

    suspend fun deleteBelegPosById(satzId: String) {
        withContext(Dispatchers.IO) {
            val tbmvBelegPos = tbmvDao.getBelegPosZuBelegPosId(satzId)
            if (tbmvBelegPos != null) tbmvDao.deleteBelegPos(tbmvBelegPos)
        }
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material - Service
     */
    suspend fun insertMat_Service(tbmvMat_Service: TbmvMat_Service) =
        tbmvDao.insertMat_Service(tbmvMat_Service)

    suspend fun deleteMat_ServiceById(matServiceId: String) {
        withContext(Dispatchers.IO) {
            val tbmvMat_Service = tbmvDao.getMat_ServiceByID(matServiceId)
            if (tbmvMat_Service != null) tbmvDao.deleteMat_Service(tbmvMat_Service)
        }
    }

    suspend fun updateMat_Service(tbmvMat_Service: TbmvMat_Service) = tbmvDao.updateMat_Service(tbmvMat_Service)

    suspend fun getMat_ServiceById(matServiceId: String) = tbmvDao.getMat_ServiceByID(matServiceId)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material/Service - Dokument
     */
    suspend fun insertMatService_Dok(tbmvMatService_Dok: TbmvMatService_Dok) =
        tbmvDao.insertMatService_Dok(tbmvMatService_Dok)

    suspend fun deleteMatService_DokById(matServiceDokId: String) {
        withContext(Dispatchers.IO) {
            val tbmvMatService_Dok = tbmvDao.getMatService_DokById(matServiceDokId)
            if (tbmvMatService_Dok != null) tbmvDao.deleteMatService_Dok(tbmvMatService_Dok)
        }
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material/Service - Historie
     */
    suspend fun insertMatService_Historie(tbmvMatService_Historie: TbmvMatService_Historie) =
        tbmvDao.insertMatService_Historie(tbmvMatService_Historie)


    suspend fun deleteMatService_HistorieById(matServiceHistorieId: String) {
        withContext(Dispatchers.IO) {
            val tbmvMatService_Historie = tbmvDao.getMatService_HistorieById(matServiceHistorieId)
            if (tbmvMatService_Historie != null) tbmvDao.deleteMatService_Historie(tbmvMatService_Historie)
        }
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Service - Dokument
     */
    suspend fun insertService_Dok(tbmvService_Dok: TbmvService_Dok) =
        tbmvDao.insertService_Dok(tbmvService_Dok)

    suspend fun deleteService_DokById(serviceDokId: String) {
        withContext(Dispatchers.IO) {
            val tbmvService_Dok = tbmvDao.getService_DokById(serviceDokId)
            if (tbmvService_Dok != null) tbmvDao.deleteService_Dok(tbmvService_Dok)
        }
    }

    /** ############################################################################################
     *  Inventurlisten
     */
    suspend fun insertInventur(tbmvInventur: TbmvInventur) = tbmvDao.upsertInventur(tbmvInventur)
    suspend fun updateInventur(tbmvInventur: TbmvInventur) = tbmvDao.upsertInventur(tbmvInventur)

    suspend fun insertInventurMat(tbmvInventurMat: TbmvInventurMat) =
        tbmvDao.upsertInveturMat(tbmvInventurMat)

    suspend fun updateInventurMat(tbmvInventurMat: TbmvInventurMat) =
        tbmvDao.upsertInveturMat(tbmvInventurMat)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Änderungs - Protokollierung
     */
    suspend fun insertChgProtokoll(tappChgProtokoll: TappChgProtokoll) =
        tbmvDao.insertChgProtokoll(tappChgProtokoll)

    suspend fun getLastChgProtokoll() = tbmvDao.getLastChgProtokoll()

    suspend fun getChgProtokollGroupedList(startTime: Date, endTime: Date) =
        tbmvDao.getChgProtokollGroupedList(startTime, endTime)

    suspend fun getChgProtokollsFiltered(
        startTime: Date,
        endTime: Date,
        datenbank: String,
        satzId: String
    ) = tbmvDao.getChgProtokollsFiltered(startTime, endTime, datenbank, satzId)


    /** ############################################################################################
     *  Protokollierung der Synchronisierungen
     */
    suspend fun insertSyncReport(tappSyncReport: TappSyncReport) =
        tbmvDao.insertSyncReport(tappSyncReport)

    suspend fun getLastSyncReport() = tbmvDao.getLastSyncReport()

}