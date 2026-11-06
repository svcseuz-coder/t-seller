package uz.carlcare.salesassistant.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service_centers ORDER BY city, name")
    fun observeCenters(): Flow<List<ServiceCenterEntity>>

    @Query("SELECT * FROM support_info WHERE id = 1")
    fun observeSupport(): Flow<SupportInfoEntity?>

    @Query("SELECT COUNT(*) FROM service_centers")
    suspend fun countCenters(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenters(centers: List<ServiceCenterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupport(info: SupportInfoEntity)
}
