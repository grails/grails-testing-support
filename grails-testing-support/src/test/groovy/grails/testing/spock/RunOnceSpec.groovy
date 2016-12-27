package grails.testing.spock

import org.junit.Before
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class RunOnceSpec extends Specification {

    @Shared
    int setupSpecCounter = 0

    @Shared
    int setupCounter = 0

    @Shared
    int onceBeforeCounter = 0

    @Shared
    int anotherOnceBeforeCounter = 0

    @Shared
    int beforeCounter = 0

    void setupSpec() {
        setupSpecCounter++
    }

    void setup() {
        setupCounter++
    }

    @Before
    void someBeforeMethod() {
        beforeCounter++
    }

    @Before
    @RunOnce
    void someOnceBeforeMethod() {
        onceBeforeCounter++
    }

    @Before
    @RunOnce
    void someOtherOnceBeforeMethod() {
        anotherOnceBeforeCounter++
    }

    void 'first test'() {
        expect:
        setupSpecCounter == 1
        setupCounter == 1
        beforeCounter == 1
        onceBeforeCounter == 1
        anotherOnceBeforeCounter == 1
    }

    void 'second test'() {
        expect:
        setupSpecCounter == 1
        setupCounter == 2
        beforeCounter == 2
        onceBeforeCounter == 1
        anotherOnceBeforeCounter == 1
    }

    void 'third test'() {
        expect:
        setupSpecCounter == 1
        setupCounter == 3
        beforeCounter == 3
        onceBeforeCounter == 1
        anotherOnceBeforeCounter == 1
    }
}
