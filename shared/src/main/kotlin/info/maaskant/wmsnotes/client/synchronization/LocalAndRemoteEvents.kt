package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

data class LocalAndRemoteEvents(val localEvents: List<Event>, val remoteEvents: List<Event>)