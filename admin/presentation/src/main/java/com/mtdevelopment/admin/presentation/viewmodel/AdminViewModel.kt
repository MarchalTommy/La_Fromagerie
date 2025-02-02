package com.mtdevelopment.admin.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.mtdevelopment.admin.domain.usecase.AddNewPathUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePathUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.sharedModels.toDomainProduct
import org.koin.core.component.KoinComponent

class AdminViewModel(
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val addNewProductUseCase: AddNewProductUseCase,
    private val updateDeliveryPathUseCase: UpdateDeliveryPathUseCase,
    private val deleteDeliveryPathUseCase: DeletePathUseCase,
    private val addNewDeliveryPathUseCase: AddNewPathUseCase
) : ViewModel(), KoinComponent {

    ///////////////////////////////////////////////////////////////////////////
    // Products
    ///////////////////////////////////////////////////////////////////////////
    fun updateProduct(product: UiProductObject) {
        updateProductUseCase.invoke(product.toDomainProduct())
    }

    fun deleteProduct(product: UiProductObject) {
        deleteProductUseCase.invoke(product.toDomainProduct())
    }

    fun addNewProduct(product: UiProductObject) {
        addNewProductUseCase.invoke(product.toDomainProduct())
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delivery Paths
    ///////////////////////////////////////////////////////////////////////////
    fun updateDeliveryPath(path: DeliveryPath) {
        updateDeliveryPathUseCase.invoke(path)
    }

    fun deleteDeliveryPath(path: DeliveryPath) {
        deleteDeliveryPathUseCase.invoke(path)
    }


    fun addNewDeliveryPath(path: DeliveryPath) {
        addNewDeliveryPathUseCase.invoke(path)
    }

}