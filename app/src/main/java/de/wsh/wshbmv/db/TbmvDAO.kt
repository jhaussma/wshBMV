package de.wsh.wshbmv.db

import androidx.lifecycle.LiveData
import androidx.room.*
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*

@Dao
interface TbmvDAO {

    /**
     * TsysUser - Usertabelle
      */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(tsysUser: TsysUser)

    @Update
    suspend fun updateUser(tsysUser: TsysUser)

    @Query("SELECT * FROM TsysUser WHERE id = :userID")
    suspend fun getUserById(userID: String): TsysUser?

    @Query("SELECT * FROM TsysUser WHERE UserKennung = :userLogName")
    suspend fun getUserByLogName(userLogName: String): TsysUser?

    @Query("SELECT * FROM TsysUser WHERE BenutzerStatus = 'Aktiv' ORDER BY KurzZeichen")
    fun getUsersActive(): LiveData<List<TsysUser>>

    /**
     *  TsysUserGruppe
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGruppe(tsysUserGruppe: TsysUserGruppe)

    /**
     *  TsysUserInGruppe (Relation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInGruppe(tsysUserInGruppe: TsysUserInGruppe)

    /**
     *  Dokumente
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDokument(tbmvDokument: TbmvDokument)


    /**
     *  TbmvLager
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLager(tbmvLager: TbmvLager)

    /**
     *  Material und Gruppen
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMat(tbmvMat: TbmvMat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe)

    /**
     *  Service - Planung
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(tbmvService: TbmvService)


    /**
     *  Belege - Transfers
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeleg(tbmvBeleg: TbmvBeleg)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBelegPos(tbmvBelegPos: TbmvBelegPos)

    /**
     *  Relation Material - Lager
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMat_Lager(tbmvMat_Lager: TbmvMat_Lager)

    /**
     *  Relation Material - Service
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMat_Service(tbmvMat_Service: TbmvMat_Service)

    /**
     *  Relation Material/Service - Dokument
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatService_Dok(tbmvMatService_Dok: TbmvMatService_Dok)

    /**
     *  Relation Material/Service - Historie
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatService_Historie(tbmvMatService_Historie: TbmvMatService_Historie)

    /**
     *  Relation Service - Dokument
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService_Dok(tbmvService_Dok: TbmvService_Dok)


    /**
     *  Änderungsverwaltung
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChgProtokoll(tappChgProtokoll: TappChgProtokoll)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncReport(tappSyncReport: TappSyncReport)



}