package de.wsh.wshbmv.repositories

import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TsysUser
import de.wsh.wshbmv.db.entities.TsysUserGruppe
import de.wsh.wshbmv.db.entities.TsysUserInGruppe
import javax.inject.Inject

class MainRepository @Inject constructor(
    val TbmvDao: TbmvDAO
){
    /**
     * die User-Zugriffe
     */
    suspend fun insertUser(tsysUser: TsysUser ) = TbmvDao.insertUser(tsysUser)
    suspend fun insertUserGruppe(tsysUserGruppe: TsysUserGruppe) = TbmvDao.insertUserGruppe(tsysUserGruppe)
    suspend fun insertUserInGruppe(tsysUserInGruppe: TsysUserInGruppe) = TbmvDao.insertUserInGruppe(tsysUserInGruppe)

    suspend fun updateUser(tsysUser: TsysUser) = TbmvDao.updateUser(tsysUser)

    suspend fun getUserByID(userID: String) = TbmvDao.getUserById(userID)
    suspend fun getUserByLogName(userLogName: String) = TbmvDao.getUserByLogName(userLogName)

    fun getUsersActive() = TbmvDao.getUsersActive()

    /**
     *  Lager-Basis
     */
    suspend fun insertLager(tbmvLager: TbmvLager) = TbmvDao.insertLager(tbmvLager)


}