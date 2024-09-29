package demo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('Cannot invoke "org.grails.orm.hibernate.HibernateGormEnhancer.registerEntity(org.grails.datastore.mapping.model.PersistentEntity)" because "this.this$0.gormEnhancer" is null')
class CarServiceSpec extends Specification implements ServiceUnitTest<CarService>, DataTest{

    void setupSpec() {
        mockDomain Car
    }

    void setup(){
        new Car(name: 'grails car', color: 'green').save(flush:true)
        new Car(name: 'gorm car').save(flush:true)
    }

    void 'test one'() {
        expect:
        service.list().size() == 1
    }
}
