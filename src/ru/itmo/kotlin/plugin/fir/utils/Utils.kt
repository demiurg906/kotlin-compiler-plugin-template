package ru.itmo.kotlin.plugin.fir

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredFunctionSymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object Finder {
    fun findExecutorsSymbol(session: FirSession): FirClassLikeSymbol<*> {
        return session.symbolProvider.getClassLikeSymbolByClassId(Names.EXECUTORS_CLASS_ID)!!
    }

    @OptIn(SymbolInternals::class)
    fun findFixedThreadPoolSymbol(session: FirSession): FirNamedFunctionSymbol {
        return session.symbolProvider.getClassDeclaredFunctionSymbols(
            Names.EXECUTORS_CLASS_ID,
            Names.NEW_FIXED_THREAD_POOL_NAME
        ).find { it.fir.valueParameters.count() == 1 }!!
    }

    @OptIn(SymbolInternals::class)
    fun findSubmitSymbol(session: FirSession): FirNamedFunctionSymbol {
        return session.symbolProvider.getClassDeclaredFunctionSymbols(
            Names.EXECUTOR_SERVICE_CLASS_ID, Names.SUBMIT_NAME
        ).find { it.fir.valueParameters.count() == 1 && it.fir.typeParameters.isNotEmpty() }!!
    }

    fun findRunBlockingSymbol(session: FirSession): FirNamedFunctionSymbol {
        return session.symbolProvider.getTopLevelFunctionSymbols(
            FqName.fromSegments(listOf("kotlinx", "coroutines")), Names.RUN_BLOCKING_NAME
        )[0]
    }
}

object Names {
    private val segments = listOf("ru", "itmo", "kotlin", "plugin", "desuspender", "DeSuspend")
    val PACKAGE_FQN = FqName.fromSegments(segments.dropLast(1))
    val ANNOTATION_NAME = Name.identifier("DeSuspend")
    val ANNOTATION_FQN = FqName.fromSegments(segments)

    val CONCURRENT_PKG_FQN = FqName.fromSegments(listOf("java", "util", "concurrent"))
    val EXECUTOR_SERVICE_NAME = Name.identifier("ExecutorService")
    val EXECUTORS_NAME = Name.identifier("Executors")
    val SUBMIT_NAME = Name.identifier("submit")
    val SUBMIT_CALLABLE_ID = CallableId(CONCURRENT_PKG_FQN, FqName.topLevel(EXECUTOR_SERVICE_NAME), SUBMIT_NAME)
    val EXECUTOR_SERVICE_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, EXECUTOR_SERVICE_NAME)
    val FUTURE_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, Name.identifier("Future"))
    val EXECUTORS_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, EXECUTORS_NAME)
    val NEW_FIXED_THREAD_POOL_NAME = Name.identifier("newFixedThreadPool")
    val CALLABLE_CLASS_ID = ClassId(CONCURRENT_PKG_FQN, Name.identifier("Callable"))
    val NEW_FIXED_THREAD_POOL_CALLABLE_ID = CallableId(CONCURRENT_PKG_FQN, FqName("Executors"), NEW_FIXED_THREAD_POOL_NAME)

    val FUNCTION0_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Function0"))

    //val COROUTINES_PKG_FQN = FqName.fromSegments(listOf("kotlinx", "coroutines"))
    val RUN_BLOCKING_NAME = Name.identifier("runBlocking")
    val RUN_BLOCKING_CALLABLE_ID = CallableId(
        packageName = FqName.fromSegments(listOf("kotlinx", "coroutines")),
        callableName = RUN_BLOCKING_NAME
    )

    val ASYNC_EXECUTOR_FQN = FqName.fromSegments(listOf("ru", "itmo", "kotlin", "plugin", "desuspender"))
    val ASYNC_EXECUTOR_NAME = Name.identifier("internal_executor")
    val ASYNC_EXECUTOR_CALLABLE_ID = CallableId(
        packageName = ASYNC_EXECUTOR_FQN,
        callableName = ASYNC_EXECUTOR_NAME
    )

    fun Name.toAsync() = Name.identifier("${this.asString()}Async")
}
