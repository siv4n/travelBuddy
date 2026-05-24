package com.example.travel_buddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travel_buddy.data.model.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val postId: String,
    val authorId: String,
    val title: String,
    val location: String,
    val description: String,
    val imageUrl: String,
    val imageUrlsString: String,
    val timestamp: Long,
    val likesCount: Int,
    val authorUsername: String,
    val authorImageUrl: String?,
    val isLiked: Boolean,
    val isSaved: Boolean
) {
    fun toPost(): Post {
        val urls = if (imageUrlsString.isEmpty()) emptyList() else imageUrlsString.split(",")
        return Post(
            postId = postId,
            authorId = authorId,
            title = title,
            location = location,
            description = description,
            imageUrl = imageUrl,
            imageUrls = urls,
            timestamp = timestamp,
            likesCount = likesCount,
            authorUsername = authorUsername,
            authorImageUrl = authorImageUrl,
            isLiked = isLiked,
            isSaved = isSaved
        )
    }

    companion object {
        fun fromPost(post: Post): PostEntity {
            return PostEntity(
                postId = post.postId,
                authorId = post.authorId,
                title = post.title,
                location = post.location,
                description = post.description,
                imageUrl = post.imageUrl,
                imageUrlsString = post.imageUrls.joinToString(","),
                timestamp = post.timestamp,
                likesCount = post.likesCount,
                authorUsername = post.authorUsername,
                authorImageUrl = post.authorImageUrl,
                isLiked = post.isLiked,
                isSaved = post.isSaved
            )
        }
    }
}
