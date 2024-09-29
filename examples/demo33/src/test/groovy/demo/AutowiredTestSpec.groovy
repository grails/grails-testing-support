package demo

// tag::test_declaration[]
import grails.testing.spring.AutowiredTest
import spock.lang.Ignore
import spock.lang.PendingFeature
import spock.lang.Specification

@Ignore('helperService is null')
class AutowiredTestSpec extends Specification implements AutowiredTest {

    Closure doWithSpring() {{ ->
        helperService HelperService
    }}

    HelperService helperService

    void setup() {
        assert helperService != null
    }

    @PendingFeature(reason = 'helperService is null')
    void 'some test method'() {
        expect:
        helperService != null
    }

    @PendingFeature(reason = 'helperService is null')
    void 'some other test method'() {
        expect:
        helperService != null
    }
}
// end::test_declaration[]