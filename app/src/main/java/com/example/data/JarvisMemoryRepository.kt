package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Entity(tableName = "jarvis_config")
data class JarvisConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface JarvisConfigDao {
    @Query("SELECT value FROM jarvis_config WHERE `key` = :k LIMIT 1")
    suspend fun getConfigValue(k: String): String?

    @Query("SELECT value FROM jarvis_config WHERE `key` = :k LIMIT 1")
    fun getConfigValueFlow(k: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfigValue(entity: JarvisConfigEntity)
}

class JarvisMemoryRepository(context: Context, private val launcherDao: LauncherDao) {

    private val configDb = Room.databaseBuilder(
        context.applicationContext,
        JarvisConfigDatabase::class.java,
        "jarvis_config_db"
    ).fallbackToDestructiveMigration().build()

    private val configDao = configDb.configDao()

    suspend fun getGeminiApiKey(): String = withContext(Dispatchers.IO) {
        configDao.getConfigValue(KEY_GEMINI_API) ?: ""
    }

    suspend fun setGeminiApiKey(key: String) = withContext(Dispatchers.IO) {
        configDao.setConfigValue(JarvisConfigEntity(KEY_GEMINI_API, key.trim()))
    }

    suspend fun getGeminiModel(): String = withContext(Dispatchers.IO) {
        configDao.getConfigValue(KEY_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
    }

    suspend fun setGeminiModel(model: String) = withContext(Dispatchers.IO) {
        configDao.setConfigValue(JarvisConfigEntity(KEY_GEMINI_MODEL, model.trim()))
    }

    suspend fun getOpenRouterApiKey(): String = withContext(Dispatchers.IO) {
        configDao.getConfigValue(KEY_OPENROUTER_API) ?: ""
    }

    suspend fun setOpenRouterApiKey(key: String) = withContext(Dispatchers.IO) {
        configDao.setConfigValue(JarvisConfigEntity(KEY_OPENROUTER_API, key.trim()))
    }

    suspend fun getOpenRouterModel(): String = withContext(Dispatchers.IO) {
        configDao.getConfigValue(KEY_OPENROUTER_MODEL) ?: DEFAULT_OPENROUTER_MODEL
    }

    suspend fun setOpenRouterModel(model: String) = withContext(Dispatchers.IO) {
        configDao.setConfigValue(JarvisConfigEntity(KEY_OPENROUTER_MODEL, model.trim()))
    }

    suspend fun getWheelAppPackages(): List<String> = withContext(Dispatchers.IO) {
        val raw = configDao.getConfigValue(KEY_WHEEL_APPS) ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun setWheelAppPackages(packages: List<String>) = withContext(Dispatchers.IO) {
        val raw = packages.joinToString(",")
        configDao.setConfigValue(JarvisConfigEntity(KEY_WHEEL_APPS, raw))
    }

    suspend fun logAiInteraction(query: String, response: String, actionType: String) = withContext(Dispatchers.IO) {
        launcherDao.insertLog(
            JarvisLogEntity(
                query = query,
                response = response,
                actionType = actionType
            )
        )
    }

    fun getRecentLogs(): Flow<List<JarvisLogEntity>> = launcherDao.getRecentLogsFlow()

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_OPENROUTER_API = "openrouter_api_key"
        private const val KEY_OPENROUTER_MODEL = "openrouter_model"
        private const val KEY_WHEEL_APPS = "wheel_app_packages"
        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
        const val DEFAULT_OPENROUTER_MODEL = "deepseek/deepseek-chat"
    }
}

@Database(entities = [JarvisConfigEntity::class], version = 1, exportSchema = false)
abstract class JarvisConfigDatabase : RoomDatabase() {
    abstract fun configDao(): JarvisConfigDao
}
