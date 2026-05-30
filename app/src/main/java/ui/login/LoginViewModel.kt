package ui.login

import android.app.Application


import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import data.LoginRepository
import data.LoginTable

import kotlinx.coroutines.launch


/**
 * ViewModel to handle login functionality and validate input credentials.
 */
class LoginViewModel(application: Application, private val loginRepository: LoginRepository) : AndroidViewModel(application) {

    val loginSuccess = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>(false)
    val loginLocked = MutableLiveData<Boolean>(false)

    private var failedAttempts = 0
    private var lockoutExpiresAt = 0L
    private val lockoutThreshold = 3
    private val lockoutDurationMs = 30_000L

    // Function to reset the state when navigating back to the login screen
    fun resetLoginState() {
        loginSuccess.value = false
        errorMessage.value = null
        isLoading.value = false
        loginLocked.value = false
    }

    private fun isLocked(): Boolean {
        val now = System.currentTimeMillis()
        if (now < lockoutExpiresAt) {
            loginLocked.postValue(true)
            return true
        }
        if (lockoutExpiresAt > 0L) {
            lockoutExpiresAt = 0L
            failedAttempts = 0
            loginLocked.postValue(false)
        }
        return false
    }

    private fun lockoutMessage(): String {
        val remainingSeconds = ((lockoutExpiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
        return "Too many failed attempts. Try again in $remainingSeconds seconds."
    }

    fun validateLogin(email: String, password: String) {
        if (isLocked()) {
            errorMessage.postValue(lockoutMessage())
            return
        }

        isLoading.value = true  // Start loading

        viewModelScope.launch {
            val login = loginRepository.validateLogin(email, password)
            isLoading.value = false  // Reset loading state after processing

            if (login != null) {
                failedAttempts = 0
                loginLocked.postValue(false)
                loginSuccess.postValue(true)  // Set login success
            } else {
                failedAttempts++
                if (failedAttempts >= lockoutThreshold) {
                    lockoutExpiresAt = System.currentTimeMillis() + lockoutDurationMs
                    loginLocked.postValue(true)
                    errorMessage.postValue(lockoutMessage())
                } else {
                    loginLocked.postValue(false)
                    val remainingAttempts = lockoutThreshold - failedAttempts
                    errorMessage.postValue("Invalid Username or Password. $remainingAttempts tries left.")
                }
            }
        }
    }

    fun registerLoginDetails(email: String, password: String) {
        viewModelScope.launch {
            val login = LoginTable(email = email, password = password)
            loginRepository.insertLoginDetails(login)
        }
    }
}
