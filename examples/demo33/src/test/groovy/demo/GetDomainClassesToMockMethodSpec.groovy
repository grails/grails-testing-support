package demo

import grails.testing.gorm.DataTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('Cannot invoke "org.grails.orm.hibernate.HibernateGormEnhancer.registerEntity(org.grails.datastore.mapping.model.PersistentEntity)" because "this.this$0.gormEnhancer" is null')
class GetDomainClassesToMockMethodSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        Person
    }

    void "test basic persistence mocking"() {
        setup:
        new Person(firstName: 'Robert', lastName: 'Fripp').save()
        new Person(firstName: 'Adrian', lastName: 'Belew').save()

        expect:
        Person.count() == 2
    }
}
