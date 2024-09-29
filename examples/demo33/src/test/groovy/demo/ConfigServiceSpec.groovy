package demo

import grails.testing.services.ServiceUnitTest
import spock.lang.PendingFeature
import spock.lang.Specification

class ConfigServiceSpec extends Specification implements ServiceUnitTest<ConfigService> {

    Closure doWithConfig() {{ config ->
        config['mc.allow.signup'] = true
    }}

    @PendingFeature(reason = 'isSignupAllowed() == false')
    def "singup is allowed if configuration parameter is set"() {
        expect:
        service.isSignupAllowed()
    }
}
