package demo

// tag::basic_declaration[]
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class HelperServiceSpec extends Specification implements ServiceUnitTest<HelperService> {

    void "test retrieving a property"() {
        expect:
        service.magicNumber == 42
    }
}
// end::basic_declaration[]
