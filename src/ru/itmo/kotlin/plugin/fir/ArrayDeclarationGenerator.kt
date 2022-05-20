package ru.itmo.kotlin.plugin.fir

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeAlias
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeConflictingProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

class ArrayDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        const val maxArrayAlias = 10
        const val prefix = "Array"
        val packageName = FqName("foo.bar")
    }

    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*> {
        val level = classId.shortClassName.identifier.replace(prefix, "").toInt()
        val aliasSymbol = FirTypeAliasSymbol(classId)
        val typeParameter = buildTypeParameter {
            moduleData = session.moduleData
            origin = Key.origin
            symbol = FirTypeParameterSymbol()
            variance = Variance.INVARIANT
            isReified = false
            containingDeclarationSymbol = aliasSymbol
            name = Name.identifier("T")
        }
        val typeArgument = if (level == 1) {
                typeParameter.toConeType()
            } else {
                ClassId(packageName, Name.identifier("$prefix${level - 1}")).toConeType(arrayOf(typeParameter.toConeType()))
            }
        val alias = buildTypeAlias {
            moduleData = session.moduleData
            origin = Key.origin
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
            expandedTypeRef = buildResolvedTypeRef {
                val arrayClassId = ClassId(StandardClassIds.Array.packageFqName, Name.identifier(StandardClassIds.Array.shortClassName.identifier))
                type = arrayClassId.toConeType(arrayOf(typeArgument))
            }
            name = classId.shortClassName
            symbol = aliasSymbol
            typeParameters.add(typeParameter)
        }
        return alias.symbol
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
        return (1..maxArrayAlias).map {
            ClassId(packageName, Name.identifier("$prefix$it"))
        }.toSet()
    }

    object Key : FirPluginKey() {
        override fun toString(): String {
            return "ArrayDeclarationGenerator"
        }
    }
}
