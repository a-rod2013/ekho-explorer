package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

data class TrailReview(
    val id: String,
    val trailId: String,
    val userId: String,
    val userName: String,
    val caption: String,
    val rating: Int,
    val photoCount: Int,
    val heartCount: Int = 0,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)
