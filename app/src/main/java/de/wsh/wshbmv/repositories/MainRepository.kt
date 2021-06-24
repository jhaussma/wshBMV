package de.wsh.wshbmv.repositories

import androidx.lifecycle.viewModelScope
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import de.wsh.wshbmv.other.Constants
import de.wsh.wshbmv.other.Constants.DB_AKTION_ADD_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_UPDATE_DS
import de.wsh.wshbmv.other.GlobalVars
import de.wsh.wshbmv.other.GlobalVars.sqlSynchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class MainRepository @Inject constructor(
    val tbmvDao: TbmvDAO
) {

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * die User-Zugriffe
     */
    suspend fun insertUser(tsysUser: TsysUser) = tbmvDao.insertUser(tsysUser)
    suspend fun insertUserGruppe(tsysUserGruppe: TsysUserGruppe) =
        tbmvDao.insertUserGruppe(tsysUserGruppe)

    suspend fun insertUserInGruppe(tsysUserInGruppe: TsysUserInGruppe) =
        tbmvDao.insertUserInGruppe(tsysUserInGruppe)

    suspend fun updateUser(tsysUser: TsysUser) = tbmvDao.updateUser(tsysUser)

    suspend fun getUserByID(userID: String) = tbmvDao.getUserById(userID)
    fun getUserByLogName(userLogName: String) = tbmvDao.getUserByLogName(userLogName)

    fun getUsersActive() = tbmvDao.getUsersActive()

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Dokumente
     */
    suspend fun insertDokument(tbmvDokument: TbmvDokument) = tbmvDao.insertDokument(tbmvDokument)

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Lager-Basis
     */
    suspend fun insertLager(tbmvLager: TbmvLager) = tbmvDao.insertLager(tbmvLager)

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Material und -Gruppen
     */
    suspend fun insertMat(tbmvMat: TbmvMat, noProtokoll: Boolean = false) {
        tbmvDao.upsertMat(tbmvMat)
        if (!noProtokoll) {
            val chgProtokoll = TappChgProtokoll(
                timeStamp = System.currentTimeMillis(),
                datenbank = "TbmvMat",
                satzID = tbmvMat.id,
                Aktion = DB_AKTION_ADD_DS
            )
            tbmvDao.insertChgProtokoll(chgProtokoll)
            sqlSynchronized = false
        }
    }

    suspend fun updateMat(tbmvMat: TbmvMat, noProtokoll: Boolean = false) {
        tbmvDao.upsertMat(tbmvMat)
        if (!noProtokoll) {
            val chgProtokoll = TappChgProtokoll(
                timeStamp = System.currentTimeMillis(),
                datenbank = "TbmvMat",
                satzID = tbmvMat.id,
                Aktion = DB_AKTION_UPDATE_DS
            )
            tbmvDao.insertChgProtokoll(chgProtokoll)
            sqlSynchronized  = false
        }
    }

    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe) =
        tbmvDao.insertMatGruppe(tbmvMatGruppe)

    // Materialdatensatz zu einem Scancode/Barcode (alle ohne Berechtigungsprüfung)
    suspend fun getMaterialByScancode(scancode: String) = tbmvDao.getMaterialByScancode(scancode)

     //  Material-Datensatz eines Betriebsmittels zu einem Scancode (inkl. Berechtigungsprüfung)
    suspend fun getAllowedMaterialFromScancode(scancode: String): TbmvMat? {
        // zuerst das Material suchen
         var tbmvMat: TbmvMat?
         withContext(Dispatchers.IO) {
            tbmvMat = tbmvDao.getMaterialByScancode(scancode)
            if (tbmvMat != null) {
                // Material gefunden, nun muss es noch in unserer Lagerliste-Berechtigung drin sein
                val lagers = tbmvDao.getLagersWithMaterialId(tbmvMat!!.id)
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
        // das Ergebnis ist tbmvMat, wenn wir es sehen dürfen, ansonsten null
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
    suspend fun insertMat_Lager(tbmvMat_Lager: TbmvMat_Lager) =
        tbmvDao.insertMat_Lager(tbmvMat_Lager)

    suspend fun getLagersWithMaterialId(materialID: String) = tbmvDao.getLagersWithMaterialId(materialID)



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
    suspend fun insertService(tbmvService: TbmvService) = tbmvDao.insertService(tbmvService)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Belege Transfers
     */
    suspend fun insertBeleg(tbmvBeleg: TbmvBeleg) = tbmvDao.insertBeleg(tbmvBeleg)
    suspend fun insertBelegPos(tbmvBelegPos: TbmvBelegPos) = tbmvDao.insertBelegPos(tbmvBelegPos)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material - Service
     */
    suspend fun insertMat_Service(tbmvMat_Service: TbmvMat_Service) =
        tbmvDao.insertMat_Service(tbmvMat_Service)

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material/Service - Dokument
     */
    suspend fun insertMatService_Dok(tbmvMatService_Dok: TbmvMatService_Dok) =
        tbmvDao.insertMatService_Dok(tbmvMatService_Dok)

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Material/Service - Historie
     */
    suspend fun insertMatService_Historie(tbmvMatService_Historie: TbmvMatService_Historie) =
        tbmvDao.insertMatService_Historie(tbmvMatService_Historie)

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Relation Service - Dokument
     */
    suspend fun insertService_Dok(tbmvService_Dok: TbmvService_Dok) =
        tbmvDao.insertService_Dok(tbmvService_Dok)


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  Änderungs - Protokollierung
     */
    suspend fun insertChgProtokoll(tappChgProtokoll: TappChgProtokoll) =
        tbmvDao.insertChgProtokoll(tappChgProtokoll)

    suspend fun insertSyncReport(tappSyncReport: TappSyncReport) =
        tbmvDao.insertSyncReport(tappSyncReport)

}