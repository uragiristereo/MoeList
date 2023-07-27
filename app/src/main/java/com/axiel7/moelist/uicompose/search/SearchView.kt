package com.axiel7.moelist.uicompose.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axiel7.moelist.R
import com.axiel7.moelist.data.model.anime.AnimeList
import com.axiel7.moelist.data.model.anime.seasonYearText
import com.axiel7.moelist.data.model.manga.MangaList
import com.axiel7.moelist.data.model.media.MediaType
import com.axiel7.moelist.data.model.media.durationText
import com.axiel7.moelist.data.model.media.mediaFormatLocalized
import com.axiel7.moelist.data.model.media.totalDuration
import com.axiel7.moelist.data.model.media.userPreferredTitle
import com.axiel7.moelist.uicompose.composables.media.MediaItemDetailed
import com.axiel7.moelist.uicompose.composables.media.MediaItemDetailedPlaceholder
import com.axiel7.moelist.uicompose.composables.OnBottomReached
import com.axiel7.moelist.uicompose.theme.MoeListTheme
import com.axiel7.moelist.utils.ContextExtensions.showToast
import com.axiel7.moelist.utils.NumExtensions.toStringPositiveValueOrNull
import com.axiel7.moelist.utils.NumExtensions.toStringPositiveValueOrUnknown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(
    query: String,
    performSearch: MutableState<Boolean>,
    navigateToMediaDetails: (MediaType, Int) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel()
    val listState = rememberLazyListState()
    var mediaType by remember { mutableStateOf(MediaType.ANIME) }

    listState.OnBottomReached(buffer = 3) {
        if (!viewModel.isLoading && viewModel.hasNextPage) {
            viewModel.search(
                mediaType = mediaType,
                query = query,
                page = viewModel.nextPage
            )
        }
    }

    LaunchedEffect(viewModel.message) {
        if (viewModel.showMessage) {
            context.showToast(viewModel.message)
            viewModel.showMessage = false
        }
    }

    LaunchedEffect(mediaType, performSearch.value) {
        if (query.isNotBlank() && performSearch.value) {
            viewModel.search(
                mediaType = mediaType,
                query = query
            )
            performSearch.value = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState
    ) {
        item {
            Row {
                FilterChip(
                    selected = mediaType == MediaType.ANIME,
                    onClick = {
                        mediaType = MediaType.ANIME
                        if (query.isNotBlank()) performSearch.value = true
                    },
                    label = { Text(text = stringResource(R.string.anime)) },
                    modifier = Modifier.padding(start = 8.dp),
                    leadingIcon = {
                        if (mediaType == MediaType.ANIME) {
                            Icon(
                                painter = painterResource(R.drawable.round_check_24),
                                contentDescription = "check"
                            )
                        }
                    }
                )
                FilterChip(
                    selected = mediaType == MediaType.MANGA,
                    onClick = {
                        mediaType = MediaType.MANGA
                        if (query.isNotBlank()) performSearch.value = true
                    },
                    label = { Text(text = stringResource(R.string.manga)) },
                    modifier = Modifier.padding(start = 8.dp),
                    leadingIcon = {
                        if (mediaType == MediaType.MANGA) {
                            Icon(
                                painter = painterResource(R.drawable.round_check_24),
                                contentDescription = "check"
                            )
                        }
                    }
                )
            }
        }
        items(
            items = viewModel.mediaList,
            contentType = { it.node }
        ) {
            MediaItemDetailed(
                title = it.node.userPreferredTitle(),
                imageUrl = it.node.mainPicture?.large,
                subtitle1 = {
                    Text(
                        text = buildString {
                            append(it.node.mediaType?.mediaFormatLocalized())
                            if (it.node.totalDuration().toStringPositiveValueOrNull() != null) {
                                append(" (${it.node.durationText()})")
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                subtitle2 = {
                    Text(
                        text = when (it) {
                            is AnimeList -> it.node.startSeason.seasonYearText()
                            is MangaList -> it.node.startDate ?: stringResource(R.string.unknown)
                            else -> stringResource(R.string.unknown)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                subtitle3 = {
                    Icon(
                        painter = painterResource(R.drawable.ic_round_details_star_24),
                        contentDescription = "star",
                        modifier = Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it.node.mean.toStringPositiveValueOrUnknown(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    navigateToMediaDetails(mediaType, it.node.id)
                }
            )
        }
        if (query.isNotBlank() && viewModel.mediaList.isEmpty()) {
            if (viewModel.isLoading) {
                items(10) {
                    MediaItemDetailedPlaceholder()
                }
            } else if (performSearch.value) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_results),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }//: LazyColumn
}

@Preview(showBackground = true)
@Composable
fun SearchPreview() {
    MoeListTheme {
        SearchView(
            query = "one",
            performSearch = remember { mutableStateOf(false) },
            navigateToMediaDetails = { _, _ -> }
        )
    }
}