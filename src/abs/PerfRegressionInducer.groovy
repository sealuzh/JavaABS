package abs

@Grab(group="com.github.javaparser", module="javaparser-core", version="3.2.10")
import com.github.javaparser.*
import com.github.javaparser.ast.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.stmt.*

public class PerfRegressionInducer extends EmptyRegressionInducer {

    def testFile
    def testMethod
    def testParams
    def relativeSlowdown

    def origBackup = ""

    public PerfRegressionInducer(def testFile, def testMethod, def testParams, def relativeSlowdown) {
        this.testFile = testFile
        this.testMethod = testMethod
        this.testParams = testParams
        this.relativeSlowdown = relativeSlowdown
    }

    public String doUpdate() {
        CompilationUnit cu = JavaParser.parse(new FileInputStream(testFile))
        this.origBackup = cu.toString()
        def inducer = new RelativeSlowdownInducer(testMethod, testParams, relativeSlowdown)
        inducer.visit(cu, null);
        // new AbsoluteSlowdownInducer(testMethod, testParams, 1000000000).visit(cu, null);
        // do a sanity check if we matched exactly one method
        if(!inducer.matchingSuccessful())
          throw new RuntimeException("Matching failed for file $testFile method $testMethod (${testParams.size()} params). " +
            "Expecting to match exactly once, but matched ${inducer.methodsMatched} methods.")
        def updatedContent = cu.toString()
        new File(testFile).write(updatedContent)
        return updatedContent
    }

    public void resetChanges() {
        new File(testFile).write(this.origBackup)
    }

}
