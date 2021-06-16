package de.wsh.wshbmv.ui

import android.net.Uri

interface FragCommunicator {
    // Übergabe der MaterialID zu einem Material-Datensatz
    fun passBmDataID(materialId: String)

    // Übergabe eines Bildes zu einem Betriebsmittel
    fun passNewPhoto(uri: Uri)

}