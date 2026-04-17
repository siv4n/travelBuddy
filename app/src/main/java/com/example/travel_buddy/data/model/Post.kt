package com.example.travel_buddy.data.model

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L
)
