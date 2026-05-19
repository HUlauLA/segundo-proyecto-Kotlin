package com.controldegastos.segundo_proyecto_kotlin

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var resultado by remember {
                    mutableStateOf("Cargando evento desde Firebase...")
                }

                LaunchedEffect(Unit) {
                    leerEventos { texto ->
                        resultado = texto
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = resultado)
                }
            }
        }
    }

    private fun leerEventos(onResultado: (String) -> Unit) {
        db.collection("eventos")
            .get()
            .addOnSuccessListener { documentos ->
                if (documentos.isEmpty) {
                    onResultado("No hay eventos registrados")
                    Log.d("FIREBASE", "No hay eventos registrados")
                } else {
                    val listaEventos = mutableListOf<String>()

                    for (documento in documentos) {
                        val titulo = documento.getString("titulo") ?: "Sin título"
                        val descripcion = documento.getString("descripcion") ?: "Sin descripción"
                        val fecha = documento.getString("fecha") ?: "Sin fecha"
                        val hora = documento.getString("hora") ?: "Sin hora"
                        val ubicacion = documento.getString("ubicacion") ?: "Sin ubicación"

                        val evento = """
                            Título: $titulo
                            Descripción: $descripcion
                            Fecha: $fecha
                            Hora: $hora
                            Ubicación: $ubicacion
                        """.trimIndent()

                        listaEventos.add(evento)

                        Log.d("FIREBASE", "Evento leído: $evento")
                    }

                    onResultado(listaEventos.joinToString("\n\n"))
                }
            }
            .addOnFailureListener { error ->
                onResultado("Error al leer Firestore: ${error.message}")
                Log.e("FIREBASE", "Error al leer eventos", error)
            }
    }
}