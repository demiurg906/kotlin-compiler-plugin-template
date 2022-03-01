package transformers

abstract class FirTransformer<D> {
    abstract fun <E : FirElement> transformElement(element: E, data: D): E

    open fun transformTypeRef(typeRef: FirTypeRef, data: D): FirTypeRef {
        return transformElement(typeRef, data)
    }

    open fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: D): FirTypeRef {
        return transformElement(userTypeRef, data)
    }

    open fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): FirTypeRef {
        return transformElement(resolvedTypeRef, data)
    }

    open fun transformUnresolvedTypeRef(unresolvedTypeRef: FirUnresolvedTypeRef, data: D): FirTypeRef {
        return transformElement(unresolvedTypeRef, data)
    }

    open fun transformExpression(expression: FirExpression, data: D): FirExpression {
        return transformElement(expression, data)
    }

    open fun transformBlock(block: FirBlock, data: D): FirBlock {
        return transformElement(block, data)
    }
}