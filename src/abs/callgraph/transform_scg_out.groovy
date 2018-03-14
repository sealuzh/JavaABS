package abs.callgraph

import groovy.json.JsonSlurper

/**
 * Created by christophlaaber on 12/04/17.
 */

def input = args[0]
def output = args[1]

def js = new JsonSlurper()
def parsedIn = js.parse(new File(input))

def outFile = new File(output)

def methodsFoundBy = new HashMap<String, List<String>>()

// transform
parsedIn.methods.each { k, v ->
    if (v == null || v.isEmpty()) {
        return
    }

    v.each { m ->
        def mStr = new StringBuilder()
        mStr.append(m.className)
        mStr.append(":")
        mStr.append(m.method)
        mStr.append("(")
        def first = true
        m.params.each { p ->
            if (!first) {
                mStr.append(",")
                first = false
            }
            mStr.append(p)
        }
        mStr.append(")")
        def mstr = mStr.toString()

        def list = methodsFoundBy.get(mstr)
        if (list == null) {
            methodsFoundBy[mstr] = new ArrayList<>()
        }
        methodsFoundBy[mstr].add(k)
    }
}


// print and save
methodsFoundBy.each { k, v ->
    def mStr = "##### $k #####"
    println(mStr)
    outFile.append("$mStr\n")
    v.each { b ->
        println(b)
        outFile.append("$b\n")
    }
}


