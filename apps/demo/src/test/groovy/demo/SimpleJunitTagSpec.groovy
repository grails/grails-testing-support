package demo

import grails.testing.web.taglib.TagLibUnitTest
import org.junit.Test

class SimpleJunitTagSpec implements TagLibUnitTest<SampleTagLib> {

    @Test
    void testSomethingSimple() {
        assert applyTemplate('<demo:helloWorld/>') == 'Hello, World!'
    }
}
