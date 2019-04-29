package info.maaskant.wmsnotes.model.note.policy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class TitleExtractorTest {
    @TestFactory
    fun test(): List<DynamicTest> {
        val items = listOf(
                "" to null,
                " \n\n " to null,
                "foo" to "foo",
                " foo " to "foo",
                "# foo" to "foo",
                "# foo   " to "foo",
                "#   foo   " to "foo",
                "\n\n# foo" to "foo",
                "  \n  \n# foo" to "foo"

                )
        return items.map { (content, title) ->
            DynamicTest.dynamicTest("content: '$content'") {
                assertThat(extractTitleFromContent(content)).isEqualTo(title)
            }
        }
    }
}