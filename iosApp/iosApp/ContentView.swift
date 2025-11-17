import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        // Ensure portrait only
        UIDevice.current.setValue(UIInterfaceOrientation.portrait.rawValue, forKey: "orientation")
        UINavigationController.attemptRotationToDeviceOrientation()
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onAppear {
                // Lock to portrait when view appears
                AppDelegate.orientationLock = .portrait
            }
    }
}



