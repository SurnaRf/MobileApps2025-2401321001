package bg.uni_plovdiv.fmi.everquill.data.entity

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val progress: Float, // 0f to 1f
    val progressText: String
)
