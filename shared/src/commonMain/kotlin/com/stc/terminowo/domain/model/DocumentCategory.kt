package com.stc.terminowo.domain.model

import org.jetbrains.compose.resources.StringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.category_agreement
import terminowo.shared.generated.resources.category_driver_license
import terminowo.shared.generated.resources.category_insurance
import terminowo.shared.generated.resources.category_other
import terminowo.shared.generated.resources.category_payment
import terminowo.shared.generated.resources.category_technical_inspection

enum class DocumentCategory(val key: String, val labelRes: StringResource) {
    INSURANCE("insurance", Res.string.category_insurance),
    PAYMENT("payment", Res.string.category_payment),
    AGREEMENT("agreement", Res.string.category_agreement),
    DRIVER_LICENSE("driver_license", Res.string.category_driver_license),
    TECHNICAL_INSPECTION("technical_inspection", Res.string.category_technical_inspection),
    OTHER("other", Res.string.category_other);

    companion object {
        val DEFAULT = OTHER

        fun fromKey(key: String?): DocumentCategory =
            entries.find { it.key == key } ?: OTHER
    }
}
