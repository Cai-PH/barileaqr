package dev.cai.bank.Signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import dev.cai.bank.BankViewModel.BankActivity
import dev.cai.bank.R
import dev.cai.bank.databinding.ActivitySignupBinding

class SignupActivity:AppCompatActivity(){
    private lateinit var activitySignupBinding: ActivitySignupBinding
    var valid=true
    var name="";
    var balance=0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activitySignupBinding=ActivitySignupBinding.inflate(layoutInflater)

        setContentView(activitySignupBinding.root)

        Log.d("sendhelp","a")
        activitySignupBinding.btnsubmit.setOnClickListener{
            Log.d("sendhelp","b")
            if (validateInputs()) {
                val uData = HashMap<String,Any>()

                uData["Name"] =name
                uData["balance"] =balance
                val uid=intent.getStringExtra("UID")?:"";
                val db = FirebaseFirestore.getInstance()
                db.collection("Users")
                    .document(uid?:"")
                    .set(uData)
                    .addOnSuccessListener {
                        Log.d("sendhelp","d")
                        val intent = Intent(this, BankActivity::class.java)
                        startActivity(intent);
                    }
                    .addOnFailureListener { e ->
                        Log.w("signuppatient", "Error writing document", e)
                    }
            } else {

            }
        }

    }
    fun validateInputs():Boolean {
        Log.d("sendhelp","c")
        valid=true
        name=activitySignupBinding.inpName.text.toString()
        if(name.isEmpty()) {
            activitySignupBinding.inpName.error="Please enter a valid input"
            return false;
        }
        if (activitySignupBinding.inpBal.text.toString().isEmpty()){
            activitySignupBinding.inpBal.error="Please enter a valid value"
            return false;
        }
        try {
            balance=activitySignupBinding.inpBal.text.toString().toInt()
            return true
        } catch (e: NumberFormatException) {
            activitySignupBinding.inpBal.error="Please enter a valid value"
            balance=0;
            return false;
        }
        return false
    }
}