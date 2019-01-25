# WMS Notes <!-- [![Build Status](https://travis-ci.org/scheleaap/wmsnotes-desktop-java.svg?branch=master)](https://travis-ci.org/scheleaap/wmsnotes-desktop-java) -->

WMS Notes is a *hierarchical* or *tree-based* note-taking application.

Features:
* A hierarchy of notes and folders
* Synchronization across desktop and Android phone, enabling offline note editing
* Note-taking in Markdown format
* Image attachments

The application consists of three parts:
* A [desktop application](desktop/README.md) for Linux, Windows and macOS
* An [Android app](https://github.com/scheleaap/wmsnotes-android)
* A [server application](server/README.md) for Linux, Windows and macOS

## Technical Details

WMS Notes has been used to try out several approaches and technologies, including:
* Event sourcing
* [Reactive extensions](http://reactivex.io/) (specifically [RxJava](https://www.google.com/search?client=firefox-b&q=rxjava), [RxKotlin](https://github.com/ReactiveX/RxKotlin) and [RxAndroid](https://github.com/ReactiveX/RxAndroid))
* JavaFX (and the [TornadoFX](https://tornadofx.io/) framework for Kotlin)
* [Protobuf](https://developers.google.com/protocol-buffers/) + [gRPC](https://grpc.io/)
* Android development
* Kotlin

Besides the technologies mentioned above, the project also uses:
* Spring Boot
* [Kryo](https://github.com/EsotericSoftware/kryo) (serialization framework)
* JUnit 5
* [MockK](https://mockk.io/) (Kotlin mocking framework)
