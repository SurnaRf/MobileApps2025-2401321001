package bg.uni_plovdiv.fmi.everquill.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import bg.uni_plovdiv.fmi.everquill.data.entity.Quest

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val targetPages: Int,
    val currentProgress: Int,
    val xpReward: Int,
    val date: Long,
    val isCompleted: Boolean,
    val rewardClaimed: Boolean = false,
    val lastCompletedDateEpoch: Long = 0L,
    val lastRewardedDateEpoch: Long = 0L
) {
    fun toDomain() = Quest(
        id = id,
        title = title,
        rewardXp = xpReward,
        isCompleted = isCompleted,
        rewardClaimed = rewardClaimed
    )

    companion object {
        fun fromDomain(quest: Quest) = QuestEntity(
            id = quest.id,
            title = quest.title,
            description = quest.title,
            targetPages = 20,
            currentProgress = if (quest.isCompleted) 20 else 0,
            xpReward = quest.rewardXp,
            date = System.currentTimeMillis(),
            isCompleted = quest.isCompleted,
            rewardClaimed = quest.rewardClaimed,
            lastCompletedDateEpoch = 0L,
            lastRewardedDateEpoch = 0L
        )
    }
}
