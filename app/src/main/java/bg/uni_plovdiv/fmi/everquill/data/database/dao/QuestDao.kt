package bg.uni_plovdiv.fmi.everquill.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import bg.uni_plovdiv.fmi.everquill.data.database.entity.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateQuest(quest: QuestEntity)

    @Query("SELECT * FROM quests")
    fun getDailyQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getQuestById(id: String): QuestEntity?
}
