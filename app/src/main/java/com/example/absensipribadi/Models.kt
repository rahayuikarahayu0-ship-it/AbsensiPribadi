package com.example.absensipribadi

import androidx.room.*

@Entity(tableName="attendance")
data class Attendance(
 @PrimaryKey(autoGenerate=true) val id:Long=0,
 val date:String, val type:String, val time:String, val timestamp:Long,
 val latitude:Double?, val longitude:Double?, val accuracy:Float?,
 val photoPath:String?, val note:String, val deleted:Boolean=false
)

@Dao interface AttendanceDao {
 @Query("SELECT * FROM attendance WHERE deleted=0 ORDER BY timestamp DESC") fun observe():kotlinx.coroutines.flow.Flow<List<Attendance>>
 @Query("SELECT * FROM attendance WHERE date=:date AND deleted=0 ORDER BY timestamp ASC") suspend fun day(date:String):List<Attendance>
 @Query("SELECT * FROM attendance ORDER BY timestamp ASC") suspend fun all():List<Attendance>
 @Insert suspend fun insert(a:Attendance):Long
 @Insert suspend fun insertAll(a:List<Attendance>)
 @Query("DELETE FROM attendance") suspend fun clear()
}
@Database(entities=[Attendance::class],version=1,exportSchema=false)
abstract class AppDb:RoomDatabase(){abstract fun dao():AttendanceDao}
