package demo

import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Test

class DemoControllerTest implements ControllerUnitTest<DemoController> {

    @Test
    void "test action which renders text"() {
        controller.hello()

        assert status == 200
        assert response.text == 'Hello, World!'
    }

    @Test
    void 'test invalid request method'() {
        request.method = 'POST'
        controller.clearDatabase()

        assert status == 405
    }

    @Test
    void 'test valid request method'() {
        request.method = 'DELETE'
        controller.clearDatabase()

        assert status == 200
        assert response.text == 'Success'
    }
}
