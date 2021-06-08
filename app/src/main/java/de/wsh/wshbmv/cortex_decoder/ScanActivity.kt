package de.wsh.wshbmv.cortex_decoder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codecorp.camera.Resolution
import com.codecorp.cortexdecoderlibrary.BuildConfig
import com.codecorp.decoder.CortexDecoderLibrary
import com.codecorp.decoder.CortexDecoderLibraryCallback
import com.codecorp.licensing.LicenseCallback
import com.codecorp.licensing.LicenseStatusCode
import com.codecorp.symbology.Symbologies
import com.codecorp.symbology.SymbologyType
import com.codecorp.util.Codewords
import com.codecorp.util.Utilities
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.databinding.ActivityScanBinding
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class ScanActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, CortexDecoderLibraryCallback, LicenseCallback {

    private lateinit var binding: ActivityScanBinding

    val PERMISSION_LIST:Array<String> = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    val PERMISSION_REQUEST = 5678
    val CAMERA_API = "camera2"

    var mCortexDecoderLibrary:CortexDecoderLibrary? = null

    val EDK_ACTIVATE_LICENSE_KEY = "0R71aoAa3FQ3L4pZlzrIs1Be/8Vj6DfYRnDL6zsv0bK6PRkeuftE0yYm99Yvk9/v/B9dRiHnas/FrhtRacNL9/5HJ8Espns8hATFBN4hBoiEvznSMOFSGFla8qWik4fh7n01D69b+vHaF8FQq6xd2BQ9A/imr2aVBFYM7jQo0QC11b1HCGbm2uD1bA7+NlRyHui6ThHRSbQI98mf+4jjryn79mC7u9kNMFsXTGzz95gUU9qCzmkEHs1tuDH9SVJTMaXQ4Ze6dqsBruw1CtW62tal7S4ZO5a3A3UXXi/DQXPz1QhM9SAlkehpENBt+flbIYmNLCDq+edAbJmnqs0X4rE/Ao9ozPxM3ziBvWY0JGjBttQCuNgcJOSnMlqn6NTgsl8t9lU0pCc2dRYc06V06g=="
    val EDK_CUSTOMERID = "JOE042620210001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkSDKLevel()
    }

    private fun checkSDKLevel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager() == false) {
                val uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                startActivity(intent)
            }

            checkPermission()

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }else{
            checkPermission()
        }
    }

    private fun checkPermission(){
        if(EasyPermissions.hasPermissions(this, *PERMISSION_LIST)){
            initSDK()
        }else{
            EasyPermissions.requestPermissions(this, "please allow all permissions", PERMISSION_REQUEST, *PERMISSION_LIST)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initSDK(){
        mCortexDecoderLibrary = CortexDecoderLibrary.sharedObject(applicationContext, CAMERA_API)
        mCortexDecoderLibrary?.setLicenseCallback(this)
        mCortexDecoderLibrary?.setCallback(this)
        mCortexDecoderLibrary?.setEDKCustomerID(EDK_CUSTOMERID)
        mCortexDecoderLibrary?.activateLicense(EDK_ACTIVATE_LICENSE_KEY)

        binding.activityMainButtonToSdkVersion.setText("SDK Version: "+mCortexDecoderLibrary?.sdkVersion)
    }

    private fun setSDK(){
        binding.activityScanFrame.addView(mCortexDecoderLibrary?.cameraPreview, 0)
        mCortexDecoderLibrary?.setDecoderResolution(Resolution.Resolution_1920x1080)
    }

    private fun doStartDecoding(){
        mCortexDecoderLibrary?.startCameraPreview()
        mCortexDecoderLibrary?.startDecoding()
    }

    private fun doStopDecoding(){
        mCortexDecoderLibrary?.stopDecoding()
        mCortexDecoderLibrary?.stopCameraPreview()
    }

    private fun showBarcodeResult(barcode: String?, type: SymbologyType?){
        val typeStr = Utilities.stringFromSymbologyType(type)
        binding.activityScanBarcodeResult.setText(typeStr+"\n"+barcode)
    }

    override fun onDestroy() {
        super.onDestroy()
        doStopDecoding()
        mCortexDecoderLibrary?.closeCamera()
        mCortexDecoderLibrary?.closeSharedObject()
    }

//enabled Symbology
    private fun enableQR(enable: Boolean) {
        val qrProperties = Symbologies.QRProperties()
        qrProperties.setEnabled(this, enable)
    }

    private fun enableCode128(enable: Boolean) {
        val code128Properties = Symbologies.Code128Properties()
        code128Properties.setEnabled(this, enable)
    }

    private fun enableUPCE(enable: Boolean) {
        val upcaProperties = Symbologies.UPCAProperties()
        upcaProperties.setEnabled(this, enable)
    }

    private fun enableEAN8(enable: Boolean) {
        val eaN8Properties = Symbologies.EAN8Properties()
        eaN8Properties.setEnabled(this, enable)
    }

    private fun enableEAN13(enable: Boolean) {
        val eaN13Properties = Symbologies.EAN13Properties()
        eaN13Properties.setEnabled(this, enable)
    }

    private fun enableDataMatrix(enable: Boolean) {
        val dataMatrixPresentException = Symbologies.DataMatrixProperties()
        dataMatrixPresentException.setEnabled(this, enable)
    }
//enabled Symbology


//Easy Permission
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "please allow all permissions", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if(requestCode == PERMISSION_REQUEST && perms.size == PERMISSION_LIST.size){
            Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show()
            initSDK()
        }else{
            Toast.makeText(this, "please allow all permissions", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
//Easy Permission

//CortexScan callback
    override fun receiveBarcodeCorners(p0: IntArray?) {

    }

    override fun receiveMultipleBarcodeCorners(p0: MutableList<IntArray>?) {

    }

    override fun barcodeDecodeFailed(p0: Boolean) {

    }

    override fun multiFrameDecodeCount(p0: Int) {

    }

    override fun receivedDecodedData(barcode: String?, type: SymbologyType?) {
        runOnUiThread(Runnable {  showBarcodeResult(barcode, type) })
    }

    override fun receivedMultipleDecodedData(p0: Array<out String>?, p1: Array<out SymbologyType>?) {

    }

    override fun receivedDecodedCodewordsData(p0: Codewords?) {

    }

    override fun onDeviceIDResult(p0: Int, p1: String?) {

    }

    override fun onActivationResult(p0: LicenseStatusCode?) {
        if(p0 == LicenseStatusCode.LicenseStatus_LicenseValid){
            enableQR(true)
            enableEAN8(true)
            enableEAN13(true)
            enableCode128(true)
            enableUPCE(true)
            enableUPCE(true)
            enableDataMatrix(true)

            setSDK()
            doStartDecoding()
        }else{
            Toast.makeText(this, "license invalid", Toast.LENGTH_SHORT).show()
        }
    }
//CortexScan callback

}
