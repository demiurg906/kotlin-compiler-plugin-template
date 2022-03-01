package transformers

abstract class FirElement {
    abstract fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E
    abstract fun <D> transformChildren(transformer: FirTransformer<D>, data: D)
}

abstract class FirTypeRef : FirElement()

class FirUserTypeRef(val typeLiteral: String) : FirTypeRef() {
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E {
        return transformer.transformUserTypeRef(this, data) as E
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D) {}
}

class FirResolvedTypeRef(val clazz: Class<*>) : FirTypeRef() {
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E {
        return transformer.transformResolvedTypeRef(this, data) as E
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D) {}
}

class FirUnresolvedTypeRef(val unresolvedName: String) : FirTypeRef() {
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E {
        return transformer.transformUnresolvedTypeRef(this, data) as E
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D) {}
}

class FirExpression(var typeRef: FirTypeRef) : FirElement() {
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E {
        return transformer.transformExpression(this, data) as E
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D) {
        typeRef = typeRef.transform(transformer, data)
    }
}

class FirBlock(
    val expressions: MutableList<FirExpression>
) : FirElement() {
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E {
        return transformer.transformBlock(this, data) as E
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D) {
        expressions.transformInPlace(transformer, data)
    }
}

fun <E : FirElement, D> MutableList<E>.transformInPlace(transformer: FirTransformer<D>, data: D) {
    val iterator = listIterator()
    while (iterator.hasNext()) {
        val element = iterator.next()
        val newElement: E = element.transform(transformer, data)
        if (element !== newElement) {
            iterator.set(newElement)
        }
    }
}