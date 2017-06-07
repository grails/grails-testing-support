package demo

import grails.testing.web.UrlMappingTest
import org.junit.Test
import spock.lang.Specification

class DemoUrlMappingsSpec extends Specification implements UrlMappingTest<DemoUrlMappings> {

    Class[] getControllersToMock() {
        DemoController
    }

    void testUrlMapping() {
        when:
        assertUrlMapping '/sayHello', controller: 'demo', action: 'hello'

        then:
        noExceptionThrown()
    }
}
