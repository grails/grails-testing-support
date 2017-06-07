package demo

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class DefineBeansSpec extends Specification implements ServiceUnitTest<ReportingService> {

    void "test dependency injection with defineBeans"() {
        given:
        defineBeans {
            someHelper RushHelper
        }

        expect:
        getService().retrieveSomeNumber() == 2112
    }
}
