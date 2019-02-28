# WMS Notes [![Build Status](https://travis-ci.org/scheleaap/wmsnotes-desktop-and-server.svg?branch=master)](https://travis-ci.org/scheleaap/wmsnotes-desktop-and-server)

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
* Event sourcing and CQRS
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

The Markdown editing and previewing panes have been gratefully taken from [Markdown Writer FX](https://github.com/JFormDesigner/markdown-writer-fx).

## Event Sourcing anc CQRS

Here are some good articles on event sourcing:
* Design of an CQRS/ES system: https://hackernoon.com/1-year-of-event-sourcing-and-cqrs-fb9033ccd1c6 (contains links to other good resources as well)
* On dealing with concurrent updates: https://medium.com/@teivah/event-sourcing-and-concurrent-updates-32354ec26a4c
