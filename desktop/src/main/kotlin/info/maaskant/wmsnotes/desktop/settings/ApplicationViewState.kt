package info.maaskant.wmsnotes.desktop.settings

import io.reactivex.subjects.BehaviorSubject
import org.springframework.stereotype.Component

@Component
class ApplicationViewState {
    // TODO: Use serialized subjects (call `toSerialized()`)
    val showWhitespace: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    val showLineNumbers: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    fun toggleShowLineNumbers() {
        showLineNumbers.onNext(!showLineNumbers.value!!)
    }

    fun toggleShowWhitespace() {
        showWhitespace.onNext(!showWhitespace.value!!)
    }
}
