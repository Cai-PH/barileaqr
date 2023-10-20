package dev.cai.bank.BankViewModel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Switch
import android.widget.Toast

import com.google.zxing.Result
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.BarcodeEncoder
import dev.cai.bank.databinding.BankBinding
import dev.cai.bank.databinding.QrBinding
import dev.cai.bank.databinding.TransferBinding
import java.io.FileNotFoundException
import java.io.IOException

class BankActivity:AppCompatActivity() {
    private lateinit var binding: BankBinding
    private lateinit var qrBinding: QrBinding
    private lateinit var transferBinding: TransferBinding
    var lastState=0;
    var balance=0;
    var uid="";
    var scannedUID="";
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val img = result.data?.data
                binding.textView9.text=processQRCodeFromImage(uriToBitmap(this,img!!)!!)
            }
        }
        transferBinding= TransferBinding.inflate(layoutInflater)
        uid=intent.getStringExtra("UID")!!
        binding = BankBinding.inflate(layoutInflater)
        qrBinding=QrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnTransfer.setOnClickListener{
            startQRCodeScanner(this)
        }
        binding.btnGenQR.setOnClickListener{
            qrBinding.qrimg.setImageBitmap( generateQRCode(uid,200,200))
            setContentView(qrBinding.root)
        }
        binding.btnHistory.setOnClickListener{
            uploadQR()
        }
    }
    fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                return BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
    override fun onBackPressed() {
       // setContentView(binding.root)
    }
    fun uploadQR() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
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
    fun processQRCodeFromImage(bitmap: Bitmap): String? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            val hints = mutableMapOf<DecodeHintType, Any>()
            hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
            val result: Result = reader.decode(binaryBitmap, hints) as Result
            return result.text
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

    if (result.contents != null) {
        binding.textView9.text=result.contents
        Log.d("test",result.contents)

        scannedUID=result.contents
        if(scannedUID!=uid) {

            setContentView(transferBinding.root)
            transferLogic()
        } else {
            Toast.makeText(this, "You cannot send money to yourself. Scan another code", Toast.LENGTH_SHORT).show()
        }
        val scannedData = result.contents
    } else {

    }

}
    fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }

        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.createBitmap(bitMatrix)
    }
    fun transferLogic() {
        var valid =true;
        val amt = transferBinding.amt
        var amtval= 0.0
        val db = FirebaseFirestore.getInstance()
        db.collection("Users")
            .document(uid).get()
            .addOnSuccessListener {document->
                balance= document.data?.get("balance").toString().toInt()
                transferBinding.balance.text="Current Balance: $" +balance
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: $exception")
            }
        transferBinding.btnSend.setOnClickListener() {

            try {
                amtval=amt.text.toString().toDouble()
            } catch (e: NumberFormatException) {
                amt.error="Please enter a valid value"
                valid=false
            }

            if(transferBinding.amt.text.toString().isEmpty()) {
                valid = false
                amt.error="Please Enter a valid value"
            }
            if (amtval>balance) {
                valid=false
                amt.error="You dont have enough money"
            }
            if (valid) {
                transferFunds(amtval)
            }

        }
    }
    fun transferFunds(amt:Double) {
        Log.d("test123","a")
        val db = FirebaseFirestore.getInstance()
        db.collection("Users")
            .document(uid?:"")
            .update(hashMapOf("balance" to (balance-amt).toString()) as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("test123","b")
            }
            .addOnFailureListener { e ->
                Log.w("signuppatient", "Error writing document", e)
            }
        var tempbal=0;
        db.collection("Users")
            .document(scannedUID).get()
            .addOnSuccessListener {document->
                Log.d("test123","c")
                tempbal= document.data?.get("balance").toString().toInt()
                db.collection("Users")
                    .document(scannedUID?:"")
                    .update(hashMapOf("balance" to (tempbal-amt).toString()) as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.d("test123","d")
                    }
                    .addOnFailureListener { e ->
                        Log.w("signuppatient", "Error writing document", e)
                    }
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: $exception")
            }
    }
}