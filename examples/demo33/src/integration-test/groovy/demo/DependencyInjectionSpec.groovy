package demo

import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import org.junit.Before
import spock.lang.Specification

@Integration
class DependencyInjectionSpec extends Specification {

    HelperService helperService

    @Before
    @RunOnce
    void init() {
        assert helperService != null
    }

    void 'some test method'() {
        expect:
        helperService != null
    }
}
