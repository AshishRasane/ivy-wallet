package com.ivy.accounts

import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<com.ivy.legacy.data.model.AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent
    data class CreateAccount(val data: CreateAccountData) : AccountsEvent
}
