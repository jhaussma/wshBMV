package de.wsh.wshbmv.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.wsh.wshbmv.MyApplication
import de.wsh.wshbmv.db.TbmvDatabase
import de.wsh.wshbmv.other.Constants.TBMV_DATABASE_NAME
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

    // erklärt die DAO-Funktionen für die TbmvDatabase
    @Singleton
    @Provides
    fun provideTmbvDAO(db: TbmvDatabase) = db.getTbmvDAO()

// nachfolgendes kann wieder weg und war nur für Versuche gedacht...
    @Singleton  // stellt sicher, dass es immer nur genau eine Instanz dieses Objekts gibt!
    @Provides
    @Named("String1")
    fun provideTestString1() = "Dies ist ein String der injected wurde"

}