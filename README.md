# WMS Notes <!-- [![Build Status](https://travis-ci.org/scheleaap/wmsnotes-desktop-java.svg?branch=master)](https://travis-ci.org/scheleaap/wmsnotes-desktop-java) -->

WMS Notes is a *hierarchical* or *tree-based* note-taking application.

Features:
* A hierarchy of notes and folders
* Synchronization across desktop and Android phone
* Note-taking in Markdown format
* Image attachments

The application consists of three parts:
* A [desktop application](desktop/README.md) for Linux, Windows and macOS
* An [Android app](https://github.com/scheleaap/wmsnotes-android)
* A [server application](server/README.md) for Linux, Windows and macOS

## Technical Details

WMS Notes has been used to try out several new approaches and technologies, including:
* Event sourcing
* Protobuf + gRPC
* Kotlin
