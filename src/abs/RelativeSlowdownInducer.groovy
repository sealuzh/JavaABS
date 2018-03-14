package abs

@Grab(group="com.github.javaparser", module="javaparser-core", version="3.2.10")
import com.github.javaparser.*
import com.github.javaparser.ast.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.stmt.*

class RelativeSlowdownInducer extends VoidVisitorAdapter<Void> {

    def method
    def params
    def slowdown
    def methodsMatched
    def clazzes = new LinkedList()

    public RelativeSlowdownInducer(def method, def params, def slowdown) {
        this.method = method
        this.params = params
        this.slowdown = slowdown
        this.methodsMatched = 0
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {

      clazzes.addFirst(n)
      super.visit(n, arg)
      clazzes.removeFirst()

    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {

        if(methodMatches(n)) {
            insertRegression(n)
            this.methodsMatched++
        }

    }

    public boolean matchingSuccessful() {
      return this.methodsMatched == 1
    }

    private boolean methodMatches(MethodDeclaration method) {
        if(this.method != method.getName().getIdentifier()){
            return false
        }
        if(clazzes.size() != 1)
          return false
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

        // add declarations to beginning of method
        statements.add(0, JavaParser.parseStatement("long _ptc_beginNano = System.nanoTime();"))
        statements.add(1, JavaParser.parseStatement("long _ptc_endNano = 0;"))

        // add this new end block in front of every return
        def replacements = [:]
        statements.eachWithIndex{ statement, index ->
            if(statement instanceof ReturnStmt) {
                def calcBlock = createBlock()
                calcBlock.getStatements().add(statement)
                replacements[index] = calcBlock
            }
        }
        replacements.each{ idx, newblock ->
            statements.remove(idx)
            statements.add(idx, newblock)
        }

        // add calculation code also to the end of the method for void methods
        if(method.getType() instanceof VoidType)
            statements.add(createBlock())
    }

    def createBlock() {
        return JavaParser.parseBlock("""{
_ptc_endNano = System.nanoTime();
long _ptc_waitTime = (long)((_ptc_endNano - _ptc_beginNano) * ${this.slowdown});
_ptc_beginNano = System.nanoTime();
_ptc_endNano = System.nanoTime();
while((_ptc_endNano - _ptc_beginNano) < _ptc_waitTime) {
    _ptc_endNano = System.nanoTime();
}
}"""    )
    }

}
