package sv.com.clip.shared.pagination

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

fun PageQuery.toPageable(): Pageable = PageRequest.of(
  offset / limit,
  limit,
  Sort.by(Sort.Direction.valueOf(sortOrder.name), sortField)
)
