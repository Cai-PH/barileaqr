package dev.cai.bank

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import dev.cai.bank.BankViewModel.BankActivity
import dev.cai.bank.Signup.SignupActivity
import kotlin.math.sign

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val RC_SIGN_IN = 9001
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var pass:EditText
    private lateinit var  email:EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("196184322665-rgsncea615vuipfumuiimdmcvqevqb07.apps.googleusercontent.com")
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient.revokeAccess()

        val signInButton: Button = findViewById(R.id.signInBtn)
        signInButton.setOnClickListener {
            signIn()
        }
        pass= findViewById(R.id.inpPass)
        email= findViewById(R.id.inpEmail)
        val signup:Button = findViewById(R.id.login)
        signup.setOnClickListener{
            if(!(email.text.toString().contains("@")&&email.text.toString().contains("."))){

                email.error="Invalid Email."
            } else if (pass.text.toString().isEmpty()){
                pass.error="Password cannot be empty."
            } else {
                firebaseAuthEmail()
            }
        }
    }
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign-in failed", e)
            }
        }
    }

        private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    val uid = user?.uid

                    val db = FirebaseFirestore.getInstance()
                    val collectionReference = db.collection("Users")
                    val documentReference = collectionReference.document(uid?:"")

                    documentReference.get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val intent = Intent(this, BankActivity::class.java)
                                intent.putExtra("UID",uid)
                                startActivity(intent)
                            } else {
                                val intent = Intent(this, SignupActivity::class.java)
                                intent.putExtra("UID",uid)
                                startActivity(intent)
                            }
                        }

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this@MainActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
        private fun firebaseAuthEmail() {
            mAuth.fetchSignInMethodsForEmail(email.text.toString()).addOnCompleteListener{
                data->
                if(data.isSuccessful){
                    val signInMethods = data.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {

                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.text.toString(),pass.text.toString()).addOnCompleteListener {task ->
                            if (task.isSuccessful) {
                                val user = mAuth.currentUser
                                val uid:String= user?.uid ?: ""

                                val intent = Intent(this, SignupActivity::class.java)
                                intent.putExtra("UID",uid)
                                startActivity(intent)
                            } else {
                                val exception = task.exception
                                if (exception is FirebaseAuthInvalidCredentialsException) {
                                    pass.error="Invalid Credentials. Make sure your password is a valid password."
                                } else {
                                    pass.error=task.exception.toString()
                                }
                            }
                        }.addOnFailureListener {}
                    } else {

                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.text.toString(),pass.text.toString()).addOnCompleteListener {task ->
                            if (task.isSuccessful) {
                                val user = mAuth.currentUser
                                val uid:String= user?.uid ?: ""

                                val intent = Intent(this, BankActivity::class.java)
                                intent.putExtra("UID",uid)
                                startActivity(intent)
                            } else {
                                val exception = task.exception
                                if (exception is FirebaseAuthInvalidCredentialsException) {
                                    pass.error="Invalid Credentials. Please Try again"
                                } else {
                                    pass.error=task.exception.toString()
                                }
                            }
                        }
                    }
                }

            }


        }
    companion object {
        private const val TAG = "Signin"
    }
}