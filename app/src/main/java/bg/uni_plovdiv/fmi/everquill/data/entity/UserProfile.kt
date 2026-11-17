package bg.uni_plovdiv.fmi.everquill.data.entity

data class UserProfile(
    val name: String = "Adventurer Reader",
    val level: Int = 1,
    val currentXp: Int = 0,
    val maxXpForNextLevel: Int = 100,
    val pagesReadToday: Int = 0,
    val streakDays: Int = 3,
    val dailyGoal: Int = 20
) {
    val xpProgressPercent: Float
        get() = if (maxXpForNextLevel > 0) (currentXp.toFloat() / maxXpForNextLevel.toFloat()).coerceIn(0f, 1f) else 0f
}
