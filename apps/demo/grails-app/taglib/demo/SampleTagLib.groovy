package demo

class SampleTagLib {
    static defaultEncodeAs = [taglib:'html']

    static namespace = 'demo'

    def sayHello = { attrs ->
        out << 'Hello, World!'
    }
}
