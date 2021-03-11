package demo

import geb.spock.GebSpec
import grails.testing.mixin.integration.Integration

@Integration
class JsonControllerSpec extends GebSpec {

    void "test a json view is rendered"() {
        when:
        go '/json/index'

        then:
        downloadBytes() { HttpURLConnection connection ->
            connection.setRequestProperty('Accept', 'application/json')
        } == '{"foo":"bar"}'.bytes
    }
}
