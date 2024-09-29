package demo

import grails.testing.gorm.DomainUnitTest
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
@Ignore('Cannot invoke "org.grails.orm.hibernate.HibernateGormEnhancer.registerEntity(org.grails.datastore.mapping.model.PersistentEntity)" because "this.this$0.gormEnhancer" is null')
class PersonSpec extends Specification implements DomainUnitTest<Person> {

    @Shared int id

    void "test basic persistence mocking"() {
        setup:
        new Person(firstName: 'Robert', lastName: 'Fripp').save()
        new Person(firstName: 'Adrian', lastName: 'Belew').save()

        expect:
        Person.count() == 2
    }

    void "test domain instance"() {
        setup:
        id = System.identityHashCode(domain)

        expect:
        domain != null
        domain.hashCode() == id

        when:
        domain.firstName = 'Robert'

        then:
        domain.firstName == 'Robert'
    }

    void "test we get a new domain"() {
        expect:
        domain != null
        domain.firstName == null
        System.identityHashCode(domain) != id
    }
}
