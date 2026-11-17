package bg.uni_plovdiv.fmi.everquill.data.entity

data class Achievement(
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
    val rewardClaimed: Boolean,
    val progress: Float, // 0f to 1f
    val progressText: String
)
