package ru.itmo.kotlin.plugin.fir.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import ru.itmo.kotlin.plugin.fir.utils.Names.ASYNC_EXECUTOR_FQN
import ru.itmo.kotlin.plugin.fir.utils.Names.ASYNC_EXECUTOR_NAME
import ru.itmo.kotlin.plugin.fir.utils.Names.EXECUTORS_CLASS_ID
import ru.itmo.kotlin.plugin.fir.utils.Names.EXECUTOR_SERVICE_CLASS_ID
import ru.itmo.kotlin.plugin.fir.utils.Names.KOTLINX_COROUTINES_PKG_FQN
import ru.itmo.kotlin.plugin.fir.utils.Names.NEW_FIXED_THREAD_POOL_NAME
import ru.itmo.kotlin.plugin.fir.utils.Names.RUN_BLOCKING_NAME
import ru.itmo.kotlin.plugin.fir.utils.Names.SUBMIT_NAME

object Finder {
    fun findRunBlockingSymbol(session: FirSession): FirNamedFunctionSymbol {
        return session.symbolProvider.getTopLevelFunctionSymbols(KOTLINX_COROUTINES_PKG_FQN, RUN_BLOCKING_NAME).first()
    }

    fun findExecutorsSymbol(session: FirSession): FirRegularClassSymbol {
        return session.symbolProvider.getClassLikeSymbolByClassId(EXECUTORS_CLASS_ID) as FirRegularClassSymbol
    }

    @OptIn(SymbolInternals::class)
    private fun getExecutorsScope(session: FirSession): FirContainingNamesAwareScope {
        val classSymbol = findExecutorsSymbol(session)
        val myScopeSession = ScopeSession()
        val sessionHolder = object : SessionHolder {
            override val session: FirSession
                get() = session
            override val scopeSession: ScopeSession
                get() = myScopeSession
        }
        return classSymbol.fir.staticScope(sessionHolder)!!
    }

    private fun findExecutorServiceSymbol(session: FirSession): FirRegularClassSymbol {
        return session.symbolProvider.getClassLikeSymbolByClassId(EXECUTOR_SERVICE_CLASS_ID) as FirRegularClassSymbol
    }

    private fun getExecutorServiceScope(session: FirSession): FirTypeScope {
        val classSymbol = findExecutorServiceSymbol(session)
        return classSymbol.unsubstitutedScope(session, ScopeSession(), false)
    }

    fun findFixedThreadPoolSymbol(session: FirSession): FirNamedFunctionSymbol {
        return getExecutorsScope(session).getFunctions(NEW_FIXED_THREAD_POOL_NAME).find {
            it.valueParameterSymbols.count() == 1
        }!!
    }

    fun findSubmitSymbol(session: FirSession): FirNamedFunctionSymbol {
        return getExecutorServiceScope(session).getFunctions(SUBMIT_NAME).first()
    }

    fun findInternalThreadPoolSymbol(session: FirSession): FirPropertySymbol {
        return session.symbolProvider.getTopLevelCallableSymbols(ASYNC_EXECUTOR_FQN, ASYNC_EXECUTOR_NAME).first() as FirPropertySymbol
    }
}