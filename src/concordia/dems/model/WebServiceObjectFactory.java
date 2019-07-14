package concordia.dems.model;

import concordia.dems.communication.IEventManagementCommunicationServer;
import concordia.dems.model.enumeration.Servers;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

/**
 * This RMI Server factory create an instance of registry of rmi server of
 * dedicated city
 *
 * @author Mayank Jariwala
 * @version 1.0.0
 */
public class WebServiceObjectFactory {

    private WebServiceObjectFactory() {

    }

    public static IEventManagementCommunicationServer getInstance(Servers servers) {
        IEventManagementCommunicationServer communication = null;
        try {
            switch (servers) {
                case MONTREAL:
                    URL montrealUrl = new URL("http://127.0.0.1:8080/city/montreal?wsdl");
                    QName montrealQName = new QName("http://impl.communication.dems.concordia/", "EventManagerCommunicationMontrealService");
                    Service montrealService = Service.create(montrealUrl, montrealQName);
                    communication = montrealService.getPort(IEventManagementCommunicationServer.class);
                    break;
                case TORONTO:
                    URL torontoUrl = new URL("http://127.0.0.1:8082/city/toronto?wsdl");
                    QName torontoQName = new QName("http://impl.communication.dems.concordia/", "EventManagerCommunicationTorontoService");
                    Service torontoService = Service.create(torontoUrl, torontoQName);
                    communication = torontoService.getPort(IEventManagementCommunicationServer.class);
                    break;
                case OTTAWA:
                    URL ottawaUrl = new URL("http://127.0.0.1:8081/city/ottawa?wsdl");
                    QName ottawaQName = new QName("http://impl.communication.dems.concordia/", "EventManagerCommunicationOttawaService");
                    Service ottawaService = Service.create(ottawaUrl, ottawaQName);
                    communication = ottawaService.getPort(IEventManagementCommunicationServer.class);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println("Customer Exception: " + e);
            e.printStackTrace();
        }
        return communication;
    }
}
