package demo

import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import spock.lang.Specification

@Integration
class DependencyInjectionSpec extends Specification {

    HelperService helperService

    @OnceBefore
    void init() {
        assert helperService != null
    }

    void 'some test method'() {
        expect:
        helperService != null
    }
}
