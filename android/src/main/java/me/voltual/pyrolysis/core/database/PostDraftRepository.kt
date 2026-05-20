package me.voltual.pyrolysis.core.database

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import me.voltual.pyrolysis.BBQApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlinx.io.files.Path

@Single
class PostDraftRepository {
    private val postDraftDao = BBQApplication.instance.database.postDraftDao()

    val draftFlow: Flow<DraftDto?> = postDraftDao.getDraft().map { entity ->
        entity?.let {
            DraftDto(
                title = it.title,
                content = it.content,
                // 使用 kotlinx-io 的 Path 构造 PlatformFile
                imageUris = it.imageUris.split(",")
                    .filter { s -> s.isNotEmpty() }
                    .map { pathStr -> PlatformFile(Path(pathStr)) },
                imageUrls = it.imageUrls,
                subsectionId = it.subsectionId
            )
        }
    }

    suspend fun saveDraft(draft: DraftDto) {
        val entity = PostDraft(
            title = draft.title,
            content = draft.content,
            // 存储路径字符串
            imageUris = draft.imageUris.joinToString(",") { it.path },
            imageUrls = draft.imageUrls,
            subsectionId = draft.subsectionId
        )
        postDraftDao.save(entity)
    }

    suspend fun clearDraft() {
        postDraftDao.clear()
    }

    data class DraftDto(
        val title: String,
        val content: String,
        val imageUris: List<PlatformFile>,
        val imageUrls: String,
        val subsectionId: Int
    )
}