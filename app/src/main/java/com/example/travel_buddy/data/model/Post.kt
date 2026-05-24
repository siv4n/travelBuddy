package com.example.travel_buddy.data.model

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val authorUsername: String = "",
    val authorImageUrl: String? = null,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
)
