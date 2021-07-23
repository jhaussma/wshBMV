package de.wsh.wshbmv.ui



interface FragCommunicator {
    // Übergabe der MaterialID zu einem Material-Datensatz
    fun passBmDataID(materialId: String)

    // Übergabe einer BelegID zu einem Beleg mit Positionen
    fun passBelegID(belegId: String)



}