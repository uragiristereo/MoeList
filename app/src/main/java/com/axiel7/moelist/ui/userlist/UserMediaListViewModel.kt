package com.axiel7.moelist.ui.userlist

import androidx.lifecycle.viewModelScope
import com.axiel7.moelist.data.model.anime.MyAnimeListStatus
import com.axiel7.moelist.data.model.anime.UserAnimeList
import com.axiel7.moelist.data.model.manga.MyMangaListStatus
import com.axiel7.moelist.data.model.manga.UserMangaList
import com.axiel7.moelist.data.model.media.BaseMediaNode
import com.axiel7.moelist.data.model.media.BaseMyListStatus
import com.axiel7.moelist.data.model.media.BaseUserMediaList
import com.axiel7.moelist.data.model.media.ListStatus
import com.axiel7.moelist.data.model.media.ListType
import com.axiel7.moelist.data.model.media.MediaSort
import com.axiel7.moelist.data.model.media.MediaType
import com.axiel7.moelist.data.repository.AnimeRepository
import com.axiel7.moelist.data.repository.DefaultPreferencesRepository
import com.axiel7.moelist.data.repository.MangaRepository
import com.axiel7.moelist.ui.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UserMediaListViewModel(
    mediaType: MediaType,
    initialListStatus: ListStatus? = null,
    private val animeRepository: AnimeRepository,
    private val mangaRepository: MangaRepository,
    private val defaultPreferencesRepository: DefaultPreferencesRepository,
) : BaseViewModel<UserMediaListUiState>(), UserMediaListEvent {

    override val mutableUiState = MutableStateFlow(
        UserMediaListUiState(
            mediaType = mediaType,
            listStatus = initialListStatus
                ?: if (mediaType == MediaType.MANGA) ListStatus.READING else ListStatus.WATCHING
        )
    )

    override fun onChangeStatus(value: ListStatus) {
        mutableUiState.update { it.copy(listStatus = value) }
    }

    override fun onChangeSort(value: MediaSort) {
        viewModelScope.launch(Dispatchers.IO) {
            when (mutableUiState.value.mediaType) {
                MediaType.ANIME -> defaultPreferencesRepository.setAnimeListSort(value)
                MediaType.MANGA -> defaultPreferencesRepository.setMangaListSort(value)
            }
            mutableUiState.update { it.copy(listSort = value) }
        }
    }

    override fun onChangeItemMyListStatus(value: BaseMyListStatus?, removed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            mutableUiState.value.run {
                if (value != null && selectedItem != null) {
                    val foundIndex = mediaList.indexOfFirst { it.node.id == selectedItem.node.id }
                    if (foundIndex != -1) {
                        if (mediaType == MediaType.ANIME) {
                            mediaList[foundIndex] = (mediaList[foundIndex] as UserAnimeList)
                                .copy(listStatus = myListStatus as MyAnimeListStatus)
                        } else if (mediaType == MediaType.MANGA) {
                            mediaList[foundIndex] = (mediaList[foundIndex] as UserMangaList)
                                .copy(listStatus = myListStatus as MyMangaListStatus)
                        }
                    }
                }
            }
        }
    }

    /*override var _myListStatus by object : MutableState<BaseMyListStatus?> {
        override var value: BaseMyListStatus?
            get() = selectedItem?.listStatus
            set(value) {
                when (value) {
                    is MyAnimeListStatus -> selectedItem =
                        (selectedItem as? UserAnimeList)?.copy(listStatus = value)

                    is MyMangaListStatus -> selectedItem =
                        (selectedItem as? UserMangaList)?.copy(listStatus = value)
                }
                onMediaItemStatusChanged(value)
            }

        override fun component1(): BaseMyListStatus? = value
        override fun component2(): (BaseMyListStatus?) -> Unit = { value = it }
    }*/

    override fun refreshList() {
        mutableUiState.update { it.copy(nextPage = null, loadMore = true) }
    }

    override fun loadMore() {
        mutableUiState.value.run {
            if (canLoadMore && !isLoadingMore) {
                mutableUiState.update { it.copy(loadMore = true) }
            }
        }
    }

    override fun onUpdateProgress(item: BaseUserMediaList<out BaseMediaNode>) {
        viewModelScope.launch(Dispatchers.IO) {
            setLoading(true)
            val result = if (mutableUiState.value.mediaType == MediaType.ANIME) {
                animeRepository.updateAnimeEntry(
                    animeId = item.node.id,
                    watchedEpisodes = item.listStatus?.progress?.plus(1),
                )
            } else {
                val isVolumeProgress =
                    (item as? UserMangaList)?.listStatus?.isUsingVolumeProgress() == true
                mangaRepository.updateMangaEntry(
                    mangaId = item.node.id,
                    chaptersRead = item.listStatus?.progress?.plus(1)
                        .takeIf { !isVolumeProgress },
                    volumesRead = (item.listStatus as? MyMangaListStatus)?.numVolumesRead?.plus(1)
                        .takeIf { isVolumeProgress },
                )
            }

            if (result != null) {
                mutableUiState.value.run {
                    val foundIndex = mediaList.indexOfFirst { it.node.id == item.node.id }
                    if (foundIndex != -1) {
                        if (mediaType == MediaType.ANIME) {
                            mediaList[foundIndex] = (mediaList[foundIndex] as UserAnimeList)
                                .copy(listStatus = result as MyAnimeListStatus)
                        } else if (mediaType == MediaType.MANGA) {
                            mediaList[foundIndex] = (mediaList[foundIndex] as UserMangaList)
                                .copy(listStatus = result as MyMangaListStatus)
                        }
                        val totalProgress = item.totalProgress()
                        val isMaxProgress = result.progress == totalProgress
                                || (result as? MyMangaListStatus)?.numVolumesRead == totalProgress
                        if (totalProgress != null && isMaxProgress) {
                            mutableUiState.update {
                                it.copy(
                                    lastItemUpdatedId = item.node.id,
                                    openSetAtCompletedDialog = true
                                )
                            }
                        }
                    }
                }
            }
            setLoading(false)
        }
    }

    override fun onItemSelected(item: BaseUserMediaList<*>) {
        mutableUiState.update { it.copy(selectedItem = item) }
    }

    override fun setAsCompleted(mediaId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setLoading(true)
            mutableUiState.value.run {
                val result = if (mediaType == MediaType.ANIME) {
                    animeRepository.updateAnimeEntry(
                        animeId = mediaId,
                        status = ListStatus.COMPLETED
                    )
                } else {
                    mangaRepository.updateMangaEntry(
                        mangaId = mediaId,
                        status = ListStatus.COMPLETED
                    )
                }

                if (result != null) {
                    mediaList.removeIf { it.node.id == mediaId }
                }
            }
            setLoading(false)
        }
    }

    override fun toggleSortDialog(open: Boolean) {
        mutableUiState.update { it.copy(openSortDialog = open) }
    }

    override fun toggleSetAsCompleteDialog(open: Boolean) {
        mutableUiState.update { it.copy(openSetAtCompletedDialog = open) }
    }

    override fun getRandomIdOfList() {
        viewModelScope.launch(Dispatchers.IO) {
            mutableUiState.run {
                emit(value.copy(isLoadingRandom = true))
                val result = if (value.mediaType == MediaType.ANIME) {
                    animeRepository.getAnimeIdsOfUserList(status = value.listStatus)
                } else {
                    mangaRepository.getMangaIdsOfUserList(status = value.listStatus)
                }
                if (result.data != null) {
                    emit(
                        value.copy(
                            randomId = result.data.random(),
                            isLoadingRandom = false,
                        )
                    )
                } else {
                    emit(value.copy(isLoadingRandom = false))
                }
            }
        }
    }

    override fun onRandomIdOpen() {
        mutableUiState.update { it.copy(randomId = null) }
    }

    init {
        // sort
        val listSortFlow = when (mediaType) {
            MediaType.ANIME -> defaultPreferencesRepository.animeListSort
            MediaType.MANGA -> defaultPreferencesRepository.mangaListSort
        }
        listSortFlow
            .onEach { value ->
                mutableUiState.update { it.copy(listSort = value) }
            }
            .launchIn(viewModelScope)

        // list styles
        combine(
            defaultPreferencesRepository.useGeneralListStyle,
            defaultPreferencesRepository.generalListStyle
        ) { useGeneral, generalStyle ->
            if (useGeneral) {
                mutableUiState.update { it.copy(listStyle = generalStyle) }
            } else {
                mutableUiState
                    .flatMapLatest {
                        ListType(it.listStatus, it.mediaType)
                            .stylePreference(defaultPreferencesRepository)
                    }.collect { listStyle ->
                        mutableUiState.update { it.copy(listStyle = listStyle) }
                    }
            }
        }.launchIn(viewModelScope)

        defaultPreferencesRepository.gridItemsPerRow
            .onEach { value ->
                mutableUiState.update { it.copy(itemsPerRow = value) }
            }
            .launchIn(viewModelScope)


        defaultPreferencesRepository.randomListEntryEnabled
            .onEach { value ->
                mutableUiState.update { it.copy(showRandomButton = value) }
            }
            .launchIn(viewModelScope)

        // list loading and pagination
        viewModelScope.launch(Dispatchers.IO) {
            mutableUiState
                .distinctUntilChanged { old, new ->
                    old.loadMore == new.loadMore
                            && old.listStatus == new.listStatus
                            && old.listSort == new.listSort
                }
                .filter { it.listSort != null && it.loadMore }
                .collectLatest { uiState ->
                    mutableUiState.update {
                        it.copy(
                            isLoadingMore = true,
                            isLoading = uiState.nextPage == null
                        )
                    }
                    val result = if (uiState.mediaType == MediaType.ANIME) {
                        animeRepository.getUserAnimeList(
                            status = uiState.listStatus,
                            sort = uiState.listSort!!,
                            page = uiState.nextPage
                        )
                    } else {
                        mangaRepository.getUserMangaList(
                            status = uiState.listStatus,
                            sort = uiState.listSort!!,
                            page = uiState.nextPage
                        )
                    }

                    if (result.data != null) {
                        if (uiState.nextPage == null) uiState.mediaList.clear()
                        uiState.mediaList.addAll(result.data)

                        mutableUiState.update {
                            it.copy(
                                loadMore = false,
                                nextPage = result.paging?.next,
                                isLoadingMore = false,
                                isLoading = false
                            )
                        }
                    } else {
                        mutableUiState.update {
                            it.copy(
                                loadMore = false,
                                isLoadingMore = false,
                                isLoading = false,
                                message = result.message ?: result.error
                            )
                        }
                    }
                }
        }
    }
}