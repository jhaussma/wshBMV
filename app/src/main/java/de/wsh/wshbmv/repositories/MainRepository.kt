package de.wsh.wshbmv.repositories

import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.TsysUser
import javax.inject.Inject

class MainRepository @Inject constructor(
    val TbmvDao: TbmvDAO
){
    suspend fun insertUser(tsysUser: TsysUser ) = TbmvDao.insertUser(tsysUser)

    suspend fun updateUser(tsysUser: TsysUser) = TbmvDao.updateUser(tsysUser)

    suspend fun getUserByID(userID: String) = TbmvDao.getUserById(userID)
    suspend fun getUserByLogName(userLogName: String) = TbmvDao.getUserByLogName(userLogName)

    fun getUsersActive() = TbmvDao.getUsersActive()


}