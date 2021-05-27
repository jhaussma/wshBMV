package de.wsh.wshbmv.db

import androidx.lifecycle.LiveData
import androidx.room.*
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TsysUser
import de.wsh.wshbmv.db.entities.TsysUserGruppe
import de.wsh.wshbmv.db.entities.TsysUserInGruppe

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
     *  TbmvLager
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLager(tbmvLager: TbmvLager)


}