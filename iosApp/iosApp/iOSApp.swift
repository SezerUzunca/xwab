import SwiftUI
import Shared // Ensure Kotlin shared module is imported, name depends on Xcode setup (Shared/shared)

@main
struct iOSApp: App {
    init() {
        KoinInitIosKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}