package de.wsh.wshbmv.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.wsh.wshbmv.MyApplication
import javax.inject.Named
import javax.inject.Singleton

/**
 * wir erklären die Abhängigkeiten und die Lebensdauer dieser Abhängigkeiten
 */
@Module
@InstallIn(SingletonComponent::class) // wenn Abhängigketien für die Lebensdauer der ganzen Applikation bestehen bleiben soll
//@InstallIn(ActivityComponent::class) // wenn die Abhängigkeiten nur für Lebensdauer einer Activity gelten sollen
//@InstallIn(FragmentComponent::class) // wenn die Abhängigkeiten nur für die Lebensdauer eines Fragments gelten sollen

object AppModule {

    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): MyApplication {
        return app as MyApplication
    }

    @Singleton  // stellt sicher, dass es immer nur genau eine Instanz dieses Objekts gibt!
    @Provides
    @Named("String1")
    fun provideTestString1() = "Dies ist ein String der injected wurde"

}