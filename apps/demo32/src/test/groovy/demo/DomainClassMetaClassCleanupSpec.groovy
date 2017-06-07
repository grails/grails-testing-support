package demo

import grails.testing.runtime.FreshRuntime
import grails.testing.gorm.DomainUnitTest
import spock.lang.Issue
import spock.lang.Specification

@FreshRuntime
class DomainClassMetaClassCleanupSpec extends Specification
    implements DomainUnitTest<Person> {

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
}
