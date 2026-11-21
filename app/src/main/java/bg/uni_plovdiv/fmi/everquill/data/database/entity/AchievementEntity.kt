package bg.uni_plovdiv.fmi.everquill.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import bg.uni_plovdiv.fmi.everquill.data.entity.Achievement

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rarity: String,
    val xpReward: Int,
    val requiredProgress: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val dateUnlocked: Long?,
    val rewardClaimed: Boolean
) {
    fun toDomain(): Achievement {
        val req = if (requiredProgress > 0) requiredProgress else 1
        val prog = (currentProgress.toFloat() / req.toFloat()).coerceIn(0f, 1f)
        val progText = if (isUnlocked) "Unlocked" else "$currentProgress / $requiredProgress"
        return Achievement(
            id = id,
            title = title,
            description = description,
            category = category,
            rarity = rarity,
            xpReward = xpReward,
            requiredProgress = requiredProgress,
            currentProgress = currentProgress,
            isUnlocked = isUnlocked,
            dateUnlocked = dateUnlocked,
            rewardClaimed = rewardClaimed,
            progress = prog,
            progressText = progText
        )
    }

    companion object {
        fun fromDomain(ach: Achievement) = AchievementEntity(
            id = ach.id,
            title = ach.title,
            description = ach.description,
            category = ach.category,
            rarity = ach.rarity,
            xpReward = ach.xpReward,
            requiredProgress = ach.requiredProgress,
            currentProgress = ach.currentProgress,
            isUnlocked = ach.isUnlocked,
            dateUnlocked = ach.dateUnlocked,
            rewardClaimed = ach.rewardClaimed
        )
    }
}
