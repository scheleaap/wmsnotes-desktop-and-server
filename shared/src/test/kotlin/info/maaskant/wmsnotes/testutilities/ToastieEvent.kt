package info.maaskant.wmsnotes.testutilities

//import au.com.console.kassava.kotlinToString
import info.maaskant.wmsnotes.model.Event

sealed class ToastieEvent(eventId: Int, aggId: String, revision: Int) : Event(eventId, aggId, revision)

//class ToastieIncreaseValueEvent(eventId: Int, aggId: String, revision: Int) : ToastieEvent(eventId, aggId, revision) {
//    override fun copy(eventId: Int, revision: Int): ToastieIncreaseValueEvent =
//            ToastieIncreaseValueEvent(eventId = eventId, aggId = aggId, revision = revision)
//
//    override fun toString() = kotlinToString(properties = arrayOf(ToastieIncreaseValueEvent::eventId, ToastieIncreaseValueEvent::aggId, ToastieIncreaseValueEvent::revision))
//
//    override fun canEqual(other: Any?) = other is ToastieIncreaseValueEvent
//}
//
