package bg.uni_plovdiv.fmi.everquill.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import bg.uni_plovdiv.fmi.everquill.data.entity.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val level: Int,
    val currentXp: Int,
    val maxXpForNextLevel: Int,
    val pagesReadToday: Int,
    val streakDays: Int,
    val lastReadingDate: Long,
    val dailyGoal: Int = 20
) {
    fun toDomain() = UserProfile(
        name = name,
        level = level,
        currentXp = currentXp,
        maxXpForNextLevel = maxXpForNextLevel,
        pagesReadToday = pagesReadToday,
        streakDays = streakDays,
        dailyGoal = dailyGoal
    )

    companion object {
        fun fromDomain(profile: UserProfile) = UserProfileEntity(
            id = 1,
            name = profile.name,
            level = profile.level,
            currentXp = profile.currentXp,
            maxXpForNextLevel = profile.maxXpForNextLevel,
            pagesReadToday = profile.pagesReadToday,
            streakDays = profile.streakDays,
            lastReadingDate = System.currentTimeMillis(),
            dailyGoal = profile.dailyGoal
        )
    }
}
