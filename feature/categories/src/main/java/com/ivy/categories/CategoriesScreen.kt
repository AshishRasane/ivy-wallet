package com.ivy.categories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.ivy.design.revamp.AmountFormat
import com.ivy.design.revamp.RevampCircleButton
import com.ivy.design.revamp.RevampTopBar
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.legacy.IvyWalletPreview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.ui.SearchInput
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.data.SortOrder
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.CategoriesScreen(screen: CategoriesScreen) {
    val viewModel: CategoriesViewModel = screenScopedViewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: CategoriesScreenState = CategoriesScreenState(
        compactCategoriesModeEnabled = false,
        showCategorySearchBar = false
    ),
    onEvent: (CategoriesScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val ivyContext = com.ivy.legacy.ivyWalletCtx()
    val colors = revampColors()
    var listState = rememberLazyListState()
    if (!state.categories.isEmpty()) {
        listState = rememberScrollPositionListState(
            key = "categories_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.categoriesListState?.firstVisibleItemIndex
                ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.categoriesListState?.firstVisibleItemScrollOffset
                ?: 0
        )
    }
    // the bar shows each category's share of this month's spending
    val totalExpenses = state.categories.sumOf { it.monthlyExpenses }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            RevampTopBar(title = stringResource(R.string.categories), onBack = { nav.back() }) {
                RevampCircleButton(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort categories",
                    onClick = { onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = true)) },
                )
                RevampCircleButton(
                    icon = Icons.Filled.SwapVert,
                    contentDescription = "Reorder categories",
                    onClick = { onEvent(CategoriesScreenEvent.OnReorderModalVisible(true)) },
                )
            }
            if (state.showCategorySearchBar) {
                SearchField(onSearch = { onEvent(CategoriesScreenEvent.OnSearchQueryUpdate(it)) })
            }
        }

        items(state.categories, key = { it.category.id.value }) { categoryData ->
            CategoryRow(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                currency = state.baseCurrency,
                categoryData = categoryData,
                share = if (totalExpenses > 0) (categoryData.monthlyExpenses / totalExpenses).toFloat() else 0f,
                compact = state.compactCategoriesModeEnabled,
                onLongClick = { onEvent(CategoriesScreenEvent.OnReorderModalVisible(true)) },
                onClick = {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = null,
                            categoryId = categoryData.category.id.value
                        )
                    )
                },
            )
        }

        item {
            AddCategoryButton(
                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                onClick = {
                    onEvent(CategoriesScreenEvent.OnCategoryModalVisible(CategoryModalData(category = null)))
                },
            )
        }
    }

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = state.categories,
        dismiss = {
            onEvent(CategoriesScreenEvent.OnReorderModalVisible(false))
        },
        onReordered = {
            onEvent(CategoriesScreenEvent.OnReorder(it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.category.name.value,
            style = UI.typo.b1.style(
                color = item.category.color.value.toComposeColor(),
                fontWeight = FontWeight.Bold
            )
        )
    }

    CategoryModal(
        modal = state.categoryModalData,
        onCreateCategory = {
            onEvent(CategoriesScreenEvent.OnCreateCategory(it))
        },
        onEditCategory = { },
        dismiss = {
            onEvent(CategoriesScreenEvent.OnCategoryModalVisible(null))
        }
    )

    SortModal(
        initialType = state.sortOrder,
        items = state.sortOrderItems,
        visible = state.sortModalVisible,
        dismiss = {
            onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = false))
        },
        onSortOrderChange = {
            onEvent(CategoriesScreenEvent.OnReorder(state.categories, it))
        }
    )
}

private val ScreenPadding = 20.dp

@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRow(
    currency: String,
    categoryData: CategoryData,
    /** 0..1 share of this month's spending */
    share: Float,
    compact: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    val category = categoryData.category
    val categoryColor = category.color.value.toComposeColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Reorder")
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 36.dp else 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(categoryColor.copy(alpha = AvatarTintAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = category.name.value,
                    style = RevampType.bodyStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = AmountFormat.format(-categoryData.monthlyExpenses, currency),
                    style = RevampType.bodyStrong,
                    color = if (categoryData.monthlyExpenses > 0) colors.expense else colors.inkMuted,
                )
            }
            if (!compact) {
                if (categoryData.monthlyIncome > 0) {
                    Text(
                        text = "Earned ${AmountFormat.format(categoryData.monthlyIncome, currency)} this month",
                        style = RevampType.label,
                        color = colors.income,
                    )
                }
                ShareBar(share = share, color = categoryColor)
            }
        }
    }
}

@Composable
private fun ShareBar(share: Float, color: Color) {
    val colors = revampColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(colors.divider)
    ) {
        if (share > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share.coerceIn(MinShare, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun AddCategoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = revampColors()
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .border(2.dp, colors.primary, shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onPrimaryTint)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.add_category), style = RevampType.bodyStrong, color = colors.onPrimaryTint)
    }
}

private const val AvatarTintAlpha = 0.18f
private const val MinShare = 0.02f

@Suppress("UnusedParameter")
@Composable
fun BoxWithConstraintsScope.SortModal(
    items: ImmutableList<SortOrder>,
    visible: Boolean,
    initialType: SortOrder,
    dismiss: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.sort_by),
    id: UUID = UUID.randomUUID()
) {
    var sortOrder by remember(initialType) {
        mutableStateOf(initialType)
    }

    val applyChange = {
        onSortOrderChange(sortOrder)
        dismiss()
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSet {
                applyChange()
            }
        },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(32.dp))

        items.forEach {
            SelectTypeButton(
                text = it.displayName,
                icon = when (it) {
                    SortOrder.DEFAULT -> R.drawable.ic_custom_star_s
                    SortOrder.BALANCE_AMOUNT -> R.drawable.ic_vue_money_coins
                    SortOrder.EXPENSES -> R.drawable.ic_expense
                    SortOrder.ALPHABETICAL -> R.drawable.ic_sort_by_alpha_24
                },
                selected = it == sortOrder
            ) {
                sortOrder = it
                applyChange()
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SelectTypeButton(
    text: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    selectedGradient: Gradient = GradientGreen,
    textSelectedColor: Color = White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(UI.shapes.r4)
            .background(
                brush = if (selected) selectedGradient.asHorizontalBrush() else SolidColor(UI.colors.medium),
                shape = UI.shapes.r4
            )
            .clickable {
                onClick()
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        val textColor = if (selected) textSelectedColor else UI.colors.pureInverse

        IvyIcon(
            icon = icon,
            tint = textColor,
            modifier = Modifier.fillMaxHeight()
        )

        Spacer(Modifier.width(12.dp))

        Text(
            modifier = Modifier.wrapContentHeight(),
            text = text,
            style = UI.typo.b1.style(
                color = textColor
            ),
            textAlign = TextAlign.Center,
        )

        if (selected) {
            Spacer(Modifier.weight(1f))

            IvyIcon(
                icon = R.drawable.ic_check,
                tint = textSelectedColor
            )

            Text(
                text = stringResource(R.string.selected_text),
                style = UI.typo.b2.style(
                    fontWeight = FontWeight.SemiBold,
                    color = textSelectedColor
                )
            )

            Spacer(Modifier.width(24.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewCategoriesCompactModeEnabled(theme: Theme = Theme.LIGHT) {
    Preview(theme = theme, compactModeEnabled = true)
}

@Preview
@Composable
private fun PreviewCategoriesCompactModeEnabledAndSearchBarEnabled(theme: Theme = Theme.LIGHT) {
    Preview(theme = theme, compactModeEnabled = true, displaySearchBarEnabled = true)
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    displaySearchBarEnabled: Boolean = false
) {
    IvyWalletPreview(theme) {
        val state = CategoriesScreenState(
            baseCurrency = "INR",
            compactCategoriesModeEnabled = compactModeEnabled,
            showCategorySearchBar = displaySearchBarEnabled,
            categories = persistentListOf(
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Groceries"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("groceries"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 2125.0,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Fun"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("game"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 0.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Ivy"),
                        color = ColorInt(IvyDark.toArgb()),
                        icon = IconAsset.unsafe("star"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 5000.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        color = ColorInt(GreenLight.toArgb()),
                        icon = IconAsset.unsafe("atom"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 12125.21,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(GreenDark.toArgb()),
                        icon = IconAsset.unsafe("drink"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 820.0,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),

                )
        )
        UI(state = state)
    }
}

@Preview
@Composable
private fun PreviewWithSearchBarEnabled(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    displaySearchBarEnabled: Boolean = true
) {
    IvyWalletPreview(theme) {
        val state = CategoriesScreenState(
            baseCurrency = "INR",
            compactCategoriesModeEnabled = compactModeEnabled,
            showCategorySearchBar = displaySearchBarEnabled,
            categories = persistentListOf(
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Groceries"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("groceries"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 2125.0,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Fun"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("game"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 0.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Ivy"),
                        color = ColorInt(IvyDark.toArgb()),
                        icon = IconAsset.unsafe("star"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 5000.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        color = ColorInt(GreenLight.toArgb()),
                        icon = IconAsset.unsafe("atom"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 12125.21,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(GreenDark.toArgb()),
                        icon = IconAsset.unsafe("drink"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 820.0,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),

                )
        )
        UI(state = state)
    }
}

@Composable
private fun SearchField(
    onSearch: (String) -> Unit,
) {
    var searchQueryTextFieldValue by remember {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    SearchInput(
        searchQueryTextFieldValue = searchQueryTextFieldValue,
        hint = "Search categories",
        focus = false,
        showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
        onSetSearchQueryTextField = {
            searchQueryTextFieldValue = it
            onSearch(it.text)
        }
    )
}

/** For screenshot testing */
@Composable
fun CategoriesScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenWithSearchBarUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme = theme, displaySearchBarEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenCompactUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, compactModeEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenWithSearchBarCompactUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, compactModeEnabled = true, displaySearchBarEnabled = true)
}