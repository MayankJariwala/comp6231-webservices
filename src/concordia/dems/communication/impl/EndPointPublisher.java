package concordia.dems.communication.impl;

import javax.xml.ws.Endpoint;

public class EndPointPublisher {

    public static void main(String[] args) {
        Endpoint.publish("http://127.0.0.1:8080/city/montreal", new EventManagerCommunicationMontreal());
        System.out.println("Montreal Published");
        Endpoint.publish("http://127.0.0.1:8081/city/ottawa", new EventManagerCommunicationOttawa());
        System.out.println("Ottawa Published");
        Endpoint.publish("http://127.0.0.1:8082/city/toronto", new EventManagerCommunicationToronto());
        System.out.println("Toronto Published");
    }
}
