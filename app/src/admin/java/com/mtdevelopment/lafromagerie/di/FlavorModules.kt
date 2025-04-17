package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.admin.data.di.adminDataModule
import com.mtdevelopment.admin.domain.di.adminDomainModule
import com.mtdevelopment.admin.presentation.di.adminPresentationModule

fun flavorModules() = listOf(adminDataModule, adminDomainModule, adminPresentationModule)
