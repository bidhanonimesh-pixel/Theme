package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_customizations")
data class AppCustomizationEntity(
    @PrimaryKey val packageName: String,
    val customName: String? = null,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val isFavorite: Boolean = false,
    val category: String = "General",
    val clickCount: Int = 0,
    val orbitSlotIndex: Int = -1
)

@Entity(tableName = "quick_contacts")
data class QuickContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarInitials: String,
    val isFavorite: Boolean = true,
    val accentHex: String = "#00E5FF"
)

@Entity(tableName = "jarvis_logs")
data class JarvisLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val response: String,
    val actionType: String
)

@Dao
interface LauncherDao {
    @Query("SELECT * FROM app_customizations")
    fun getAllCustomizationsFlow(): Flow<List<AppCustomizationEntity>>

    @Query("SELECT * FROM app_customizations WHERE packageName = :pkg LIMIT 1")
    suspend fun getCustomization(pkg: String): AppCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomization(item: AppCustomizationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllCustomizations(items: List<AppCustomizationEntity>)

    @Query("UPDATE app_customizations SET clickCount = clickCount + 1 WHERE packageName = :pkg")
    suspend fun incrementClickCount(pkg: String)

    @Query("SELECT * FROM quick_contacts")
    fun getAllQuickContactsFlow(): Flow<List<QuickContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: QuickContactEntity)

    @Query("DELETE FROM quick_contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Insert
    suspend fun insertLog(log: JarvisLogEntity)

    @Query("SELECT * FROM jarvis_logs ORDER BY timestamp DESC LIMIT 30")
    fun getRecentLogsFlow(): Flow<List<JarvisLogEntity>>
}

@Database(
    entities = [
        AppCustomizationEntity::class,
        QuickContactEntity::class,
        JarvisLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao
}
