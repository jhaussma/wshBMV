package de.wsh.wshbmv.repositories

import androidx.lifecycle.MutableLiveData
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import de.wsh.wshbmv.other.Constants.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class MainRepository @Inject constructor(
    val tbmvDao: TbmvDAO
) {

    /**
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

    /**
     *  Dokumente
     */
    suspend fun insertDokument(tbmvDokument: TbmvDokument) = tbmvDao.insertDokument(tbmvDokument)

    /**
     *  Lager-Basis
     */
    suspend fun insertLager(tbmvLager: TbmvLager) = tbmvDao.insertLager(tbmvLager)
    fun getLagerByID(lagerId: String) = tbmvDao.getLagerByID(lagerId)
    fun getLagerByUserID(userGuid: String) = tbmvDao.getLagerListeByUserID(userGuid)
    fun getLagerByName(lagerName: String) = tbmvDao.getLagerByName(lagerName)

    /**
     *  Material und -Gruppen
     */
    suspend fun insertMat(tbmvMat: TbmvMat) = tbmvDao.insertMat(tbmvMat)
    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe) =
        tbmvDao.insertMatGruppe(tbmvMatGruppe)

    suspend fun getMaterialByMatID(materialID: String) = tbmvDao.getMaterialByMatID(materialID)
    suspend fun getMatGruppeByGruppeID(matGruppeId: String) =
        tbmvDao.getMatGruppeByGruppeID(matGruppeId)


    // Betriebsmittel Datensatz
    suspend fun getBMDatenZuMatID(materialID: String): BmData {
        var bmData: BmData? = null
        withContext(Dispatchers.IO) {
            val material = tbmvDao.getMaterialByMatID(materialID)
            val matGruppe =
                material.let { it?.let { it1 -> tbmvDao.getMatGruppeByGruppeID(it1.matGruppeGuid) } }
            val user = material.let { it?.let { it1 -> tbmvDao.getUserById(it1.userGuid) } }
            val lager = tbmvDao.getLagerWithMatInStore(materialID)?.lager?.first()
            val hauptlager = tbmvDao.getHauptLagerVonMaterial(materialID)?.lager?.first()
            Timber.tag(TAG).d("tbmvDao.getMaterial: ${material.toString()}")
            bmData = BmData(
                tbmvMat = material,
                tbmvMatGruppe = matGruppe,
                tsysUser = user,
                matLager = lager,
                matHautpLager = hauptlager,
                nextServiceDatum = Date()
            )
        }
        Timber.tag(TAG).d("Rückgabe von  bmData: ${bmData?.tbmvMat.toString()}")
        return bmData
    }


    /**
     * für TESTZWECKE, später korrigieren...
     */
    fun getMaterialSortByMatchocde() = tbmvDao.getMaterialSortByMatchocde()
    fun getMaterialSortByScancode() = tbmvDao.getMaterialSortByScancode()
    fun getMaterialSortBySeriennr() = tbmvDao.getMaterialSortBySeriennr()
    fun getMaterialSortByHersteller() = tbmvDao.getMaterialSortByHersteller()
    fun getMaterialSortByModell() = tbmvDao.getMaterialSortByModell()
    fun getMaterialSortByStatus() = tbmvDao.getMaterialSortByStatus()

    /**
     *  Service
     */
    suspend fun insertService(tbmvService: TbmvService) = tbmvDao.insertService(tbmvService)


    /**
     *  Belege Transfers
     */
    suspend fun insertBeleg(tbmvBeleg: TbmvBeleg) = tbmvDao.insertBeleg(tbmvBeleg)
    suspend fun insertBelegPos(tbmvBelegPos: TbmvBelegPos) = tbmvDao.insertBelegPos(tbmvBelegPos)

    /**
     *  Relation Material - Lager
     */
    suspend fun insertMat_Lager(tbmvMat_Lager: TbmvMat_Lager) =
        tbmvDao.insertMat_Lager(tbmvMat_Lager)

    //    suspend fun getMatlistOfLager(lagerId: String) = tbmvDao.getMatlistOfLager(lagerId)
    suspend fun getLagerWithMatINStore(materialID: String) =
        tbmvDao.getLagerWithMatInStore(materialID)

    suspend fun getHauptLagerVonMaterial(materialID: String) =
        tbmvDao.getHauptLagerVonMaterial(materialID)

    /**
     *  Relation Material - Service
     */
    suspend fun insertMat_Service(tbmvMat_Service: TbmvMat_Service) =
        tbmvDao.insertMat_Service(tbmvMat_Service)

    /**
     *  Relation Material/Service - Dokument
     */
    suspend fun insertMatService_Dok(tbmvMatService_Dok: TbmvMatService_Dok) =
        tbmvDao.insertMatService_Dok(tbmvMatService_Dok)

    /**
     *  Relation Material/Service - Historie
     */
    suspend fun insertMatService_Historie(tbmvMatService_Historie: TbmvMatService_Historie) =
        tbmvDao.insertMatService_Historie(tbmvMatService_Historie)

    /**
     *  Relation Service - Dokument
     */
    suspend fun insertService_Dok(tbmvService_Dok: TbmvService_Dok) =
        tbmvDao.insertService_Dok(tbmvService_Dok)


    /**
     *  Änderungs - Protokollierung
     */
    suspend fun insertChgProtokoll(tappChgProtokoll: TappChgProtokoll) =
        tbmvDao.insertChgProtokoll(tappChgProtokoll)

    suspend fun insertSyncReport(tappSyncReport: TappSyncReport) =
        tbmvDao.insertSyncReport(tappSyncReport)

}