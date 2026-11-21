package bg.uni_plovdiv.fmi.everquill.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import bg.uni_plovdiv.fmi.everquill.data.database.dao.AchievementDao
import bg.uni_plovdiv.fmi.everquill.data.database.dao.BookDao
import bg.uni_plovdiv.fmi.everquill.data.database.dao.QuestDao
import bg.uni_plovdiv.fmi.everquill.data.database.dao.ReadingSessionDao
import bg.uni_plovdiv.fmi.everquill.data.database.dao.UserProfileDao
import bg.uni_plovdiv.fmi.everquill.data.database.entity.AchievementEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.BookEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.QuestEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.ReadingSessionEntity
import bg.uni_plovdiv.fmi.everquill.data.database.entity.UserProfileEntity
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BookEntity::class,
        ReadingSessionEntity::class,
        QuestEntity::class,
        AchievementEntity::class,
        UserProfileEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun questDao(): QuestDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN coverUrl TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN publisher TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN genre TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN publishYear INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN dailyGoal INTEGER NOT NULL DEFAULT 20")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quests ADD COLUMN rewardClaimed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE quests SET rewardClaimed = 1 WHERE isCompleted = 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE achievements ADD COLUMN category TEXT NOT NULL DEFAULT 'READING_MILESTONES'")
                db.execSQL("ALTER TABLE achievements ADD COLUMN rarity TEXT NOT NULL DEFAULT 'COMMON'")
                db.execSQL("ALTER TABLE achievements ADD COLUMN xpReward INTEGER NOT NULL DEFAULT 25")
                db.execSQL("ALTER TABLE achievements ADD COLUMN dateUnlocked INTEGER")
                db.execSQL("ALTER TABLE achievements ADD COLUMN rewardClaimed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE achievements SET rewardClaimed = 1 WHERE isUnlocked = 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quests ADD COLUMN lastCompletedDateEpoch INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE quests ADD COLUMN lastRewardedDateEpoch INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE quests SET isCompleted = 0, rewardClaimed = 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "everleaf_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { populateInitialData(it) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val bookDao = database.bookDao()
            val questDao = database.questDao()
            val achievementDao = database.achievementDao()
            val userProfileDao = database.userProfileDao()

            userProfileDao.insertUserProfile(
                UserProfileEntity(
                    id = 1,
                    name = "Reader",
                    level = 1,
                    currentXp = 40,
                    maxXpForNextLevel = 100,
                    pagesReadToday = 15,
                    streakDays = 3,
                    lastReadingDate = System.currentTimeMillis(),
                    dailyGoal = 20
                )
            )

            bookDao.insertBook(
                BookEntity(
                    id = "1",
                    title = "The Fellowship of the Ring",
                    author = "J.R.R. Tolkien",
                    totalPages = 423,
                    currentPage = 120,
                    status = BookStatus.READING
                )
            )
            bookDao.insertBook(
                BookEntity(
                    id = "2",
                    title = "Dune",
                    author = "Frank Herbert",
                    totalPages = 412,
                    currentPage = 0,
                    status = BookStatus.TO_READ
                )
            )

            questDao.insertOrUpdateQuest(
                QuestEntity(
                    id = "q1",
                    title = "Read 20 pages today",
                    description = "Read at least 20 pages in your active books",
                    targetPages = 20,
                    currentProgress = 0,
                    xpReward = 50,
                    date = System.currentTimeMillis(),
                    isCompleted = false,
                    rewardClaimed = false,
                    lastCompletedDateEpoch = 0L,
                    lastRewardedDateEpoch = 0L
                )
            )
            questDao.insertOrUpdateQuest(
                QuestEntity(
                    id = "q2",
                    title = "Open your library journal",
                    description = "Explore your library collection",
                    targetPages = 1,
                    currentProgress = 0,
                    xpReward = 20,
                    date = System.currentTimeMillis(),
                    isCompleted = false,
                    rewardClaimed = false,
                    lastCompletedDateEpoch = 0L,
                    lastRewardedDateEpoch = 0L
                )
            )

            achievementDao.insertAll(
                listOf(
                    AchievementEntity("a1", "First Steps", "Complete your first book", "READING_MILESTONES", "COMMON", 25, 1, 0, false, null, false),
                    AchievementEntity("a2", "Bookworm", "Read 100 total pages", "READING_MILESTONES", "RARE", 50, 100, 0, false, null, false),
                    AchievementEntity("a13", "Page Turner", "Read 250 total pages", "READING_MILESTONES", "RARE", 50, 250, 0, false, null, false),
                    AchievementEntity("a5", "Bibliophile", "Read 1,000 total pages", "READING_MILESTONES", "LEGENDARY", 250, 1000, 0, false, null, false),
                    AchievementEntity("a3", "Getting Started", "Complete your first reading session", "READING_HABITS", "COMMON", 25, 1, 0, false, null, false),
                    AchievementEntity("a6", "Marathon Reader", "Read at least 50 pages in a single reading session", "READING_HABITS", "RARE", 50, 50, 0, false, null, false),
                    AchievementEntity("a7", "Night Owl", "Complete a reading session starting at or after 22:00", "READING_HABITS", "RARE", 50, 1, 0, false, null, false),
                    AchievementEntity("a8", "Consistent Reader", "Maintain a 3-day reading streak", "DEDICATION", "COMMON", 25, 3, 0, false, null, false),
                    AchievementEntity("a4", "Dedicated Reader", "Maintain a 7-day reading streak", "DEDICATION", "EPIC", 100, 7, 0, false, null, false),
                    AchievementEntity("a9", "Committed Scholar", "Maintain a 14-day reading streak", "DEDICATION", "LEGENDARY", 250, 14, 0, false, null, false),
                    AchievementEntity("a10", "Curator", "Add 5 books to the library", "COLLECTION_EXPLORATION", "RARE", 50, 5, 0, false, null, false),
                    AchievementEntity("a11", "Genre Explorer", "Read books from at least 3 distinct genres", "COLLECTION_EXPLORATION", "EPIC", 100, 3, 0, false, null, false),
                    AchievementEntity("a12", "Collector", "Add 20 books to the library", "COLLECTION_EXPLORATION", "LEGENDARY", 250, 20, 0, false, null, false)
                )
            )
        }
    }
}
