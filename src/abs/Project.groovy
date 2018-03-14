package abs

import groovy.io.FileType

/**
 * Created by christophlaaber on 08/03/17.
 */

class Project {
    static excludedEndings = ["javadoc.jar", "sources.jar", "tests.jar"]

    static Iterable<File> jars(String baseUrl, String jarPath, String jarFileNamePattern) {
        if (jarPath == null) {
            jarPath = ""
        }
        def rets = []
        def url = baseUrl + jarPath
        def dir = new File(url)
        dir.eachFileRecurse(FileType.FILES) { file ->
            def n = file.name
            if (!n.endsWith(".jar")) {
                return false
            }
            def endingOccur = excludedEndings.findIndexOf { String el ->
                def l = n.length()
                def begin = l - el.length()
                if (begin < 0) {
                    return false
                }
                def ending = n.substring(begin, l)
                return el == ending
            }

            def patternMatch = n.matches(jarFileNamePattern)
            def found = endingOccur == -1 && patternMatch
            if (found) {
                rets.add(file)
            }
        }
        return rets
    }
}