package dk.schulz.quiettype.models

object HttpDownloadPolicy {
    fun requireSuccessfulStatus(statusCode: Int): Int {
        check(statusCode in 200..299) { "Model download failed with HTTP status $statusCode" }
        return statusCode
    }
}
