package bg.uni_plovdiv.fmi.everquill.data.entity

data class Quest(
    val id: String,
    val title: String,
    val rewardXp: Int,
    val isCompleted: Boolean,
    val rewardClaimed: Boolean = false
)
