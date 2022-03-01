import transformers.FirBlock
import transformers.FirExpression
import transformers.FirUserTypeRef

fun createBlock(): FirBlock = FirBlock(
    mutableListOf(
        FirExpression(
            FirUserTypeRef("java.lang.String")
        ),
        FirExpression(
            FirUserTypeRef("some.unresolved.class")
        )
    )
)

fun main() {
    var block = createBlock()
    println("------- before -------")
    println(block.render())

    val transformer = TypeResolveTransformer()
    block = block.transform(transformer, null)

    println("------- after -------")
    println(block.render())
}