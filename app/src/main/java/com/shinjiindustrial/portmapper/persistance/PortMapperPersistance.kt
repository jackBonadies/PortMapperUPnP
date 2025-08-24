package java.com.shinjiindustrial.portmapper.persistance

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Entity(tableName = "port_mappings", primaryKeys = ["deviceIp", "deviceSignature", "protocol", "externalPort"])
data class PortMappingEntity(

    // uniquely identifies a rule
    @ColumnInfo(name = "deviceIp") val deviceIp: String,
    @ColumnInfo(name = "deviceSignature") val deviceSignature: String,
    @ColumnInfo(name = "externalPort") val externalPort: Int,
    @ColumnInfo(name = "protocol") val protocol: String,

    // using these we can tell if it was likely created by us, or done out of band
    //   and if so, then remove it bc it was overwritten out of band by the user
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "internalIp") val internalIp: String,
    @ColumnInfo(name = "internalPort") val internalPort: Int,

    // these are the preferences that we cannot store on the device
    @ColumnInfo(name = "autorenew") val autoRenew: Boolean,
    @ColumnInfo(name = "desiredLeaseDuration") val desiredLeaseDuration: Int,

    // this is a preference we can sometimes store on the device
    @ColumnInfo(name = "desiredEnabled") val desiredEnabled: Boolean,
)

@Dao
interface PortMappingDao {
    @Query("SELECT * FROM port_mappings")
    fun getAll(): List<PortMappingEntity>

    @Query("""
        SELECT * FROM port_mappings
        WHERE deviceIp = :deviceIp
          AND deviceSignature = :deviceSignature
          AND protocol = :protocol
          AND externalPort = :externalPort
        LIMIT 1
    """)
    suspend fun getByPrimaryKey(
        deviceIp: String,
        deviceSignature: String,
        protocol: String,
        externalPort: Int
    ): PortMappingEntity?

    @Upsert
    suspend fun upsert(entity: PortMappingEntity)

    @Query("""
          DELETE FROM port_mappings
          WHERE deviceIp = :deviceIp
            AND deviceSignature = :deviceSignature
            AND externalPort = :externalPort
            AND protocol = :protocol
        """)
    suspend fun deleteByKey(deviceIp: String, deviceSignature: String, protocol: String, externalPort: Int): Int
}

@Database(entities = [PortMappingEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portMappingDao(): PortMappingDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            // .addMigrations(MIGRATION_1_2, ...)
            .build()

    @Provides
    fun providePortMappingDao(db: AppDatabase): PortMappingDao = db.portMappingDao()
}