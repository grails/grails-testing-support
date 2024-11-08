package demo

import grails.plugin.geb.ContainerGebSpec
import grails.testing.mixin.integration.Integration

@Integration
class JsonControllerSpec extends ContainerGebSpec {

    void "test a json view is rendered"() {
        when:
        go '/json/index'

        then:
        downloadBytes() { HttpURLConnection connection ->
            connection.setRequestProperty('Accept', 'application/json')
        } == '{"foo":"bar"}'.bytes
    }
}
