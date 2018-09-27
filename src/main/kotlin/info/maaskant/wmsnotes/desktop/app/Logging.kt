package info.maaskant.wmsnotes.desktop.app

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
 * Source: https://amarszalek.net/blog/2018/05/13/logging-in-kotlin-right-approach/
 */
fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(getClassName(this.javaClass)) }
}
fun <T : Any> getClassName(clazz: Class<T>): String {
    return clazz.name.removeSuffix("\$Companion")
}