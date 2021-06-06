package de.wsh.wshbmv.cortex_decoder

import android.app.Activity
import com.codecorp.decoder.CortexDecoderLibraryCallback
import com.codecorp.symbology.SymbologyType
import com.codecorp.util.Codewords

class CortexActivity : Activity(), CortexDecoderLibraryCallback{





    override fun receivedDecodedData(p0: String?, p1: SymbologyType?) {
        TODO("Not yet implemented")
    }

    override fun receivedMultipleDecodedData(
        p0: Array<out String>?,
        p1: Array<out SymbologyType>?
    ) {
        TODO("Not yet implemented")
    }

    override fun receiveBarcodeCorners(p0: IntArray?) {
        TODO("Not yet implemented")
    }

    override fun receiveMultipleBarcodeCorners(p0: MutableList<IntArray>?) {
        TODO("Not yet implemented")
    }

    override fun receivedDecodedCodewordsData(p0: Codewords?) {
        TODO("Not yet implemented")
    }

    override fun barcodeDecodeFailed(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun multiFrameDecodeCount(p0: Int) {
        TODO("Not yet implemented")
    }

}