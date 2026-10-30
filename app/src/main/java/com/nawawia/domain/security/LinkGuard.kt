package com.nawawia.domain.security

import com.nawawia.core.error.AppError
import com.nawawia.core.result.AppResult
import java.net.URI

object LinkGuard {

    private val ALLOWED_ROOT_HOSTS = setOf(
        "dorar.net",
        "islamqa.info",
        "sunnah.com",
        "shamela.ws"
    )

    /**
     * Validates an external URL against strict HTTPS, host allowlist, port, and userinfo rules.
     */
    fun validate(rawUrl: String): AppResult<String> {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Empty URL"))
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Malformed URI"))
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "https") {
            return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Only HTTPS is permitted"))
        }

        if (uri.userInfo != null || uri.rawUserInfo != null) {
            return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Userinfo in URL is prohibited"))
        }

        if (uri.port != -1 && uri.port != 443) {
            return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Non-standard port is prohibited"))
        }

        val host = uri.host?.lowercase()?.removeSuffix(".")
            ?: return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Missing host"))

        val isAllowedHost = ALLOWED_ROOT_HOSTS.any { allowedRoot ->
            host == allowedRoot || host.endsWith(".$allowedRoot")
        }

        if (!isAllowedHost) {
            return AppResult.Failure(AppError.LinkBlocked(rawUrl, "Host '$host' is not on the scholarly allowlist"))
        }

        return AppResult.Success(trimmed)
    }

    fun isAllowed(rawUrl: String): Boolean = validate(rawUrl).isSuccess
}
