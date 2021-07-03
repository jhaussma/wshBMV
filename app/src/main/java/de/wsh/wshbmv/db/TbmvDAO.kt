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
    suspend fun getUserById(userID: String): TsysUser?

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
    suspend fun getLagerByID(lagerGuid: String): TbmvLager?

    @Query("SELECT * FROM TbmvLager WHERE userGuid LIKE :userGuid ORDER BY Matchcode")
    suspend fun getLagerListeByUserID(userGuid: String): List<TbmvLager>

    @Query("SELECT * FROM TbmvLager WHERE matchcode LIKE :lagerName")
    fun getLagerByName(lagerName: String): LiveData<TbmvLager>?

    @Query("SELECT * FROM TbmvLager ORDER BY Typ, Matchcode")
    suspend fun getLagerListSorted(): List<TbmvLager>


    /**
     *  Material und Gruppen
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMat(tbmvMat: TbmvMat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatGruppe(tbmvMatGruppe: TbmvMatGruppe)

    @Query("SELECT * FROM TbmvMat WHERE id LIKE :materialGuid")
    suspend fun getMaterialByMatID(materialGuid: String): TbmvMat?

    @Query("SELECT * FROM TbmvMat WHERE scancode LIKE :scancode")
    suspend fun getMaterialByScancode(scancode: String): TbmvMat?

    @Query("SELECT * FROM TbmvMatGruppe WHERE id LIKE :matGruppeGuid")
    suspend fun getMatGruppeByGruppeID(matGruppeGuid: String): TbmvMatGruppe?


    /**
     *  Service - Arten
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(tbmvService: TbmvService)


    /**
     *  Belege - Transfers
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeleg(tbmvBeleg: TbmvBeleg)

    @Delete
    suspend fun deleteBeleg(tbmvBeleg: TbmvBeleg)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBelegPos(tbmvBelegPos: TbmvBelegPos)

    @Delete
    suspend fun deleteBelegPos(tbmvBelegPos: TbmvBelegPos)

    @Update
    suspend fun updateBeleg(tbmvBeleg: TbmvBeleg)

    @Update
    suspend fun updateBelegPos(tbmvBelegPos: TbmvBelegPos)


    // Abfragen Transferlisten...
    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE zielLagerGuid = :lagerId ORDER BY belegDatum DESC")
    fun getBelegeToLagerAlle(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE zielLagerGuid = :lagerId AND belegStatus = 'Erfasst' ORDER BY belegDatum DESC")
    fun getBelegeToLagerOffen(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE zielLagerGuid = :lagerId AND belegStatus = 'In Arbeit' ORDER BY belegDatum DESC")
    fun getBelegeToLagerInArbeit(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE zielLagerGuid = :lagerId AND belegStatus IN ('Fertig','Storniert')  ORDER BY belegDatum DESC")
    fun getBelegeToLagerErledigt(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE id IN (SELECT belegId FROM TbmvBelegPos WHERE vonLagerGuid = :lagerId GROUP BY belegId) ORDER BY belegDatum DESC")
    fun getBelegeVonLagerAlle(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE id IN (SELECT belegId FROM TbmvBelegPos WHERE vonLagerGuid = :lagerId GROUP BY belegId) AND belegStatus = 'Erfasst' ORDER BY belegDatum DESC")
    fun getBelegeVonLagerOffen(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE id IN (SELECT belegId FROM TbmvBelegPos WHERE vonLagerGuid = :lagerId GROUP BY belegId) AND belegStatus = 'In Arbeit' ORDER BY belegDatum DESC")
    fun getBelegeVonLagerInArbeit(lagerId: String): LiveData<List<BelegAndZielort>>

    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE id IN (SELECT belegId FROM TbmvBelegPos WHERE vonLagerGuid = :lagerId GROUP BY belegId) AND belegStatus IN ('Fertig','Storniert') ORDER BY belegDatum DESC")
    fun getBelegeVonLagerErledigt(lagerId: String): LiveData<List<BelegAndZielort>>


    // Abfrage Einzelbelege (und -positionen)
    @Transaction
    @Query("SELECT * FROM TbmvBeleg WHERE id = :belegId")
    suspend fun getBelegZuBelegId(belegId: String): TbmvBeleg?

    @Transaction
    @Query("SELECT * FROM TbmvBelegPos WHERE belegId = :belegId ORDER BY pos")
    fun getBelegposVonBelegLive(belegId: String): LiveData<List<BelegposAndMaterialAndLager>>

    @Transaction
    @Query("SELECT * FROM TbmvBelegPos WHERE belegId = :belegId ORDER BY pos")
    suspend fun getBelegposVonBeleg(belegId: String): List<TbmvBelegPos>


    /**
     *  Relation Material - Lager
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMat_Lager(tbmvMat_Lager: TbmvMat_Lager)

    @Transaction
    @Query("SELECT * FROM TbmvMat_Lager WHERE lagerId LIKE :lagerId")
    suspend fun getMatlistOfLager(lagerId: String): MatInLager?

    @Transaction
    @Query("SELECT TbmvLager.* FROM TbmvLager INNER JOIN TbmvMat_Lager ON TbmvLager.id = TbmvMat_Lager.lagerId WHERE TbmvMat_Lager.matId = :matId")
    suspend fun getLagersWithMaterialId(matId: String): List<TbmvLager>

    @Transaction
    @Query("SELECT * FROM TbmvMat_Lager WHERE matId LIKE :matId AND bestand > 0")
    suspend fun getLagerWithMatInStore(matId: String): LagerWithMaterial?

    @Transaction
    @Query("SELECT * FROM TbmvMat_Lager WHERE matId LIKE :matId AND isDefault = 1")
    suspend fun getHauptLagerVonMaterial(matId: String): LagerWithMaterial?

    // sortierte / gefilterte Abfragen der Betriebsmittel eines Lagers
    @Transaction
    @Query("SELECT TbmvMat.* FROM TbmvMat INNER JOIN TbmvMat_Lager ON TbmvMat.id = TbmvMat_Lager.matId WHERE TbmvMat_Lager.lagerId = :lagerId ORDER BY matchcode, beschreibung")
    fun getMaterialSortByMatchocde(lagerId: String): LiveData<List<TbmvMat>>

    @Transaction
    @Query("SELECT TbmvMat.* FROM TbmvMat INNER JOIN TbmvMat_Lager ON TbmvMat.id = TbmvMat_Lager.matId WHERE TbmvMat_Lager.lagerId = :lagerId ORDER BY scancode")
    fun getMaterialSortByScancode(lagerId: String): LiveData<List<TbmvMat>>

    @Transaction
    @Query("SELECT TbmvMat.* FROM TbmvMat INNER JOIN TbmvMat_Lager ON TbmvMat.id = TbmvMat_Lager.matId WHERE TbmvMat_Lager.lagerId = :lagerId ORDER BY seriennummer")
    fun getMaterialSortBySeriennr(lagerId: String): LiveData<List<TbmvMat>>

    @Transaction
    @Query("SELECT TbmvMat.* FROM TbmvMat INNER JOIN TbmvMat_Lager ON TbmvMat.id = TbmvMat_Lager.matId WHERE TbmvMat_Lager.lagerId = :lagerId ORDER BY hersteller")
    fun getMaterialSortByHersteller(lagerId: String): LiveData<List<TbmvMat>>

    @Transaction
    @Query("SELECT TbmvMat.* FROM TbmvMat INNER JOIN TbmvMat_Lager ON TbmvMat.id = TbmvMat_Lager.matId WHERE TbmvMat_Lager.lagerId = :lagerId ORDER BY modell")
    fun getMaterialSortByModell(lagerId: String): LiveData<List<TbmvMat>>

    @Transaction
    @Query("SELECT TbmvMat.* FROM TbmvMat INNER JOIN TbmvMat_Lager ON TbmvMat.id = TbmvMat_Lager.matId WHERE TbmvMat_Lager.lagerId = :lagerId ORDER BY matStatus")
    fun getMaterialSortByStatus(lagerId: String): LiveData<List<TbmvMat>>


    /**
     *  Relation Material - Service
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMat_Service(tbmvMat_Service: TbmvMat_Service)

    @Transaction
    @Query("SELECT * FROM TbmvMat_Service WHERE MatID = :materialId ORDER BY NextServiceDatum")
    suspend fun getServiceOfMaterial(materialId: String): List<TbmvMat_Service>


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