package de.wsh.wshbmv.repositories

import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import javax.inject.Inject

class MainRepository @Inject constructor(
    val TbmvDao: TbmvDAO
) {

    /**
     * die User-Zugriffe
     */
    suspend fun insertUser(tsysUser: TsysUser) = TbmvDao.insertUser(tsysUser)
    suspend fun insertUserGruppe(tsysUserGruppe: TsysUserGruppe) =
        TbmvDao.insertUserGruppe(tsysUserGruppe)

    suspend fun insertUserInGruppe(tsysUserInGruppe: TsysUserInGruppe) =
        TbmvDao.insertUserInGruppe(tsysUserInGruppe)

    fun updateUser(tsysUser: TsysUser) = TbmvDao.updateUser(tsysUser)

    suspend fun getUserByID(userID: String) = TbmvDao.getUserById(userID)
    fun getUserByLogName(userLogName: String) = TbmvDao.getUserByLogName(userLogName)

    fun getUsersActive() = TbmvDao.getUsersActive()

    /**
     *  Dokumente
     */
    suspend fun insertDokument(tbmvDokument: TbmvDokument) = TbmvDao.insertDokument(tbmvDokument)

    /**
     *  Lager-Basis
     */
    suspend fun insertLager(tbmvLager: TbmvLager) = TbmvDao.insertLager(tbmvLager)
    fun getLagerByUserID(userGuid: String) = TbmvDao.getLagerByUserID(userGuid)
    fun getLagerByName(lagerName: String) = TbmvDao.getLagerByName(lagerName)

    /**
     *  Material und -Gruppen
     */
    suspend fun insertMat(tbmvMat: TbmvMat) = TbmvDao.insertMat(tbmvMat)
    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe) =
        TbmvDao.insertMatGruppe(tbmvMatGruppe)

    /**
     *  Service
     */
    suspend fun insertService(tbmvService: TbmvService) = TbmvDao.insertService(tbmvService)


    /**
     *  Belege Transfers
     */
    suspend fun insertBeleg(tbmvBeleg: TbmvBeleg) = TbmvDao.insertBeleg(tbmvBeleg)
    suspend fun insertBelegPos(tbmvBelegPos: TbmvBelegPos) = TbmvDao.insertBelegPos(tbmvBelegPos)

    /**
     *  Relation Material - Lager
     */
    suspend fun insertMat_Lager(tbmvMat_Lager: TbmvMat_Lager) =
        TbmvDao.insertMat_Lager(tbmvMat_Lager)

    /**
     *  Relation Material - Service
     */
    suspend fun insertMat_Service(tbmvMat_Service: TbmvMat_Service) =
        TbmvDao.insertMat_Service(tbmvMat_Service)

    /**
     *  Relation Material/Service - Dokument
     */
    suspend fun insertMatService_Dok(tbmvMatService_Dok: TbmvMatService_Dok) =
        TbmvDao.insertMatService_Dok(tbmvMatService_Dok)

    /**
     *  Relation Material/Service - Historie
     */
    suspend fun insertMatService_Historie(tbmvMatService_Historie: TbmvMatService_Historie) =
        TbmvDao.insertMatService_Historie(tbmvMatService_Historie)

    /**
     *  Relation Service - Dokument
     */
    suspend fun insertService_Dok(tbmvService_Dok: TbmvService_Dok) =
        TbmvDao.insertService_Dok(tbmvService_Dok)


    /**
     *  Änderungs - Protokollierung
     */
    suspend fun insertChgProtokoll(tappChgProtokoll: TappChgProtokoll) =
        TbmvDao.insertChgProtokoll(tappChgProtokoll)

    suspend fun insertSyncReport(tappSyncReport: TappSyncReport) =
        TbmvDao.insertSyncReport(tappSyncReport)

}