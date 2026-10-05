package org.shawnz.dailytracker.browser

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri

/**
 * Opens a game in a Custom Tab.
 *
 * A game opens in this app's own task, so leaving the tab returns to the screen that started
 * it and the app stays one entry in recents.
 */
object CustomTabLauncher {
    private var connection: CustomTabsServiceConnection? = null
    private var session: CustomTabsSession? = null
    private var likely: List<String> = emptyList()

    /**
     * Connects to the browser and warms it up.
     *
     * Does nothing when already connected, or when no installed browser supports Custom Tabs.
     */
    fun bind(context: Context) {
        if (connection != null) return
        val app = context.applicationContext
        val browser = CustomTabsClient.getPackageName(app, null) ?: return

        val newConnection =
            object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(
                    name: ComponentName,
                    client: CustomTabsClient,
                ) {
                    client.warmup(0)
                    session = client.newSession(null)
                    sendLikely()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    session = null
                }
            }

        connection = newConnection
        val bound =
            runCatching {
                CustomTabsClient.bindCustomTabsService(app, browser, newConnection)
            }.getOrDefault(false)
        if (!bound) connection = null
    }

    fun unbind(context: Context) {
        val current = connection ?: return
        connection = null
        session = null
        runCatching { context.applicationContext.unbindService(current) }
    }

    /**
     * Lists the games that could be opened next, in order of likelihood.
     *
     * May be called before [bind]. Each call replaces the previous list.
     */
    fun prepare(urls: List<String>) {
        likely = urls
        sendLikely()
    }

    private fun sendLikely() {
        val current = session ?: return
        val ranked =
            likely.map { url ->
                Bundle().apply { putParcelable(CustomTabsService.KEY_URL, normalize(url)) }
            }
        if (ranked.isEmpty()) return
        runCatching { current.mayLaunchUrl(null, null, ranked) }
    }

    fun launch(
        context: Context,
        url: String,
        toolbarColor: Int,
    ) {
        val uri = normalize(url)
        val intent =
            CustomTabsIntent
                .Builder(session)
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams
                        .Builder()
                        .setToolbarColor(toolbarColor)
                        .build(),
                ).build()

        try {
            intent.launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            // No browser handles Custom Tabs, so fall back to whatever will open a URL.
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
    }

    /** Adds a scheme when the stored URL has none. A custom game is saved as typed. */
    internal fun normalize(url: String): Uri {
        val trimmed = url.trim()
        val withScheme =
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        return withScheme.toUri()
    }
}
