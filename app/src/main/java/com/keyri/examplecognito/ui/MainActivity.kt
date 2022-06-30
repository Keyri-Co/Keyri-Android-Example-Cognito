package com.keyri.examplecognito.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.keyri.examplecognito.databinding.ActivityMainBinding
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val easyKeyriAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val text = if (it.resultCode == RESULT_OK) "Authenticated" else "Failed to authenticate"

            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val payload = result.data?.getStringExtra(KEY_PAYLOAD)
            val email = result.data?.getStringExtra(KEY_EMAIL)

            if (result.resultCode == RESULT_OK && email != null && payload != null) {
                keyriAuth(email, payload)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        with(binding) {
            bCognitoLogin.setOnClickListener {
                startAuthActivity(LoginActivity::class.java)
            }

            bCognitoRegister.setOnClickListener {
                startAuthActivity(RegisterActivity::class.java)
            }
        }
    }

    private fun keyriAuth(publicUserId: String?, payload: String) {
        val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
            putExtra(AuthWithScannerActivity.APP_KEY, "NJOFSuP652zthaoHaeDmImZ2CTh4NGqX")
            putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, publicUserId)
            putExtra(AuthWithScannerActivity.PAYLOAD, payload)
        }

        easyKeyriAuthLauncher.launch(intent)
    }

    private fun startAuthActivity(clazz: Class<*>) {
        Intent(this@MainActivity, clazz).let(authLauncher::launch)
    }

    companion object {
        const val KEY_EMAIL = "EMAIL"
        const val KEY_PAYLOAD = "PAYLOAD"
    }
}
