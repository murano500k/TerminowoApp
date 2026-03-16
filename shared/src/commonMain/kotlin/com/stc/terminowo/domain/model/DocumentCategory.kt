package com.stc.terminowo.domain.model

import org.jetbrains.compose.resources.StringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.category_agreement
import terminowo.shared.generated.resources.category_documents
import terminowo.shared.generated.resources.category_health
import terminowo.shared.generated.resources.category_insurance
import terminowo.shared.generated.resources.category_other
import terminowo.shared.generated.resources.category_payments
import terminowo.shared.generated.resources.category_subscriptions
import terminowo.shared.generated.resources.category_technical_inspection

enum class DocumentCategory(val key: String, val labelRes: StringResource) {
    INSURANCE("insurance", Res.string.category_insurance),
    PAYMENTS("payments", Res.string.category_payments),
    AGREEMENT("agreement", Res.string.category_agreement),
    DOCUMENTS("documents", Res.string.category_documents),
    TECHNICAL_INSPECTION("technical_inspection", Res.string.category_technical_inspection),
    SUBSCRIPTIONS("subscriptions", Res.string.category_subscriptions),
    HEALTH("health", Res.string.category_health),
    OTHER("other", Res.string.category_other);

    companion object {
        val DEFAULT = OTHER

        // Legacy key aliases for backward compatibility with existing DB rows
        private val legacyAliases = mapOf(
            "payment" to PAYMENTS,
            "driver_license" to DOCUMENTS
        )

        fun fromKey(key: String?): DocumentCategory =
            entries.find { it.key == key }
                ?: legacyAliases[key]
                ?: OTHER
    }
}
