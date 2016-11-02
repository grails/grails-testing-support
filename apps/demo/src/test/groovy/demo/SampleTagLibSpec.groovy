package demo

// tag::basic_declaration[]
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class SampleTagLibSpec extends Specification implements TagLibUnitTest<SampleTagLib> {

// end::basic_declaration[]
    // tag::test_simple_tag_as_method[]
    void "test simple tag as method"() {
        expect:
        tagLib.helloWorld() == 'Hello, World!'
    }
    // end::test_simple_tag_as_method[]
    // tag::test_simple_tag_with_applyTemplate[]
    void "test tags with applyTemplate"() {
        expect:
        applyTemplate('<demo:helloWorld/>') == 'Hello, World!'
        applyTemplate('<demo:sayHello name="Adrian"/>') == 'Hello, Adrian!'
    }
    // end::test_simple_tag_with_applyTemplate[]
    // tag::test_tag_as_method_with_parameters[]
    void "test tag as method with parameters"() {
        expect:
        tagLib.sayHello(name: 'Robert') == 'Hello, Robert!'
    }
    // end::test_tag_as_method_with_parameters[]
// tag::basic_declaration[]
}
// end::basic_declaration[]

