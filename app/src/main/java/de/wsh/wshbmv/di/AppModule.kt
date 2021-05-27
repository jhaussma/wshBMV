package de.wsh.wshbmv.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.wsh.wshbmv.MyApplication
import de.wsh.wshbmv.db.TbmvDatabase
import de.wsh.wshbmv.other.Constants.KEY_FIRST_SYNC_DONE
import de.wsh.wshbmv.other.Constants.KEY_FIRST_TIME
import de.wsh.wshbmv.other.Constants.KEY_LAGER
import de.wsh.wshbmv.other.Constants.KEY_USER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_HASH
import de.wsh.wshbmv.other.Constants.SHARED_PREFERENCES_NAME
import de.wsh.wshbmv.other.Constants.TBMV_DATABASE_NAME
import de.wsh.wshbmv.sql_db.SqlConnection
import javax.inject.Named
import javax.inject.Singleton

/**
 * wir erklären die Abhängigkeiten und deren Lebensdauer für die Gesamtlaufzeit der Applikation
 */
@Module
@InstallIn(SingletonComponent::class) // wenn Abhängigketien für die Lebensdauer der ganzen Applikation bestehen bleiben soll
//@InstallIn(ActivityComponent::class) // wenn die Abhängigkeiten nur für Lebensdauer einer Activity gelten sollen
//@InstallIn(FragmentComponent::class) // wenn die Abhängigkeiten nur für die Lebensdauer eines Fragments gelten sollen

object AppModule {

    // stellt den Zugriff auf den Context der Application her (während der gesamten Laufzeit der App)
    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): MyApplication {
        return app as MyApplication
    }

    // erklärt die TbmvDatabase
    @Singleton
    @Provides
    fun provideTbmvDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        TbmvDatabase::class.java,
        TBMV_DATABASE_NAME
    ).build()

    /**
     * erklärt die DAO-Funktionen für die TbmvDatabase
     */
    @Singleton
    @Provides
    fun provideTmbvDAO(db: TbmvDatabase) = db.getTbmvDAO()


    // erklärt die SQL-Connection
    @Singleton
    @Provides
    fun provideSqlConnection(sqlConnection: SqlConnection) = sqlConnection.dbConn()


    /**
     *  lokal gespeicherte App-Infos wie Username, akt. Lagerort, First-Time-Aufruf (wegen Installation)
     */
    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    @Named("UserName")
    fun provideUserName(sharedePref: SharedPreferences) =
        sharedePref.getString(KEY_USER_NAME, "") ?: ""

    @Singleton
    @Provides
    @Named("UserHash")
    fun provideUserHash(sharedePref: SharedPreferences) =
        sharedePref.getString(KEY_USER_HASH, "") ?: ""

    @Singleton
    @Provides
    @Named("LagerOrt")
    fun provideLagerort(sharedePref: SharedPreferences) = sharedePref.getString(KEY_LAGER, "") ?: ""

    @Singleton
    @Provides
    @Named("FirstTimeAppOpend")
    fun provideFirstTime(sharedePref: SharedPreferences) =
        sharedePref.getBoolean(KEY_FIRST_TIME, true)

    @Singleton
    @Provides
    @Named("FirstSyncDone")
    fun provideFirstSyncDone(sharedePref: SharedPreferences) =
        sharedePref.getBoolean(KEY_FIRST_SYNC_DONE, false)


}