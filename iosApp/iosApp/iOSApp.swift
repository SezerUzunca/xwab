import SwiftUI
import Shared // Ensure Kotlin shared module is imported, name depends on Xcode setup (Shared/shared)

@main
struct iOSApp: App {
    /// The application graph, built once for the process as `MainApplication` builds it on
    /// Android. Every scene's view controller is made over this one.
    private let runtime = AppRuntimeKt.createAppRuntime()

    var body: some Scene {
        WindowGroup {
            ContentView(runtime: runtime)
        }
    }
}
