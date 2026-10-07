package com.ivy.features

import kotlinx.collections.immutable.ImmutableList

data class FeaturesUiState(
    val featureItemViewStates: ImmutableList<FeatureItemViewState>,
)

sealed interface FeatureItemViewState {
    data class FeatureToggleViewState(
        val key: String,
        val name: String,
        val enabled: Boolean,
        val description: String?,
        /** Android runtime permissions that must be granted before enabling the feature. */
        val requiredPermissions: ImmutableList<String>,
    ) : FeatureItemViewState

    data class FeatureHeaderViewState(val name: String) : FeatureItemViewState
}
