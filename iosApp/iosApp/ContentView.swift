import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    let runtime: AppRuntime

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(runtime: runtime)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    let runtime: AppRuntime

    var body: some View {
        ComposeView(runtime: runtime)
            .ignoresSafeArea()
    }
}
