package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

// TODO: Delete this class and move its fields into Solution
data class CompensatingAction(
        val compensatedLocalEvents: List<Event>,
        val compensatedRemoteEvents: List<Event>,
        val newLocalEvents: List<Event>,
        val newRemoteEvents: List<Event>
)