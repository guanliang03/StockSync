package data

import java.security.MessageDigest
import java.util.Locale

class LoginRepository(private val loginDao: LoginDao) {

    private companion object {
        private const val HASH_ALGORITHM = "SHA-256"
        private const val HASH_HEX_FORMAT = "%02x"
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        digest.update(salt.lowercase(Locale.getDefault()).toByteArray(Charsets.UTF_8))
        val hashedBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashedBytes.joinToString(separator = "") { HASH_HEX_FORMAT.format(it) }
    }

    private fun isPasswordValid(plainPassword: String, storedPassword: String, email: String): Boolean {
        if (storedPassword.isBlank()) return false
        if (storedPassword == plainPassword) return true
        return storedPassword == hashPassword(plainPassword, email)
    }

    suspend fun validateLogin(email: String, password: String): LoginTable? {
        val login = loginDao.getLoginDetailsByEmail(email)
        return if (login != null && isPasswordValid(password, login.password, email)) {
            login
        } else {
            null
        }
    }

    suspend fun insertLoginDetails(loginTable: LoginTable) {
        val securePassword = hashPassword(loginTable.password, loginTable.email)
        loginDao.insertDetails(loginTable.copy(password = securePassword))
    }
}




