package demo

// tag::basic_declaration[]
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class FirstTagLibSpec extends Specification implements TagLibUnitTest<FirstTagLib> {

    void setupSpec() {
        mockTagLib SecondTagLib
    }

    void "test invoking a tag which invokes a tag in another taglib"() {
        expect:
        tagLib.sayHello() == 'Hello From SecondTagLib'
    }
}
// end::basic_declaration[]
