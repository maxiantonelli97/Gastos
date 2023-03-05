package com.example.gastos

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.gastos.databinding.ActivityMainBinding
import com.example.gastos.entity.UserModel
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.securepreferences.SecurePreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var securePreferences: SecurePreferences
    private var logueado: Boolean? = null
    private lateinit var resultManager: ActivityResultLauncher<IntentSenderRequest>
    private val tag = "LOGIN INFO HELP"
    private lateinit var item: MenuItem
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        securePreferences = SecurePreferences(this)

        val user = intent.hasExtra("userId") && intent.hasExtra("userName")

        logueado = user

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        resultManager = registerForActivityResult(
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
                            usuarioLogueado()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e("firebase", "signInWithCredential:failure", task.exception)
                        }
                    }
                } catch (e: ApiException) {
                    // The ApiException status code indicates the detailed failure reason.
                }
            } else {
                // TODO error login
            }
        }

        navController = if (logueado == true) {
            findNavController(R.id.nav_host_fragment_connect)
        } else {
            findNavController(R.id.nav_host_fragment_disconnect)
        }
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        val menuItem = menu.findItem(R.id.action_log)
        this.item = menuItem
        if (logueado == true) {
            usuarioLogueado()
        } else {
            usuarioNoLogueado()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_log -> {
                if (logueado == true) {
                    deleteUser()
                    usuarioNoLogueado()
                } else {
                    this.item = item
                    signIn()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = if (logueado == true) {
            findNavController(R.id.nav_host_fragment_connect)
        } else {
            findNavController(R.id.nav_host_fragment_disconnect)
        }
        return navController.navigateUp(appBarConfiguration) ||
            super.onSupportNavigateUp()
    }

    private fun usuarioLogueado() {
        item.isVisible = true
        item.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_logout_24, null)
        logueado = true
        binding.progressBar.visibility = View.GONE
        binding.mainLayout.visibility = View.VISIBLE
        binding.navHostFragmentDisconnectView.visibility = View.GONE
        binding.navHostFragmentConnectView.visibility = View.VISIBLE
        binding.toolbar.title = getString(R.string.home_fragment_label)
        navController.navigate(R.id.nav_homeFragment)
    }

    fun usuarioNoLogueado() {
        item.isVisible = false
        logueado = false
        binding.toolbar.title = getString(R.string.action_logIn)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        binding.progressBar.visibility = View.GONE
        binding.mainLayout.visibility = View.VISIBLE
        binding.navHostFragmentConnectView.visibility = View.GONE
        binding.navHostFragmentDisconnectView.visibility = View.VISIBLE
    }

    fun setLabel(label: String) {
        supportActionBar?.title = label
    }

    private fun saveUser(id: String?, name: String?, mail: String?) {
        securePreferences.edit().putString("userId", id).apply()
        securePreferences.edit().putString("userName", name).apply()
        securePreferences.edit().putString("userMail", mail).apply()
    }

    private fun deleteUser() {
        securePreferences.edit().remove("userId").apply()
        securePreferences.edit().remove("userName").apply()
        securePreferences.edit().remove("userMail").apply()
    }

    fun getuser(): UserModel? {
        val user = UserModel(
            securePreferences.getString("userId", null),
            securePreferences.getString("userName", null),
            securePreferences.getString("userMail", null)
        )
        return if (user.id == null || user.name == null || user.mail == null) {
            null
        } else {
            user
        }
    }

    fun signIn() {
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
                } catch (e: IntentSender.SendIntentException) {
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
    }

    fun isOnline(): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        Toast.makeText(applicationContext, getString(R.string.conectese), Toast.LENGTH_LONG).show()
        return false
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.nav_homeFragment) {
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }
}
