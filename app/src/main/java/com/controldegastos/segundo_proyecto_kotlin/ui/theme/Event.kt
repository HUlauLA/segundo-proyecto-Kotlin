package com.controldegastos.segundo_proyecto_kotlin

import java.util.Date

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val date: Date,
    val location: String,
    val imageUrl: String? = null
) {
    val isUpcoming: Boolean
        get() = date.after(Date()) // Compara con la fecha y hora actual
}