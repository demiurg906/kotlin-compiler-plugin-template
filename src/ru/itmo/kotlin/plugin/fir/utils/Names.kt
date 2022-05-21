package ru.itmo.kotlin.plugin.fir.utils

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object Names {
    private val segments = listOf("ru", "itmo", "kotlin", "plugin", "desuspender", "DeSuspend")

    val DESUSPENDER_PACKAGE_FQN = FqName.fromSegments(segments.dropLast(1))
    val CONCURRENT_PKG_FQN = FqName.fromSegments(listOf("java", "util", "concurrent"))
    val KOTLINX_COROUTINES_PKG_FQN = FqName.fromSegments(listOf("kotlinx", "coroutines"))

    val ANNOTATION_FQN = FqName.fromSegments(segments)
    val ASYNC_EXECUTOR_FQN = FqName.fromSegments(listOf("ru", "itmo", "kotlin", "plugin", "desuspender"))

    private val COROUTINE_SCOPE_NAME = Name.identifier("CoroutineScope")
    private val EXECUTORS_NAME = Name.identifier("Executors")
    private val EXECUTOR_SERVICE_NAME = Name.identifier("ExecutorService")
    val ANNOTATION_NAME = Name.identifier("DeSuspend")
    val SUBMIT_NAME = Name.identifier("submit")
    val RUN_BLOCKING_NAME = Name.identifier("runBlocking")
    val ASYNC_EXECUTOR_NAME = Name.identifier("internal_executor")
    val NEW_FIXED_THREAD_POOL_NAME = Name.identifier("newFixedThreadPool")

    val ANNOTATION_CLASS_ID = ClassId(DESUSPENDER_PACKAGE_FQN, ANNOTATION_NAME)
    val NOTHING_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Nothing"))
    val FUTURE_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, Name.identifier("Future"))
    val EXECUTORS_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, EXECUTORS_NAME)
    val EXECUTOR_SERVICE_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, EXECUTOR_SERVICE_NAME)
    val FUNCTION0_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Function0"))
    val SUSPEND_FUNCTION_CLASS_ID =
        ClassId(FqName.fromSegments(listOf("kotlin", "coroutines")), Name.identifier("SuspendFunction1"))
    val COROUTINE_SCOPE_CLASS_ID = ClassId(KOTLINX_COROUTINES_PKG_FQN, COROUTINE_SCOPE_NAME)

    val ASYNC_EXECUTOR_CALLABLE_ID = CallableId(
        packageName = ASYNC_EXECUTOR_FQN,
        callableName = ASYNC_EXECUTOR_NAME
    )

    fun Name.toAsync() = Name.identifier("${this.asString()}Async")
}
