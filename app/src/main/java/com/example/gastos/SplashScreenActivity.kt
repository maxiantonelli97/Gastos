package com.example.gastos

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.securepreferences.SecurePreferences

class SplashScreenActivity : AppCompatActivity() {

    private val tag = "LOGIN INFO HELP"
    private var loginSucces: MutableLiveData<Boolean> = MutableLiveData(false)
    private lateinit var securePreferences: SecurePreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenSplash = installSplashScreen()
        setContentView(R.layout.activity_splash_screen)

        auth = FirebaseAuth.getInstance()

        val resultManager: ActivityResultLauncher<IntentSenderRequest> = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential =
                        Identity.getSignInClient(this).getSignInCredentialFromIntent(result.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.googleIdToken, null)
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e("firebase", "signInWithCredential:success")
                            saveUser(auth.currentUser?.uid, auth.currentUser?.displayName, auth.currentUser?.email)
                            loginSucces.value = true
                            navigateMainActivity(auth.currentUser?.uid, auth.currentUser?.displayName, auth.currentUser?.email)
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e("firebase", "signInWithCredential:failure", task.exception)
                        }
                    }
                } catch (e: ApiException) {
                    // The ApiException status code indicates the detailed failure reason.
                }
            } else {
                navigateMainActivity(null, null, null)
            }
        }

        securePreferences = SecurePreferences(this)

        val user = securePreferences.getString("userId", null)
        val name = securePreferences.getString("userName", null)
        val mail = securePreferences.getString("userMail", null)
        if (user == null && name == null && mail == null) {
            val request = GetSignInIntentRequest.builder()
                .setServerClientId(getString(R.string.ID_cliente_google))
                .build()

            Identity.getSignInClient(this)
                .getSignInIntent(request)
                .addOnSuccessListener { result: PendingIntent ->
                    try {
                        resultManager.launch(
                            IntentSenderRequest.Builder(result).build()
                        )
                    } catch (e: SendIntentException) {
                        Log.e(tag, "Google Sign-in failed")
                    }
                }
                .addOnFailureListener { e: Exception? ->
                    Log.e(
                        tag,
                        "Google Sign-in failed",
                        e
                    )
                }
        } else {
            navigateMainActivity(user, name, mail)
        }

        screenSplash.setKeepOnScreenCondition {
            loginSucces.value == false
        }
    }

    private fun navigateMainActivity(userId: String?, userName: String?, userMail: String?) {
        val bundle = Bundle()
        if (userId != null) {
            bundle.putString("userId", userId)
        }
        if (userName != null) {
            bundle.putString("userName", userName)
        }
        if (userMail != null) {
            bundle.putString("userMail", userMail)
        }
        loginSucces.value = true
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    private fun saveUser(id: String?, name: String?, mail: String?) {
        securePreferences.edit().putString("userId", id).apply()
        securePreferences.edit().putString("userName", name).apply()
        securePreferences.edit().putString("userMail", mail).apply()
    }
}
