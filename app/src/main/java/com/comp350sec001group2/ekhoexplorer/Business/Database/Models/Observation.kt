package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

data class Observation(
    val id: String,
    val userId: String,
    val commonName: String,
    val scientificName: String,
    val category: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val photoCount: Int,
    val visibility: String,
    val createdAt: Long? = null
)
