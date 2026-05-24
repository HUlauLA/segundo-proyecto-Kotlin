package com.controldegastos.segundo_proyecto_kotlin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            AppEventos(auth)
        }
    }
}

data class Evento(
    val id: Int,
    val titulo: String,
    val fecha: String,
    val hora: String,
    val ubicacion: String,
    val descripcion: String
)

@Composable
fun AppEventos(auth: FirebaseAuth) {

    var pantallaActual by remember { mutableStateOf("login") }

    var eventos by remember {
        mutableStateOf(
            listOf(
                Evento(
                    id = 1,
                    titulo = "Conferencia de Tecnología",
                    fecha = "25/05/2026",
                    hora = "2:00 PM",
                    ubicacion = "San Salvador",
                    descripcion = "Evento sobre innovación y desarrollo móvil."
                )
            )
        )
    }

    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) }

    when (pantallaActual) {

        "login" -> PantallaLogin(
            auth = auth,
            onLoginExitoso = {
                pantallaActual = "home"
            }
        )

        "home" -> PantallaHomeEventos(
            eventos = eventos,
            onCrearEvento = {
                pantallaActual = "crear"
            },
            onEditarEvento = { evento ->
                eventoSeleccionado = evento
                pantallaActual = "editar"
            }
        )

        "crear" -> PantallaCrearEvento(
            onGuardar = { nuevoEvento ->
                val nuevoId = (eventos.maxOfOrNull { it.id } ?: 0) + 1
                eventos = eventos + nuevoEvento.copy(id = nuevoId)
                pantallaActual = "home"
            },
            onCancelar = {
                pantallaActual = "home"
            }
        )

        "editar" -> {
            eventoSeleccionado?.let { evento ->
                PantallaEditarEvento(
                    evento = evento,
                    onActualizar = { eventoActualizado ->
                        eventos = eventos.map {
                            if (it.id == eventoActualizado.id) eventoActualizado else it
                        }
                        pantallaActual = "home"
                    },
                    onEliminar = { id ->
                        eventos = eventos.filter { it.id != id }
                        pantallaActual = "home"
                    },
                    onCancelar = {
                        pantallaActual = "home"
                    }
                )
            }
        }
    }
}

@Composable
fun PantallaLogin(
    auth: FirebaseAuth,
    onLoginExitoso: () -> Unit
) {

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Gestión de Eventos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // LOGIN
        Button(
            onClick = {

                auth.signInWithEmailAndPassword(correo, password)
                    .addOnCompleteListener {

                        if (it.isSuccessful) {

                            Toast.makeText(
                                context,
                                "Inicio de sesión exitoso",
                                Toast.LENGTH_SHORT
                            ).show()

                            onLoginExitoso()

                        } else {

                            Toast.makeText(
                                context,
                                "Error al iniciar sesión",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // REGISTRO
        Button(
            onClick = {

                auth.createUserWithEmailAndPassword(correo, password)
                    .addOnCompleteListener {

                        if (it.isSuccessful) {

                            Toast.makeText(
                                context,
                                "Usuario registrado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            Toast.makeText(
                                context,
                                "Error al registrar usuario",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }
    }
}

@Composable
fun PantallaHomeEventos(
    eventos: List<Evento>,
    onCrearEvento: () -> Unit,
    onEditarEvento: (Evento) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = "Eventos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCrearEvento,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear Evento")
        }

        Spacer(modifier = Modifier.height(16.dp))

        eventos.forEach { evento ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = evento.titulo,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Fecha: ${evento.fecha}")
                    Text(text = "Hora: ${evento.hora}")
                    Text(text = "Ubicación: ${evento.ubicacion}")
                    Text(text = "Descripción: ${evento.descripcion}")

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onEditarEvento(evento) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar / Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaCrearEvento(
    onGuardar: (Evento) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = "Crear Evento",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título del evento") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = fecha,
            onValueChange = { fecha = it },
            label = { Text("Fecha") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = hora,
            onValueChange = { hora = it },
            label = { Text("Hora") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = ubicacion,
            onValueChange = { ubicacion = it },
            label = { Text("Ubicación") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    titulo.isBlank() ||
                    fecha.isBlank() ||
                    hora.isBlank() ||
                    ubicacion.isBlank() ||
                    descripcion.isBlank()
                ) {
                    Toast.makeText(
                        context,
                        "Completa todos los campos",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val nuevoEvento = Evento(
                        id = 0,
                        titulo = titulo,
                        fecha = fecha,
                        hora = hora,
                        ubicacion = ubicacion,
                        descripcion = descripcion
                    )

                    Toast.makeText(
                        context,
                        "Evento creado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    onGuardar(nuevoEvento)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Evento")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancelar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
fun PantallaEditarEvento(
    evento: Evento,
    onActualizar: (Evento) -> Unit,
    onEliminar: (Int) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf(evento.titulo) }
    var fecha by remember { mutableStateOf(evento.fecha) }
    var hora by remember { mutableStateOf(evento.hora) }
    var ubicacion by remember { mutableStateOf(evento.ubicacion) }
    var descripcion by remember { mutableStateOf(evento.descripcion) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = "Editar Evento",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título del evento") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = fecha,
            onValueChange = { fecha = it },
            label = { Text("Fecha") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = hora,
            onValueChange = { hora = it },
            label = { Text("Hora") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = ubicacion,
            onValueChange = { ubicacion = it },
            label = { Text("Ubicación") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    titulo.isBlank() ||
                    fecha.isBlank() ||
                    hora.isBlank() ||
                    ubicacion.isBlank() ||
                    descripcion.isBlank()
                ) {
                    Toast.makeText(
                        context,
                        "Completa todos los campos",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val eventoActualizado = Evento(
                        id = evento.id,
                        titulo = titulo,
                        fecha = fecha,
                        hora = hora,
                        ubicacion = ubicacion,
                        descripcion = descripcion
                    )

                    Toast.makeText(
                        context,
                        "Evento actualizado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    onActualizar(eventoActualizado)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Actualizar Evento")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                mostrarConfirmacion = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Eliminar Evento")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancelar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = {
                mostrarConfirmacion = false
            },
            title = {
                Text("Confirmar eliminación")
            },
            text = {
                Text("¿Está seguro que desea eliminar este evento?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Evento eliminado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        onEliminar(evento.id)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacion = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}