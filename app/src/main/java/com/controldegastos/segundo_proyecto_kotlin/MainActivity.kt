package com.controldegastos.segundo_proyecto_kotlin



//google
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

//
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient



    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()



//codigo--
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(getString(R.string.default_web_client_id))
    .requestEmail()
    .build()

googleSignInClient = GoogleSignIn.getClient(this, gso)



    val signInIntent = googleSignInClient.signInIntent
    startActivityForResult(signInIntent, 100)
}


    

override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == 100) {

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        if (task.isSuccessful) {

            Toast.makeText(
                this,
                "Inicio con Google exitoso",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}






        

        val txtCorreo = findViewById<EditText>(R.id.txtCorreo)
        val txtPassword = findViewById<EditText>(R.id.txtPassword)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegistro = findViewById<Button>(R.id.btnRegistro)

        // LOGIN
        btnLogin.setOnClickListener {

            val correo = txtCorreo.text.toString()
            val password = txtPassword.text.toString()

            auth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Inicio de sesión exitoso",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Error al iniciar sesión",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // REGISTRO
        btnRegistro.setOnClickListener {

            val correo = txtCorreo.text.toString()
            val password = txtPassword.text.toString()

            auth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Usuario registrado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Error al registrar usuario",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}
