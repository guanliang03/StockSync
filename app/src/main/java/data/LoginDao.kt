package data


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LoginDao {

    @Insert
    suspend fun insertDetails(data: LoginTable)

    @Query("SELECT * FROM LoginDetails WHERE Email = :email AND Password = :password LIMIT 1")
    suspend fun getLoginDetailsByEmailAndPassword(email: String, password: String): LoginTable?
}

