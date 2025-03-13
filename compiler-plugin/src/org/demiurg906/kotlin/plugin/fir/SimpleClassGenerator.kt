package org.demiurg906.kotlin.plugin.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.*

/*
 * Generates top level class
 *
 * public final class foo.bar.MyClass {
 *     fun foo(): String = "Hello world"
 * }
 */
class SimpleClassGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        val MY_CLASS_ID = ClassId(FqName.fromSegments(listOf("foo", "bar")), Name.identifier("MyClass"))

        val FOO_ID = CallableId(MY_CLASS_ID, Name.identifier("foo"))
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId != MY_CLASS_ID) return null
        val klass = createTopLevelClass(MY_CLASS_ID, Key)
        return klass.symbol
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classId = context.owner.classId
        require(classId == MY_CLASS_ID)
        val constructor = createConstructor(context.owner, Key, /*generateDelegatedNoArgConstructorCall = true*/)
        return listOf(constructor.symbol)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val function = createMemberFunction(owner, Key, callableId.callableName, returnType = session.builtinTypes.stringType.type)
        return listOf(function.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return if (classSymbol.classId == MY_CLASS_ID) {
            setOf(FOO_ID.callableName, SpecialNames.INIT)
        } else {
            emptySet()
        }
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelClassIds(): Set<ClassId> {
        return setOf(MY_CLASS_ID)
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == MY_CLASS_ID.packageFqName
    }

    object Key : GeneratedDeclarationKey()
}
