package com.xwab.app.composition

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.nowplaying.shell.NowPlayingBar
import com.xwab.app.navigation.Navigator

/**
 * What features contribute to one navigation host: the entry installers behind their routes and
 * the now-playing bar drawn under every tab.
 *
 * Both come from feature implementations, which this module does not see. The composition root
 * collects them with Metro and hands them over through [AppEntriesFactory], so the shell compiles
 * against feature contracts alone.
 */
interface AppEntries {
    val entryProviderInstallers: Set<EntryProviderScope<NavKey>.() -> Unit>
    val nowPlayingBar: NowPlayingBar
}

/**
 * Builds [AppEntries] for one host, over the navigator that host owns.
 *
 * The host builds the navigator from the stacks it restored and passes it in, because the entry
 * callbacks [AppEntryCallbacks] supplies must drive that same navigator.
 */
fun interface AppEntriesFactory {
    fun create(navigator: Navigator): AppEntries
}
