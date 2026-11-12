package com.saged.githubvault.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="local_files")
data class LocalFile(
 @PrimaryKey val id:String, val repositoryId:String, val relativePath:String,
 val localPath:String, val sha:String, val downloadedAt:Long,
 val removedRemotely:Boolean=false, val updateAvailable:Boolean=false
)
@Entity(tableName="transfers")
data class Transfer(
 @PrimaryKey val id:String, val kind:String, val repositoryId:String, val label:String,
 val payload:String, val status:String="QUEUED", val progress:Float=0f,
 val error:String?=null, val createdAt:Long=System.currentTimeMillis()
)
@Dao
interface LocalFileDao {
 @Query("SELECT * FROM local_files ORDER BY relativePath") fun observe():Flow<List<LocalFile>>
 @Query("SELECT * FROM local_files") suspend fun all():List<LocalFile>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(file:LocalFile)
 @Query("UPDATE local_files SET removedRemotely=:removed,updateAvailable=:available WHERE id=:id AND sha=:expectedSha") suspend fun flags(id:String,expectedSha:String,removed:Boolean,available:Boolean)
 @Query("DELETE FROM local_files WHERE id=:id") suspend fun delete(id:String)
}
@Dao
interface TransferDao {
 @Query("SELECT * FROM transfers ORDER BY createdAt DESC") fun observe():Flow<List<Transfer>>
 @Query("SELECT * FROM transfers WHERE status IN ('QUEUED','RUNNING','RETRYING')") suspend fun pending():List<Transfer>
 @Query("SELECT * FROM transfers WHERE id=:id") suspend fun get(id:String):Transfer?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(transfer:Transfer)
 @Query("UPDATE transfers SET status=:status,progress=:progress,error=:error WHERE id=:id") suspend fun update(id:String,status:String,progress:Float,error:String?=null)
}
@Database(entities=[LocalFile::class,Transfer::class],version=1,exportSchema=false)
abstract class VaultDatabase:RoomDatabase() { abstract fun files():LocalFileDao; abstract fun transfers():TransferDao }
