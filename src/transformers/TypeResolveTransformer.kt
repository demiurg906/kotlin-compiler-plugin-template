import transformers.*

class TypeResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        element.transformChildren(this, data)
        return element
    }

    override fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: Nothing?): FirTypeRef {
        return try {
            val kClass = Class.forName(userTypeRef.typeLiteral)
            FirResolvedTypeRef(kClass)
        } catch (e: ClassNotFoundException) {
            FirUnresolvedTypeRef(userTypeRef.typeLiteral)
        }
    }
}