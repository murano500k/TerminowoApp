package com.stc.terminowo.data.remote

data class DocumentAiConfig(
    val projectId: String,
    val location: String,
    val processorId: String,
    val apiKey: String
) {
    val endpoint: String
        get() = "https://${location}-documentai.googleapis.com/v1/projects/$projectId/locations/$location/processors/$processorId:process"
}
