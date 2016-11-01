package demo

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class SampleTagLibSpec extends Specification implements TagLibUnitTest<SampleTagLib> {

    void "test simple tag"() {
        expect:
        applyTemplate('<demo:sayHello/>') == 'Hello, World!'
    }
}
