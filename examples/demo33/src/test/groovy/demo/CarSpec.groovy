package demo

import grails.testing.gorm.DomainUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('Cannot invoke "org.grails.orm.hibernate.HibernateGormEnhancer.registerEntity(org.grails.datastore.mapping.model.PersistentEntity)" because "this.this$0.gormEnhancer" is null')
class CarSpec extends Specification implements DomainUnitTest<Car> {

    def setup() {
    }

    def cleanup() {
    }

    void "test basic persistence mocking"() {
        setup:
        new Car(name: 'grails car', color: 'green').save()
        new Car(name: 'gorm car').save()

        expect:
        Car.count() == 1
    }
}
