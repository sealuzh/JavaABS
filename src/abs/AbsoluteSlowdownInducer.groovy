package abs

@Grab(group="com.github.javaparser", module="javaparser-core", version="3.1.2")
import com.github.javaparser.*
import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.*

class AbsoluteSlowdownInducer extends VoidVisitorAdapter<Void> {

    def method
    def params
    def slowdown

    public AbsoluteSlowdownInducer(def method, def params, def absSlowdown) {
        this.method = method
        this.params = params
        this.slowdown = absSlowdown
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {

        if(methodMatches(n)) {
            insertRegression(n)
        }

    }

    private boolean methodMatches(MethodDeclaration method) {
        if(this.method != method.getName().getIdentifier()){
            return false
        }
        if(this.params.size() != method.getParameters().size()) {
            return false
        }
        def mismatch = false
        this.params.eachWithIndex {param, idx ->
            def methodParam = method.getParameters()[idx].getType().toString()
            if(methodParam != param)
                mismatch = true
        }
        return !(mismatch)
    }

    private void insertRegression(MethodDeclaration method) {

        BlockStmt body = method.getBody().get()
        def statements = body.getStatements()

        // add slowdown to beginning of method
        statements.add(0, createBlock())
    }

    def createBlock() {
        return JavaParser.parseBlock("""{
long _ptc_waitTime = ${this.slowdown};
long _ptc_beginNano = System.nanoTime();
long _ptc_endNano = System.nanoTime();
while((_ptc_endNano - _ptc_beginNano) < _ptc_waitTime) {
    _ptc_endNano = System.nanoTime();
}
}"""    )
    }

}
