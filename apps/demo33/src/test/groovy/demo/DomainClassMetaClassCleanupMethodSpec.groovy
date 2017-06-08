package demo

import grails.testing.gorm.DomainUnitTest
import grails.testing.runtime.FreshRuntime
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class DomainClassMetaClassCleanupMethodSpec extends Specification
        implements DomainUnitTest<Person> {

    @FreshRuntime
    @Issue('GRAILS-11661')
    void 'test adding one method'() {
        when:
        Person.metaClass.static.one = {}

        then:
        Person.metaClass.hasMetaMethod('one')
        !Person.metaClass.hasMetaMethod('two')
    }

    @Issue('GRAILS-11661')
    void 'test adding another method'() {
        when:
        Person.metaClass.static.two = {}

        then:
        !Person.metaClass.hasMetaMethod('one')
        Person.metaClass.hasMetaMethod('two')
    }

    void "test the method two still exists"() {
        expect:
        Person.metaClass.hasMetaMethod('two')
    }
}

