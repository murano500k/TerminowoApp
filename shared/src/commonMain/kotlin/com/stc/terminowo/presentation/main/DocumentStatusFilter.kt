package com.stc.terminowo.presentation.main

import org.jetbrains.compose.resources.StringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.filter_active
import terminowo.shared.generated.resources.filter_all
import terminowo.shared.generated.resources.filter_expired
import terminowo.shared.generated.resources.filter_urgent

enum class DocumentStatusFilter(val labelRes: StringResource) {
    ALL(Res.string.filter_all),
    ACTIVE(Res.string.filter_active),
    URGENT(Res.string.filter_urgent),
    EXPIRED(Res.string.filter_expired)
}
