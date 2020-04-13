package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

data class CompensatingAction(
        val compensatedLocalEvents: List<Event>,
        val compensatedRemoteEvents: List<Event>,
        val newLocalEvents: List<Event>,
        val newRemoteEvents: List<Event>
)