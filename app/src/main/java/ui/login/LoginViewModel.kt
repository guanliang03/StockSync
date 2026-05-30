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

    // Function to reset the state when navigating back to the login screen
    fun resetLoginState() {
        loginSuccess.value = false
        errorMessage.value = null
        isLoading.value = false
    }

    fun validateLogin(email: String, password: String) {
        isLoading.value = true  // Start loading

        viewModelScope.launch {
            val login = loginRepository.validateLogin(email, password)
            isLoading.value = false  // Reset loading state after processing

            if (login != null) {
                loginSuccess.postValue(true)  // Set login success
            } else {
                errorMessage.postValue("Invalid Matric Number or Password")  // Set error message
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
