package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.checkout.presentation.model.UserInfo
import com.mtdevelopment.checkout.presentation.state.DeliveryUiDataState
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class DeliveryViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase,
) : ViewModel(), KoinComponent {

    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    var deliveryUiDataState by mutableStateOf(DeliveryUiDataState())
        private set

    private val _selectedPath = MutableStateFlow<DeliveryPath?>(null)
    val selectedPath: StateFlow<DeliveryPath?> = _selectedPath.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    fun setSelectedPath(path: DeliveryPath) {
        _selectedPath.value = path
    }

    fun setUserInfo(userInfo: UserInfo) {
        _userInfo.value = userInfo
        saveUserInfo()
    }

    // TODO: Manage error state
    fun saveUserInfo(onError: () -> Unit = {}) {
        viewModelScope.launch {
            if (selectedPath.value == null || userInfo.value == null) {
                onError.invoke()
                return@launch
            }
            saveToDatastoreUseCase.invoke(
                userInformation = UserInformation(
                    name = userInfo.value!!.userName,
                    address = userInfo.value!!.userAddress,
                    lastSelectedPath = selectedPath.value!!
                )
            )
        }
    }

    fun saveSelectedDate(date: Long) {
        viewModelScope.launch {
            saveToDatastoreUseCase.invoke(deliveryDate = date)
        }
    }

    fun setIsDatePickerClickable(isClickable: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(shouldDatePickerBeClickable = isClickable)
    }

    fun setIsDatePickerShown(isShown: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(datePickerVisibility = isShown)
    }

    fun setDateFieldText(text: String) {
        deliveryUiDataState = deliveryUiDataState.copy(dateFieldText = text)
    }

    fun setUserNameFieldText(name: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userNameFieldText = name)
    }

    fun setUserAddressFieldText(address: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userAddressFieldText = name)
    }
}