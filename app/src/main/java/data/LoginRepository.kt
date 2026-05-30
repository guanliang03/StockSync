package data

import android.app.Application
import androidx.lifecycle.LiveData
class LoginRepository(private val loginDao: LoginDao) {

    // Validate login credentials
    suspend fun validateLogin(email: String, password: String): LoginTable? {
        return loginDao.getLoginDetailsByEmailAndPassword(email, password)
    }

    // Insert new login details into the database
    suspend fun insertLoginDetails(loginTable: LoginTable) {
        loginDao.insertDetails(loginTable)
    }
}




