package com.xwab.app.composition

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.browse.impl.BrowseComponent
import com.xwab.app.feature.browse.impl.DefaultBrowseComponent
import com.xwab.app.feature.category.api.navigation.CategoryConfig
import com.xwab.app.feature.category.impl.CategoryComponent
import com.xwab.app.feature.category.impl.DefaultCategoryComponent
import com.xwab.app.feature.favorites.impl.DefaultFavoritesComponent
import com.xwab.app.feature.favorites.impl.FavoritesComponent
import com.xwab.app.feature.sounds.api.navigation.PlayerConfig
import com.xwab.app.feature.sounds.impl.DefaultPlayerComponent
import com.xwab.app.feature.sounds.impl.PlayerComponent
import com.xwab.app.feature.story.impl.DefaultStoriesComponent
import com.xwab.app.feature.story.impl.StoriesComponent
import com.xwab.app.navigation.AppTab
import com.xwab.app.navigation.BrowseTabConfig
import com.xwab.app.navigation.FavoritesTabConfig
import com.xwab.app.navigation.StoriesTabConfig
import com.xwab.app.navigation.TabBackStackPolicy
import org.koin.core.Koin

/**
 * The app shell's navigation facade: which tab is selected, and each tab's own back stack.
 *
 * This is the one file allowed to import feature implementations — the composition root, same
 * boundary the old `AppEntryProvider.kt` held. `:shared/navigation` stays feature-API-only.
 */
interface AppComponent {
    val selectedTab: Value<AppTab>
    val browseStack: Value<ChildStack<*, BrowseTabChild>>
    val favoritesStack: Value<ChildStack<*, FavoritesTabChild>>
    val storiesStack: Value<ChildStack<*, StoriesTabChild>>
    fun selectTab(tab: AppTab)
}

/** One case per screen a tab can show, each holding the component that screen renders from. */
sealed interface BrowseTabChild {
    class Root(val component: BrowseComponent) : BrowseTabChild
    class Category(val component: CategoryComponent) : BrowseTabChild
    class Player(val component: PlayerComponent) : BrowseTabChild
}

sealed interface FavoritesTabChild {
    class Root(val component: FavoritesComponent) : FavoritesTabChild
    class Player(val component: PlayerComponent) : FavoritesTabChild
}

sealed interface StoriesTabChild {
    class Root(val component: StoriesComponent) : StoriesTabChild
}

/**
 * Builds and owns every tab's [StackNavigation], created once as a property of this single
 * retained component rather than lazily on first selection — so a tab's components, and the
 * coroutine scopes and hot flows they hold, survive switching away from it exactly like every
 * other retained component in the tree.
 */
class DefaultAppComponent(
    componentContext: ComponentContext,
    private val koin: Koin,
) : AppComponent, ComponentContext by componentContext {

    private val browseNavigation = StackNavigation<BrowseTabConfig>()
    private val favoritesNavigation = StackNavigation<FavoritesTabConfig>()
    private val storiesNavigation = StackNavigation<StoriesTabConfig>()

    override val browseStack: Value<ChildStack<*, BrowseTabChild>> = childStack(
        source = browseNavigation,
        serializer = BrowseTabConfig.serializer(),
        initialConfiguration = BrowseTabConfig.Root,
        key = "BrowseStack",
        handleBackButton = false,
        childFactory = ::browseChild,
    )

    override val favoritesStack: Value<ChildStack<*, FavoritesTabChild>> = childStack(
        source = favoritesNavigation,
        serializer = FavoritesTabConfig.serializer(),
        initialConfiguration = FavoritesTabConfig.Root,
        key = "FavoritesStack",
        handleBackButton = false,
        childFactory = ::favoritesChild,
    )

    override val storiesStack: Value<ChildStack<*, StoriesTabChild>> = childStack(
        source = storiesNavigation,
        serializer = StoriesTabConfig.serializer(),
        initialConfiguration = StoriesTabConfig.Root,
        key = "StoriesStack",
        handleBackButton = false,
        childFactory = ::storiesChild,
    )

    /** The tab rules themselves live in [TabBackStackPolicy], tested there with fake tabs. */
    private val tabPolicy = TabBackStackPolicy(
        startTab = AppTab.BROWSE,
        stackSize = { tab ->
            when (tab) {
                AppTab.BROWSE -> browseStack.value.items.size
                AppTab.FAVORITES -> favoritesStack.value.items.size
                AppTab.STORIES -> storiesStack.value.items.size
            }
        },
        pop = { tab ->
            when (tab) {
                AppTab.BROWSE -> browseNavigation.pop()
                AppTab.FAVORITES -> favoritesNavigation.pop()
                AppTab.STORIES -> storiesNavigation.pop()
            }
        },
        clearSubStack = { tab ->
            when (tab) {
                AppTab.BROWSE -> browseNavigation.popTo(index = 0)
                AppTab.FAVORITES -> favoritesNavigation.popTo(index = 0)
                AppTab.STORIES -> storiesNavigation.popTo(index = 0)
            }
        },
    )

    override val selectedTab: Value<AppTab> = tabPolicy.selectedTab

    override fun selectTab(tab: AppTab) = tabPolicy.selectTab(tab)

    init {
        // Centralized rather than one `handleBackButton = true` per stack: only the visible tab
        // should ever respond to back, and letting every stack register its own callback leaves
        // that to essenty's dispatch order instead of this component's own tab-aware policy.
        backHandler.register(BackCallback(isEnabled = true) { tabPolicy.goBack() })
    }

    private fun browseChild(config: BrowseTabConfig, componentContext: ComponentContext): BrowseTabChild =
        when (config) {
            is BrowseTabConfig.Root -> BrowseTabChild.Root(
                DefaultBrowseComponent(
                    componentContext = componentContext,
                    musicCatalog = koin.get(),
                    onCategoryClick = { categoryId ->
                        browseNavigation.pushToFront(BrowseTabConfig.Category(CategoryConfig(categoryId.value)))
                    },
                ),
            )

            is BrowseTabConfig.Category -> BrowseTabChild.Category(
                DefaultCategoryComponent(
                    componentContext = componentContext,
                    categoryId = CategoryId(config.config.categoryId),
                    observeCategoryContentUseCase = koin.get(),
                    favoritesRepository = koin.get(),
                    playbackCoordinator = koin.get(),
                    onMusicClick = { musicId ->
                        browseNavigation.pushToFront(BrowseTabConfig.Player(PlayerConfig(musicId.value)))
                    },
                    onBack = { browseNavigation.pop() },
                ),
            )

            is BrowseTabConfig.Player -> BrowseTabChild.Player(
                DefaultPlayerComponent(
                    componentContext = componentContext,
                    trackId = TrackId(config.config.musicId),
                    observePlayerContentUseCase = koin.get(),
                    favoritesRepository = koin.get(),
                    playbackCoordinator = koin.get(),
                    onBack = { browseNavigation.pop() },
                ),
            )
        }

    private fun favoritesChild(config: FavoritesTabConfig, componentContext: ComponentContext): FavoritesTabChild =
        when (config) {
            is FavoritesTabConfig.Root -> FavoritesTabChild.Root(
                DefaultFavoritesComponent(
                    componentContext = componentContext,
                    observeFavoritesContentUseCase = koin.get(),
                    playbackCoordinator = koin.get(),
                    onMusicClick = { musicId ->
                        favoritesNavigation.pushToFront(FavoritesTabConfig.Player(PlayerConfig(musicId.value)))
                    },
                ),
            )

            is FavoritesTabConfig.Player -> FavoritesTabChild.Player(
                DefaultPlayerComponent(
                    componentContext = componentContext,
                    trackId = TrackId(config.config.musicId),
                    observePlayerContentUseCase = koin.get(),
                    favoritesRepository = koin.get(),
                    playbackCoordinator = koin.get(),
                    onBack = { favoritesNavigation.pop() },
                ),
            )
        }

    private fun storiesChild(config: StoriesTabConfig, componentContext: ComponentContext): StoriesTabChild =
        when (config) {
            is StoriesTabConfig.Root -> StoriesTabChild.Root(
                DefaultStoriesComponent(
                    componentContext = componentContext,
                    observeStoriesContentUseCase = koin.get(),
                    playbackCoordinator = koin.get(),
                ),
            )
        }
}
