package dev.cai.bank.BankViewModel

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class BankActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    fun startQRCodeScanner(activity: AppCompatActivity) {
    val integrator = IntentIntegrator(activity)
    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
    integrator.setPrompt("Scan a QR Code")
    integrator.setCameraId(0)  // Use the device's default camera
    integrator.setBeepEnabled(true)
    integrator.setOrientationLocked(true)
    integrator.initiateScan()
}
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

    if (result.contents != null) {
        // You can use result.contents to access the scanned QR code's content.
        val scannedData = result.contents
        // Handle the scanned data as needed.
    } else {
        // Handle the case where the scan was canceled or failed.
    }
}
}