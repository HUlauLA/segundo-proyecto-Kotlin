package com.controldegastos.segundo_proyecto_kotlin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.controlegastos.segundo_proyecto_kotlin.ui.theme.EventCard
import com.controldegastos.segundo_proyecto_kotlin.Event
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Lista de prueba (Mock Data)
    val sampleEvents = remember { getSampleEvents() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Próximos", "Pasados")

    // Filtrar los eventos según la pestaña seleccionada
    val filteredEvents = remember(selectedTab) {
        if (selectedTab == 0) {
            sampleEvents.filter { it.isUpcoming }
        } else {
            sampleEvents.filter { !it.isUpcoming }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Eventos") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pestañas de Navegación interna (Próximos / Pasados)
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Lista Vertical de Eventos
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(16.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "No hay eventos en esta categoría.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(filteredEvents) { event ->
                        EventCard(
                            event = event,
                            onEventClick = { eventId ->
                                // Navegación al detalle pasando el ID del evento
                                navController.navigate("event_detail/$eventId")
                            }
                        )
                    }
                }
            }
        }
    }
}

// Función auxiliar para generar datos de prueba
fun getSampleEvents(): List<Event> {
    val calendar = Calendar.getInstance()

    // Evento Próximo
    calendar.add(Calendar.DAY_OF_YEAR, 5)
    val futureDate = calendar.time

    // Evento Pasado
    calendar.add(Calendar.DAY_OF_YEAR, -10)
    val pastDate = calendar.time

    return listOf(
        Event("1", "Conferencia Tech de Kotlin", "Aprende las novedades de Kotlin.", futureDate, "Auditorio Principal"),
        Event("2", "Taller de Jetpack Compose", "Creación de interfaces modernas.", futureDate, "Laboratorio 3"),
        Event("3", "Hackathon 2025", "Competencia de desarrollo de software.", pastDate, "Gimnasio Universitario")
    )
}