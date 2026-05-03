package io.github.sj42tech.route42.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun Route42ScreenList(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalSpacing: Dp = 16.dp,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

@Composable
internal fun Route42ScreenColumn(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        content()
    }
}
