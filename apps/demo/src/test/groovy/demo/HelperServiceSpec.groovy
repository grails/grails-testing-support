package demo

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class HelperServiceSpec extends Specification implements ServiceUnitTest<HelperService> {

    void "test invoking method"() {
        expect:
        service.magicNumber == 42
    }
}
