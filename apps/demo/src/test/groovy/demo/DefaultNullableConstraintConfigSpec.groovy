package demo

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class DefaultNullableConstraintConfigSpec extends Specification implements DomainUnitTest<Person> {

    static doWithConfig = { c ->
        c.grails.gorm.default.constraints = {
            '*'(nullable: true)
        }
    }

    void 'test default nullable'() {
        expect:
        new Person().validate()
    }
}
