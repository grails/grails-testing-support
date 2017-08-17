package demo

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification


class JsonControllerSpec extends Specification implements ControllerUnitTest<JsonController> {

    def setup() {
    }

    def cleanup() {
    }

    void "test a json view is rendered"() {
        when:
        request.addHeader('Accept', 'application/json')
        controller.index()

        then:
        response.text == '{"foo":"bar"}'
    }
}
