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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity

//CREDENCIALES PARA EL USUARIO ADMINISTRADOR: admin@gmail.com ; admin123

private const val ADMIN_EMAIL = "admin@gmail.com"
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

//Para guardar el usuario en Firebase
fun guardarUsuarioEnFirestore(
    uid: String,
    nombre: String,
    correo: String,
    onExito: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val rol = if (correo.equals(ADMIN_EMAIL, ignoreCase = true)) {
        "admin"
    } else {
        "usuario"
    }

    val usuario = hashMapOf(
        "uid" to uid,
        "nombre" to nombre,
        "correo" to correo,
        "rol" to rol,
        "creadoEn" to Timestamp.now()
    )

    db.collection("usuarios")
        .document(uid)
        .set(usuario)
        .addOnSuccessListener {
            onExito()
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al guardar usuario")
        }
}

data class Evento(
    val id: String = "",
    val titulo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val ubicacion: String = "",
    val descripcion: String = ""
)

data class UsuarioInscrito(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = ""
)

data class ComentarioEvento(
    val id: String = "",
    val eventoId: String = "",
    val usuarioId: String = "",
    val nombreUsuario: String = "",
    val comentario: String = ""
)

@Composable
fun AppEventos(auth: FirebaseAuth) {
    val db = FirebaseFirestore.getInstance()

    var pantallaActual by remember { mutableStateOf("login") }
    var rolUsuario by remember { mutableStateOf("usuario")}

    var eventos by remember {
        mutableStateOf<List<Evento>>(emptyList())
    }

    LaunchedEffect(Unit) {
        db.collection("eventos")
            .get()
            .addOnSuccessListener { documentos ->
                eventos = documentos.map { doc ->
                    Evento(
                        id = doc.id,
                        titulo = doc.getString("titulo") ?: "",
                        fecha = doc.getString("fecha") ?: "",
                        hora = doc.getString("hora") ?: "",
                        ubicacion = doc.getString("ubicacion") ?: "",
                        descripcion = doc.getString("descripcion") ?: ""
                    )
                }
            }
            .addOnFailureListener {
                eventos = emptyList()
            }
    }

    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) }

    when (pantallaActual) {

        "inscritos" -> {
            eventoSeleccionado?.let { evento ->
                PantallaInscritosEvento(
                    evento = evento,
                    onVolver = {
                        pantallaActual = "home"
                    }
                )
            }
        }

        "misEventos" -> PantallaMisEventos(
            auth = auth,
            onVolver = {
                pantallaActual = "home"
            }
        )

        "login" -> PantallaLogin(
            auth = auth,
            onLoginExitoso = {
                pantallaActual = "home"
            },
            onRolObtenido = { rol ->
                rolUsuario = rol
            }
        )

        "home" -> PantallaHomeEventos(
            eventos = eventos,
            rolUsuario = rolUsuario,
            auth = auth,
            onCrearEvento = {
                pantallaActual = "crear"
            },
            onEditarEvento = { evento ->
                eventoSeleccionado = evento
                pantallaActual = "editar"
            },
            onMisEventos = {
                pantallaActual = "misEventos"
            },
            onVerInscritos = { evento ->
                eventoSeleccionado = evento
                pantallaActual = "inscritos"
            },
            onCerrarSesion = {
                auth.signOut()
                rolUsuario = "usuario"
                pantallaActual = "login"
            }
        )

        "crear" -> PantallaCrearEvento(
            onGuardar = { nuevoEvento ->

                val datosEvento = hashMapOf(
                    "titulo" to nuevoEvento.titulo,
                    "fecha" to nuevoEvento.fecha,
                    "hora" to nuevoEvento.hora,
                    "ubicacion" to nuevoEvento.ubicacion,
                    "descripcion" to nuevoEvento.descripcion,
                    "estado" to "activo",
                    "organizadorId" to (auth.currentUser?.uid ?: "sin_usuario")
                )

                db.collection("eventos")
                    .add(datosEvento)
                    .addOnSuccessListener { documento ->
                        eventos = eventos + nuevoEvento.copy(id = documento.id)
                        pantallaActual = "home"
                    }
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

                        val datosActualizados = mapOf(
                            "titulo" to eventoActualizado.titulo,
                            "fecha" to eventoActualizado.fecha,
                            "hora" to eventoActualizado.hora,
                            "ubicacion" to eventoActualizado.ubicacion,
                            "descripcion" to eventoActualizado.descripcion
                        )

                        db.collection("eventos")
                            .document(eventoActualizado.id)
                            .update(datosActualizados)
                            .addOnSuccessListener {
                                eventos = eventos.map {
                                    if (it.id == eventoActualizado.id) eventoActualizado else it
                                }
                                pantallaActual = "home"
                            }
                    },
                    onEliminar = { id ->

                        db.collection("eventos")
                            .document(id)
                            .delete()
                            .addOnSuccessListener {
                                eventos = eventos.filter { it.id != id }
                                pantallaActual = "home"
                            }
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
    onLoginExitoso: () -> Unit,
    onRolObtenido: (String) -> Unit
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
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            val usuarioFirebase = auth.currentUser

                            if (usuarioFirebase != null) {
                                obtenerRolUsuario(
                                    uid = usuarioFirebase.uid,
                                    onResultado = { rol ->
                                        onRolObtenido(rol)

                                        Toast.makeText(
                                            context,
                                            "Inicio de sesión exitoso como $rol",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        onLoginExitoso()
                                    },
                                    onError = { mensaje ->
                                        Toast.makeText(
                                            context,
                                            mensaje,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }

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
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            val usuarioFirebase = auth.currentUser

                            if (usuarioFirebase != null) {
                                guardarUsuarioEnFirestore(
                                    uid = usuarioFirebase.uid,
                                    nombre = correo.substringBefore("@"),
                                    correo = correo,
                                    onExito = {
                                        Toast.makeText(
                                            context,
                                            "Usuario registrado correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val rolNuevo = if (correo.equals(ADMIN_EMAIL, ignoreCase = true)) {
                                            "admin"
                                        } else {
                                            "usuario"
                                        }

                                        onRolObtenido(rolNuevo)

                                        onLoginExitoso()
                                    },
                                    onError = { mensaje ->
                                        Toast.makeText(
                                            context,
                                            mensaje,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }

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

//Aqui almacenamos el rol de cada usuario, dependiendo del rol ese será el acceso a la app
fun obtenerRolUsuario(
    uid: String,
    onResultado: (String) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("usuarios")
        .document(uid)
        .get()
        .addOnSuccessListener { documento ->
            if (documento.exists()) {
                val rol = documento.getString("rol") ?: "usuario"
                onResultado(rol)
            } else {
                onResultado("usuario")
            }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al obtener rol")
        }
}

@Composable
fun PantallaHomeEventos(
    eventos: List<Evento>,
    rolUsuario: String,
    auth: FirebaseAuth,
    onCrearEvento: () -> Unit,
    onEditarEvento: (Evento) -> Unit,
    onMisEventos: () -> Unit,
    onVerInscritos: (Evento) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current

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

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        if (rolUsuario == "usuario") {
            Button(
                onClick = onMisEventos,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mis eventos")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (rolUsuario == "admin") {
            Button(
                onClick = onCrearEvento,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Evento")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        eventos.forEach { evento ->
            var comentarioTexto by remember { mutableStateOf("") }
            var comentariosEvento by remember { mutableStateOf<List<ComentarioEvento>>(emptyList()) }

            LaunchedEffect(evento.id) {
                obtenerComentariosEvento(
                    eventoId = evento.id,
                    onResultado = { lista ->
                        comentariosEvento = lista
                    },
                    onError = { mensaje ->
                        Toast.makeText(
                            context,
                            mensaje,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
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

                    if (rolUsuario == "admin") {
                        Button(
                            onClick = { onEditarEvento(evento) }
                        ) {
                            Text("Editar / Eliminar")
                        }
                    }

                    if (rolUsuario == "admin") {
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onVerInscritos(evento)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver inscritos")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            compartirEvento(context, evento)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Compartir evento")
                    }

                    if (rolUsuario == "usuario") {
                        Button(
                            onClick = {
                                val usuarioId = auth.currentUser?.uid

                                if (usuarioId != null) {
                                    confirmarAsistenciaEvento(
                                        eventoId = evento.id,
                                        usuarioId = usuarioId,
                                        onExito = {
                                            Toast.makeText(
                                                context,
                                                "Asistencia confirmada",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onYaConfirmado = {
                                            Toast.makeText(
                                                context,
                                                "Ya confirmaste asistencia a este evento",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = { mensaje ->
                                            Toast.makeText(
                                                context,
                                                mensaje,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Confirmar asistencia")
                        }
                    }

                    if (rolUsuario == "usuario") {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = comentarioTexto,
                            onValueChange = { comentarioTexto = it },
                            label = { Text("Escribe un comentario") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val usuarioId = auth.currentUser?.uid

                                if (usuarioId == null) {
                                    Toast.makeText(
                                        context,
                                        "No hay usuario autenticado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (comentarioTexto.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Escribe un comentario",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    guardarComentarioEvento(
                                        eventoId = evento.id,
                                        usuarioId = usuarioId,
                                        comentario = comentarioTexto,
                                        onExito = {
                                            Toast.makeText(
                                                context,
                                                "Comentario guardado",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            obtenerComentariosEvento(
                                                eventoId = evento.id,
                                                onResultado = { lista ->
                                                    comentariosEvento = lista
                                                },
                                                onError = { mensaje ->
                                                    Toast.makeText(
                                                        context,
                                                        mensaje,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )

                                            comentarioTexto = ""
                                        },
                                        onError = { mensaje ->
                                            Toast.makeText(
                                                context,
                                                mensaje,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enviar comentario")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Comentarios",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (comentariosEvento.isEmpty()) {
                        Text("No hay comentarios todavía.")
                    } else {
                        comentariosEvento.forEach { comentario ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = comentario.nombreUsuario,
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = comentario.comentario,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
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
                        id = "",
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
    onEliminar: (String) -> Unit,
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

fun confirmarAsistenciaEvento(
    eventoId: String,
    usuarioId: String,
    onExito: () -> Unit,
    onYaConfirmado: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("asistencias")
        .whereEqualTo("eventoId", eventoId)
        .whereEqualTo("usuarioId", usuarioId)
        .get()
        .addOnSuccessListener { documentos ->

            if (!documentos.isEmpty) {
                onYaConfirmado()
            } else {
                val asistencia = hashMapOf(
                    "eventoId" to eventoId,
                    "usuarioId" to usuarioId,
                    "estado" to "confirmado",
                    "fechaConfirmacion" to Timestamp.now()
                )

                db.collection("asistencias")
                    .add(asistencia)
                    .addOnSuccessListener {
                        onExito()
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al confirmar asistencia")
                    }
            }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al verificar asistencia")
        }
}

fun obtenerMisEventosUsuario(
    usuarioId: String,
    onResultado: (List<Evento>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("asistencias")
        .whereEqualTo("usuarioId", usuarioId)
        .whereEqualTo("estado", "confirmado")
        .get()
        .addOnSuccessListener { asistencias ->

            if (asistencias.isEmpty) {
                onResultado(emptyList())
                return@addOnSuccessListener
            }

            val eventosConfirmados = mutableListOf<Evento>()
            var pendientes = asistencias.size()

            for (asistencia in asistencias) {
                val eventoId = asistencia.getString("eventoId") ?: ""

                if (eventoId.isBlank()) {
                    pendientes--
                    if (pendientes == 0) {
                        onResultado(eventosConfirmados)
                    }
                    continue
                }

                db.collection("eventos")
                    .document(eventoId)
                    .get()
                    .addOnSuccessListener { doc ->

                        if (doc.exists()) {
                            val evento = Evento(
                                id = doc.id,
                                titulo = doc.getString("titulo") ?: "",
                                fecha = doc.getString("fecha") ?: "",
                                hora = doc.getString("hora") ?: "",
                                ubicacion = doc.getString("ubicacion") ?: "",
                                descripcion = doc.getString("descripcion") ?: ""
                            )

                            eventosConfirmados.add(evento)
                        }

                        pendientes--

                        if (pendientes == 0) {
                            onResultado(eventosConfirmados)
                        }
                    }
                    .addOnFailureListener { error ->
                        pendientes--

                        if (pendientes == 0) {
                            onResultado(eventosConfirmados)
                        }
                    }
            }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al obtener tus eventos")
        }
}

@Composable
fun PantallaMisEventos(
    auth: FirebaseAuth,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    var eventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    val hoy = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    LaunchedEffect(Unit) {
        val usuarioId = auth.currentUser?.uid

        if (usuarioId != null) {
            obtenerMisEventosUsuario(
                usuarioId = usuarioId,
                onResultado = { lista ->
                    eventos = lista
                    cargando = false
                },
                onError = { mensaje ->
                    cargando = false
                    Toast.makeText(
                        context,
                        mensaje,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            cargando = false
            Toast.makeText(
                context,
                "No hay usuario autenticado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val eventosProximos = eventos
        .filter { it.fecha >= hoy }
        .sortedBy { it.fecha }

    val eventosAnteriores = eventos
        .filter { it.fecha < hoy }
        .sortedByDescending { it.fecha }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Mis eventos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (cargando) {
            Text("Cargando tus eventos...")
        } else {
            Text(
                text = "Próximos",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (eventosProximos.isEmpty()) {
                Text("No tienes eventos próximos confirmados.")
            } else {
                eventosProximos.forEach { evento ->
                    CardEventoMisEventos(evento)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Anteriores",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (eventosAnteriores.isEmpty()) {
                Text("No tienes eventos anteriores.")
            } else {
                eventosAnteriores.forEach { evento ->
                    CardEventoMisEventos(evento)
                }
            }
        }
    }
}

@Composable
fun CardEventoMisEventos(evento: Evento) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = evento.titulo,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Fecha: ${evento.fecha}")
            Text("Hora: ${evento.hora}")
            Text("Ubicación: ${evento.ubicacion}")
            Text("Descripción: ${evento.descripcion}")
        }
    }
}

fun obtenerInscritosEvento(
    eventoId: String,
    onResultado: (List<UsuarioInscrito>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("asistencias")
        .whereEqualTo("eventoId", eventoId)
        .whereEqualTo("estado", "confirmado")
        .get()
        .addOnSuccessListener { asistencias ->

            if (asistencias.isEmpty) {
                onResultado(emptyList())
                return@addOnSuccessListener
            }

            val inscritos = mutableListOf<UsuarioInscrito>()
            var pendientes = asistencias.size()

            for (asistencia in asistencias) {
                val usuarioId = asistencia.getString("usuarioId") ?: ""

                if (usuarioId.isBlank()) {
                    pendientes--
                    if (pendientes == 0) {
                        onResultado(inscritos)
                    }
                    continue
                }

                db.collection("usuarios")
                    .document(usuarioId)
                    .get()
                    .addOnSuccessListener { usuarioDoc ->

                        if (usuarioDoc.exists()) {
                            val usuario = UsuarioInscrito(
                                uid = usuarioDoc.getString("uid") ?: usuarioId,
                                nombre = usuarioDoc.getString("nombre") ?: "Sin nombre",
                                correo = usuarioDoc.getString("correo") ?: "Sin correo"
                            )

                            inscritos.add(usuario)
                        }

                        pendientes--

                        if (pendientes == 0) {
                            onResultado(inscritos)
                        }
                    }
                    .addOnFailureListener {
                        pendientes--

                        if (pendientes == 0) {
                            onResultado(inscritos)
                        }
                    }
            }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al obtener inscritos")
        }
}

@Composable
fun PantallaInscritosEvento(
    evento: Evento,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    var inscritos by remember { mutableStateOf<List<UsuarioInscrito>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(evento.id) {
        obtenerInscritosEvento(
            eventoId = evento.id,
            onResultado = { lista ->
                inscritos = lista
                cargando = false
            },
            onError = { mensaje ->
                cargando = false
                Toast.makeText(
                    context,
                    mensaje,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 45.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Inscritos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = evento.titulo,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (cargando) {
            Text("Cargando inscritos...")
        } else {
            Text(
                text = "Total inscritos: ${inscritos.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (inscritos.isEmpty()) {
                Text("No hay usuarios inscritos en este evento.")
            } else {
                inscritos.forEach { usuario ->
                    CardUsuarioInscrito(usuario)
                }
            }
        }
    }
}

@Composable
fun CardUsuarioInscrito(usuario: UsuarioInscrito) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("Correo: ${usuario.correo}")
        }
    }
}

fun compartirEvento(
    context: android.content.Context,
    evento: Evento
) {
    val textoCompartir = """
        Te invito a este evento en mi comunidad:

        ${evento.titulo}

        Fecha: ${evento.fecha}
        Hora: ${evento.hora}
        Ubicación: ${evento.ubicacion}

        Descripción:
        ${evento.descripcion}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Evento comunitario: ${evento.titulo}")
        putExtra(Intent.EXTRA_TEXT, textoCompartir)
    }

    val chooser = Intent.createChooser(intent, "Compartir evento")
    context.startActivity(chooser)
}

fun guardarComentarioEvento(
    eventoId: String,
    usuarioId: String,
    comentario: String,
    onExito: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val datosComentario = hashMapOf(
        "eventoId" to eventoId,
        "usuarioId" to usuarioId,
        "comentario" to comentario,
        "creadoEn" to Timestamp.now()
    )

    db.collection("comentarios")
        .add(datosComentario)
        .addOnSuccessListener {
            onExito()
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al guardar comentario")
        }
}

fun obtenerComentariosEvento(
    eventoId: String,
    onResultado: (List<ComentarioEvento>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("comentarios")
        .whereEqualTo("eventoId", eventoId)
        .get()
        .addOnSuccessListener { documentos ->

            if (documentos.isEmpty) {
                onResultado(emptyList())
                return@addOnSuccessListener
            }

            val comentarios = mutableListOf<ComentarioEvento>()
            var pendientes = documentos.size()

            for (doc in documentos) {
                val usuarioId = doc.getString("usuarioId") ?: ""
                val textoComentario = doc.getString("comentario") ?: ""

                if (usuarioId.isBlank()) {
                    comentarios.add(
                        ComentarioEvento(
                            id = doc.id,
                            eventoId = eventoId,
                            usuarioId = usuarioId,
                            nombreUsuario = "Usuario",
                            comentario = textoComentario
                        )
                    )

                    pendientes--

                    if (pendientes == 0) {
                        onResultado(comentarios)
                    }

                    continue
                }

                db.collection("usuarios")
                    .document(usuarioId)
                    .get()
                    .addOnSuccessListener { usuarioDoc ->

                        val nombre = usuarioDoc.getString("nombre") ?: "Usuario"

                        comentarios.add(
                            ComentarioEvento(
                                id = doc.id,
                                eventoId = eventoId,
                                usuarioId = usuarioId,
                                nombreUsuario = nombre,
                                comentario = textoComentario
                            )
                        )

                        pendientes--

                        if (pendientes == 0) {
                            onResultado(comentarios)
                        }
                    }
                    .addOnFailureListener {
                        comentarios.add(
                            ComentarioEvento(
                                id = doc.id,
                                eventoId = eventoId,
                                usuarioId = usuarioId,
                                nombreUsuario = "Usuario",
                                comentario = textoComentario
                            )
                        )

                        pendientes--

                        if (pendientes == 0) {
                            onResultado(comentarios)
                        }
                    }
            }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Error al obtener comentarios")
        }
}