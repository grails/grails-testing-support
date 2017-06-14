package demo

import grails.testing.spring.AutowiredTest
import org.junit.Before
import spock.lang.Specification

class AutowiredTestSpec extends Specification implements AutowiredTest {

    Closure doWithSpring() {{ ->
        helperService HelperService
    }}

    HelperService helperService

    void setup() {
        assert helperService != null
    }

    void 'some test method'() {
        expect:
        helperService != null
    }

    void 'some other test method'() {
        expect:
        helperService != null
    }
}
