package com.xwab.app.composition

import androidx.navigation3.runtime.EntryProviderScope
import com.xwab.app.navigation.Navigator
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

/**
 * Collects what features contribute to one navigation host, independently of the ViewModel graph:
 * their entry installers, the now-playing bar, and the shell's callbacks those installers take.
 *
 * Features contribute with `@ContributesTo(EntryProviderScope::class)`, so a feature on this
 * module's classpath is installed without being listed here. The scope is Navigation 3's own type
 * because it is the one class every side already sees. This graph lives here, the only module that
 * sees feature implementations, and the shell reads it only as [AppEntries].
 *
 * The input is the navigator the host built over the stacks Compose restored; every callback the
 * shell contributes drives that one navigator.
 */
@DependencyGraph(EntryProviderScope::class)
internal interface AppEntryGraph : AppEntries {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides navigator: Navigator): AppEntryGraph
    }
}

/** One entry graph per navigation host. Public for the shell's tests, which draw real entries. */
object AppEntryGraphs : AppEntriesFactory {
    override fun create(navigator: Navigator): AppEntries =
        createGraphFactory<AppEntryGraph.Factory>().create(navigator)
}
