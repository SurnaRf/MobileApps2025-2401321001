package bg.uni_plovdiv.fmi.everquill.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import bg.uni_plovdiv.fmi.everquill.data.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insertSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId")
    fun getSessionsForBook(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY date DESC")
    suspend fun getAllSessionsSync(): List<ReadingSessionEntity>
}
