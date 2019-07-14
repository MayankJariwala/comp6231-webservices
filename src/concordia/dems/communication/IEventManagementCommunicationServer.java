package concordia.dems.communication;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style = Style.RPC)
public interface IEventManagementCommunicationServer {

    @WebMethod
    String performOperation(String userRequest);
}
