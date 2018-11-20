package demo

import grails.gorm.services.Service

@Service(Car)
class CarService{
    List<Car> list(){
        Car.list()
    }
}
