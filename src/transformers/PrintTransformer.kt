import transformers.*

class PrintTransformer(val builder: StringBuilder) : FirTransformer<Int>() {
    override fun <E : FirElement> transformElement(element: E, data: Int): E {
        error("Should not be here")
    }

    override fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: Int): FirTypeRef {
        printTab(data)
        builder.appendLine("User type: ${userTypeRef.typeLiteral}")
        return userTypeRef
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Int): FirTypeRef {
        printTab(data)
        builder.appendLine("Resolved type: ${resolvedTypeRef.clazz}")
        return resolvedTypeRef
    }

    override fun transformUnresolvedTypeRef(unresolvedTypeRef: FirUnresolvedTypeRef, data: Int): FirTypeRef {
        printTab(data)
        builder.appendLine("Unresolved type: ${unresolvedTypeRef.unresolvedName}")
        return unresolvedTypeRef
    }

    override fun transformExpression(expression: FirExpression, data: Int): FirExpression {
        printTab(data)
        builder.appendLine("Expression:")
        expression.transformChildren(this, data + 1)
        return expression
    }

    override fun transformBlock(block: FirBlock, data: Int): FirBlock {
        printTab(data)
        builder.appendLine("Block:")
        block.transformChildren(this, data + 1)
        return block
    }

    private fun printTab(offset: Int) {
        builder.append(" | ")
        builder.append("  ".repeat(offset))
    }
}

fun FirElement.render(): String = buildString {
    transform<FirElement, Int>(PrintTransformer(this), 0)
}