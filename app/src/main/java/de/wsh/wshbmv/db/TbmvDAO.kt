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

    @Query("SELECT * FROM TsysUser WHERE id LIKE :userID")
    fun getUserById(userID: String): TsysUser?

    @Query("SELECT * FROM TsysUser WHERE userKennung LIKE :userLogName")
    fun getUserByLogName(userLogName: String): TsysUser?

    @Query("SELECT * FROM TsysUser WHERE benutzerStatus LIKE 'Aktiv' ORDER BY kurzZeichen")
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

    @Query("SELECT * FROM TbmvLager WHERE id LIKE :lagerGuid")
    fun getLagerByID(lagerGuid: String): TbmvLager?

    @Query("SELECT * FROM TbmvLager WHERE userGuid LIKE :userGuid ORDER BY Matchcode")
    fun getLagerListeByUserID(userGuid: String): List<TbmvLager>

    @Query("SELECT * FROM TbmvLager WHERE matchcode LIKE :lagerName")
    fun getLagerByName(lagerName: String): TbmvLager?


    /**
     *  Material und Gruppen
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMat(tbmvMat: TbmvMat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe)

    @Query("SELECT * FROM TbmvMat WHERE id LIKE :materialGuid")
    fun getMaterialByMatID(materialGuid: String): TbmvMat?

    @Query("SELECT * FROM TbmvMatGruppe WHERE id LIKE :matGruppeGuid")
    fun getMatGruppeByGruppeID(matGruppeGuid: String): TbmvMatGruppe?


    /**
     * für TESTZWECKE, später korrigieren...
     */
    @Query("SELECT * FROM TbmvMat ORDER BY matchcode, beschreibung")
    fun getMaterialSortByMatchocde(): LiveData<List<TbmvMat>>

    @Query("SELECT * FROM TbmvMat ORDER BY scancode")
    fun getMaterialSortByScancode(): LiveData<List<TbmvMat>>

    @Query("SELECT * FROM TbmvMat ORDER BY seriennummer")
    fun getMaterialSortBySeriennr(): LiveData<List<TbmvMat>>

    @Query("SELECT * FROM TbmvMat ORDER BY hersteller")
    fun getMaterialSortByHersteller(): LiveData<List<TbmvMat>>

    @Query("SELECT * FROM TbmvMat ORDER BY modell")
    fun getMaterialSortByModell(): LiveData<List<TbmvMat>>

    @Query("SELECT * FROM TbmvMat ORDER BY matStatus")
    fun getMaterialSortByStatus(): LiveData<List<TbmvMat>>


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

    @Transaction
    @Query("SELECT * FROM TbmvMat_Lager WHERE lagerId LIKE :lagerId")
    fun getMatlistOfLager(lagerId: String): MatInLager?

    @Transaction
    @Query("SELECT * FROM TbmvMat_Lager WHERE matId LIKE :matId AND bestand > 0")
    fun getLagerWithMatInStore(matId: String): LagerWithMaterial?

    @Transaction
    @Query("SELECT * FROM TbmvMat_Lager WHERE matId LIKE :matId AND isDefault = 1")
    fun getHauptLagerVonMaterial(matId: String): LagerWithMaterial?



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