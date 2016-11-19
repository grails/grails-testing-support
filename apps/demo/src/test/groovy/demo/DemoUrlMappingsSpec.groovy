package demo

import grails.testing.web.UrlMappingTest
import org.junit.Test

class DemoUrlMappingsSpec implements UrlMappingTest<DemoUrlMappings> {

    Class[] getControllersToMock() {
        DemoController
    }

    @Test
    void testUrlMapping() {
        assertUrlMapping '/sayHello', controller: 'demo', action: 'hello'
    }
}
