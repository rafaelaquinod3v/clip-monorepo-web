package sv.com.clip.shared.pagination

import sv.com.clip.shared.pagination.SortOrder

data class PageQuery(
    val offset: Int,
    val limit: Int,
    val sortField: String,
    val sortOrder: SortOrder,
)
