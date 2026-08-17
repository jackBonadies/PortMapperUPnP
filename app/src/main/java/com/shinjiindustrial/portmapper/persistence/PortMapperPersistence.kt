package com.shinjiindustrial.portmapper.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// deviceSignature (i.e. UDN) is the closest we can semantically get to a unique device the user cares about -
//   it is unique per device (so a router at parents / friends / 2nd router will be distinct)
//   and so it makes sense a user would likely want these rules either only for this UDN
//   (i.e. my parents house I have X server, my friends house we play X game)
//   or global (i.e. no matter where I am I want to forward X to my mobile phone).
//   They do not care about remote IP which can both change for the same device OR
//   be the same for distinct devices (i.e. always 192.168.1.1) (and so broken both ways)

@Entity(
    tableName = "port_mappings",
    primaryKeys = ["deviceSignature", "protocol", "externalPort"]
)
data class PortMappingEntity(

    // uniquely identifies a rule
    @ColumnInfo(name = "deviceSignature") val deviceSignature: String,
    @ColumnInfo(name = "protocol") val protocol: String,
    @ColumnInfo(name = "externalPort") val externalPort: Int,

    // the device ip at the time the rule was last written
    @ColumnInfo(name = "deviceIp") val deviceIp: String,

    // using these we can tell if it was likely created by us, or done out of band
    //   and if so, then remove it bc it was overwritten out of band by the user
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "internalIp") val internalIp: String,
    @ColumnInfo(name = "internalPort") val internalPort: Int,

    // these are the preferences that we cannot store on the router
    @ColumnInfo(name = "autorenew") val autoRenew: Boolean,
    @ColumnInfo(name = "desiredLeaseDuration") val desiredLeaseDuration: Int,
    @ColumnInfo(name = "autorenewManualCadence", defaultValue = "-1") val autoRenewManualCadence: Int,

    // this is a preference we can sometimes store on the router
    @ColumnInfo(name = "desiredEnabled") val desiredEnabled: Boolean,

    // wall clock (System.currentTimeMillis), can be null (added on v4)
    @ColumnInfo(name = "createdAtUtcMs") val createdAtUtcMs: Long?,
    // last time we saw this rule still present on the router
    @ColumnInfo(name = "lastSeenAtUtcMs") val lastSeenAtUtcMs: Long?,
)

@Entity(
    tableName = "devices",
    primaryKeys = ["deviceSignature"]
)
data class DevicesEntity(

    // uniquely identifies a device
    @ColumnInfo(name = "deviceSignature") val deviceSignature: String,

    // these are the preferences that we cannot store on the router
    @ColumnInfo(name = "useWildcardForRemoteHostDelete") val useWildcardForRemoteHostDelete: Boolean,

    @ColumnInfo(name = "lastKnownIp") val lastKnownIp: String,

    // mirrors DeviceDetails.
    //   needed to show more info about devices that we may have created rules on
    //   which may not match the current device
    @ColumnInfo(name = "displayName") val displayName: String?,
    @ColumnInfo(name = "friendlyName") val friendlyName: String?,
    @ColumnInfo(name = "manufacturer") val manufacturer: String?,
    @ColumnInfo(name = "modelName") val modelName: String?,
    @ColumnInfo(name = "modelNumber") val modelNumber: String?,
    @ColumnInfo(name = "serialNumber") val serialNumber: String?,
    @ColumnInfo(name = "upc") val upc: String?,
    @ColumnInfo(name = "deviceType") val deviceType: String?,
    @ColumnInfo(name = "upnpVersion") val upnpVersion: Int?,
    @ColumnInfo(name = "udaVersion") val udaVersion: String?,

    @ColumnInfo(name = "lastSeenAtUtcMs") val lastSeenAtUtcMs: Long?,
)

@Dao
interface DevicesDao {
    @Query("SELECT * FROM devices")
    suspend fun getAll(): List<DevicesEntity>

    @Query(
        """
        SELECT * FROM devices
        WHERE deviceSignature = :deviceSignature
    """
    )
    suspend fun getByPrimaryKey(
        deviceSignature: String,
    ): DevicesEntity?

    @Upsert
    suspend fun upsert(entity: DevicesEntity)

    @Query(
        """
          DELETE FROM devices
          WHERE deviceSignature = :deviceSignature
        """
    )
    suspend fun deleteByKey(
        deviceSignature: String,
    ): Int
}

@Dao
interface PortMappingDao {
    @Query("SELECT * FROM port_mappings")
    suspend fun getAll(): List<PortMappingEntity>

    @Query(
        """
        SELECT * FROM port_mappings
        WHERE deviceSignature = :deviceSignature
    """
    )
    suspend fun getAllForDevice(deviceSignature: String): List<PortMappingEntity>

    @Query(
        """
        SELECT * FROM port_mappings
        WHERE deviceSignature = :deviceSignature
          AND protocol = :protocol
          AND externalPort = :externalPort
        LIMIT 1
    """
    )
    suspend fun getByPrimaryKey(
        deviceSignature: String,
        protocol: String,
        externalPort: Int
    ): PortMappingEntity?

    @Upsert
    suspend fun upsert(entity: PortMappingEntity)

    @Query(
        """
          UPDATE port_mappings
          SET lastSeenAtUtcMs = :seenAtUtcMs, deviceIp = :deviceIp
          WHERE deviceSignature = :deviceSignature
            AND protocol = :protocol
            AND externalPort = :externalPort
        """
    )
    suspend fun markSeen(
        deviceSignature: String,
        protocol: String,
        externalPort: Int,
        seenAtUtcMs: Long,
        deviceIp: String,
    ): Int

    @Query(
        """
          DELETE FROM port_mappings
          WHERE deviceSignature = :deviceSignature
            AND protocol = :protocol
            AND externalPort = :externalPort
        """
    )
    suspend fun deleteByKey(
        deviceSignature: String,
        protocol: String,
        externalPort: Int
    ): Int
}

// v3 -> v4 drops the IP from both primary keys, so the tables have to be recreated (Room
//   auto-migrations cannot change a primary key).  INSERT OR REPLACE collapses the duplicates
//   this creates: a router seen at two IPs previously produced two rows per rule, and those rows
//   now share a key.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `port_mappings_new` (
                `deviceSignature` TEXT NOT NULL,
                `protocol` TEXT NOT NULL,
                `externalPort` INTEGER NOT NULL,
                `deviceIp` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `internalIp` TEXT NOT NULL,
                `internalPort` INTEGER NOT NULL,
                `autorenew` INTEGER NOT NULL,
                `desiredLeaseDuration` INTEGER NOT NULL,
                `autorenewManualCadence` INTEGER NOT NULL DEFAULT -1,
                `desiredEnabled` INTEGER NOT NULL,
                `createdAtUtcMs` INTEGER,
                `lastSeenAtUtcMs` INTEGER,
                PRIMARY KEY(`deviceSignature`, `protocol`, `externalPort`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO `port_mappings_new` (
                `deviceSignature`, `protocol`, `externalPort`, `deviceIp`, `description`,
                `internalIp`, `internalPort`, `autorenew`, `desiredLeaseDuration`,
                `autorenewManualCadence`, `desiredEnabled`, `createdAtUtcMs`, `lastSeenAtUtcMs`
            )
            SELECT
                `deviceSignature`, `protocol`, `externalPort`, `deviceIp`, `description`,
                `internalIp`, `internalPort`, `autorenew`, `desiredLeaseDuration`,
                `autorenewManualCadence`, `desiredEnabled`, NULL, NULL
            FROM `port_mappings`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `port_mappings`")
        db.execSQL("ALTER TABLE `port_mappings_new` RENAME TO `port_mappings`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `devices_new` (
                `deviceSignature` TEXT NOT NULL,
                `useWildcardForRemoteHostDelete` INTEGER NOT NULL,
                `lastKnownIp` TEXT NOT NULL,
                `displayName` TEXT,
                `friendlyName` TEXT,
                `manufacturer` TEXT,
                `modelName` TEXT,
                `modelNumber` TEXT,
                `serialNumber` TEXT,
                `upc` TEXT,
                `deviceType` TEXT,
                `upnpVersion` INTEGER,
                `udaVersion` TEXT,
                `lastSeenAtUtcMs` INTEGER,
                PRIMARY KEY(`deviceSignature`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO `devices_new` (
                `deviceSignature`, `useWildcardForRemoteHostDelete`, `lastKnownIp`, `displayName`,
                `friendlyName`, `manufacturer`, `modelName`, `modelNumber`, `serialNumber`, `upc`,
                `deviceType`, `upnpVersion`, `udaVersion`, `lastSeenAtUtcMs`
            )
            SELECT
                `deviceSignature`, `useWildcardForRemoteHostDelete`, `deviceIp`, NULL,
                NULL, NULL, NULL, NULL, NULL, NULL,
                NULL, NULL, NULL, NULL
            FROM `devices`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `devices`")
        db.execSQL("ALTER TABLE `devices_new` RENAME TO `devices`")
    }
}

@Database(entities = [PortMappingEntity::class, DevicesEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)])
abstract class AppDatabase : RoomDatabase() {
    abstract fun portMappingDao(): PortMappingDao
    abstract fun devicesDao(): DevicesDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .addMigrations(MIGRATION_3_4)
            .build()

    @Provides
    fun providePortMappingDao(db: AppDatabase): PortMappingDao = db.portMappingDao()

    @Provides
    fun provideDevicesDao(db: AppDatabase): DevicesDao = db.devicesDao()
}
