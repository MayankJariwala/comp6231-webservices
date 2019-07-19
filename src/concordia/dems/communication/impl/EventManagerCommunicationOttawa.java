package concordia.dems.communication.impl;

import concordia.dems.communication.IEventManagementCommunicationServer;
import concordia.dems.helpers.Constants;
import concordia.dems.helpers.EventOperation;
import concordia.dems.helpers.Helper;
import concordia.dems.model.enumeration.Servers;
import concordia.dems.servers.OttawaUDPClient;

import javax.jws.WebService;

/**
 * @author MayankJariwala
 * @version 1.0.0
 */
@WebService(endpointInterface = "concordia.dems.communication.IEventManagementCommunicationServer")
public class EventManagerCommunicationOttawa implements IEventManagementCommunicationServer {

    private OttawaUDPClient ottawaUDPClient = new OttawaUDPClient();

    /**
     * @param userRequest: Either Manager or Customer
     */
    @Override
    public String performOperation(String userRequest) {
        // Checking whether string is empty
        String verifyingRequestBody = userRequest.replaceAll(",", "");
        if (verifyingRequestBody.equals("")) {
            return "The request body is empty";
        }

        String[] unWrappingRequest = userRequest.split(",", 4);

        // If request is for event availability then simply returns all events in server
        if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.LIST_AVAILABILITY)) {
            return clearWhiteSpaces(getEventAvailabilityFromAllServers(userRequest));
        }

        // For getting booking schedule of user , also call all servers service
        if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.GET_BOOKING_SCHEDULE)) {
            return clearWhiteSpaces(getBookingScheduleForClients(userRequest));
        }

        // Swap Event Functionality[Assignment 2 Functionality]
        if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.SWAP_EVENT)) {
            return clearWhiteSpaces(this.swapEventForCustomer(unWrappingRequest));
        }

        switch (unWrappingRequest[Constants.TO_INDEX]) {
            case "montreal":
                if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.BOOK_EVENT)) {
                    return clearWhiteSpaces(this.sendBookEventMessageToMontreal(unWrappingRequest, userRequest));
                } else
                    return clearWhiteSpaces(ottawaUDPClient.sendMessageToMontrealUDP(userRequest));
            case "toronto":
                if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.BOOK_EVENT)) {
                    return clearWhiteSpaces(this.sendBookEventMessageToToronto(unWrappingRequest, userRequest));
                } else
                    return clearWhiteSpaces(ottawaUDPClient.sendMessageToTorontoUDP(userRequest));
            case "ottawa":
                return clearWhiteSpaces(ottawaUDPClient.sendMessageToOttawaUDP(userRequest));

        }
        return "";
    }

    private String generateStringForUnwrappingRequest(String[] unWrappingRequest) {
        return String.join(",", unWrappingRequest[0], unWrappingRequest[1], unWrappingRequest[2], unWrappingRequest[3]);
    }

    private String getEventAvailabilityFromAllServers(String userRequest) {
        String torontoEvents = ottawaUDPClient.sendMessageToTorontoUDP(userRequest);
        String ottawaEvents = ottawaUDPClient.sendMessageToOttawaUDP(userRequest);
        String montrealEvents = ottawaUDPClient.sendMessageToMontrealUDP(userRequest);
        return String.join("\n", torontoEvents, ottawaEvents, montrealEvents);
    }

    private String getBookingScheduleForClients(String userRequest) {
        String torontoEvents = ottawaUDPClient.sendMessageToTorontoUDP(userRequest);
        String ottawaEvents = ottawaUDPClient.sendMessageToOttawaUDP(userRequest);
        String montrealEvents = ottawaUDPClient.sendMessageToMontrealUDP(userRequest);
        return String.join("\n", torontoEvents, ottawaEvents, montrealEvents);
    }

    private boolean isCustomerEligibleForBookingEvent(String[] unWrappingRequest) {
        String torontoEvents = ottawaUDPClient
                .sendMessageToTorontoUDP(generateStringForUnwrappingRequest(unWrappingRequest));
        String montrealEvents = ottawaUDPClient
                .sendMessageToMontrealUDP(generateStringForUnwrappingRequest(unWrappingRequest));
        return Helper.checkIfEqualMoreThanThree(torontoEvents, montrealEvents,
                unWrappingRequest[Constants.INFORMATION_INDEX]);
    }

    /**
     * This function help communication layer to pass message to toronto server
     *
     * @param unWrappingRequest Unwrapping Request
     * @param userRequest       Request of User
     * @return String
     */
    private String sendBookEventMessageToToronto(String[] unWrappingRequest, String userRequest) {
        unWrappingRequest[Constants.ACTION_INDEX] = EventOperation.GET_BOOKING_SCHEDULE;
        boolean isNotEligible = isCustomerEligibleForBookingEvent(unWrappingRequest);
        if (isNotEligible)
            return "Rejected - Limit Exceeded! You have been already registered for 3 events for a specific month";
        return ottawaUDPClient.sendMessageToTorontoUDP(userRequest);
    }

    /**
     * This function help communication layer to pass message to montreal server
     *
     * @param unWrappingRequest Unwrapping Request
     * @param userRequest       Request of User
     * @return String
     */
    private String sendBookEventMessageToMontreal(String[] unWrappingRequest, String userRequest) {
        unWrappingRequest[Constants.ACTION_INDEX] = EventOperation.GET_BOOKING_SCHEDULE;
        boolean isNotEligible = isCustomerEligibleForBookingEvent(unWrappingRequest);
        if (isNotEligible)
            return "Rejected - Limit Exceeded! You have been already registered for 3 events for a specific month";
        return ottawaUDPClient.sendMessageToMontrealUDP(userRequest);
    }

    /**
     * Swap Function initiator - Entire responsible to handle book and cancel event
     *
     * @param unWrappingRequest: UnMarshalled Request
     * @return String Response of Swap Operation
     */
    private String swapEventForCustomer(String[] unWrappingRequest) {
        String[] swapEventdata = unWrappingRequest[Constants.INFORMATION_INDEX].split(",");
        String customerId = swapEventdata[0];
        String newEventId = swapEventdata[1];
        String newEventType = swapEventdata[2];
        String addResponse, cancelResponse, responseStatus;
        // User Request for booking: montreal,montreal,Book
        // Event,MTLC1234,MTLA120319,SEMINAR
        addResponse = this.swapOperationBookEvent(unWrappingRequest, customerId, newEventId, newEventType);
        responseStatus = addResponse.split("-")[0].trim();
        if (responseStatus.equalsIgnoreCase("success")) {
            // Remove User From Existing Event
            String oldEventId = swapEventdata[3];
            // swapEventdata[4] = Old Event Type are ignore during cancellation process
            cancelResponse = this.swapOperationCancelEvent(unWrappingRequest, customerId, oldEventId);
            responseStatus = cancelResponse.split("-")[0].trim();
            if (responseStatus.equalsIgnoreCase("success")) {
                return String.join(" {} ", addResponse, cancelResponse);
            } else {
                cancelResponse = this.swapOperationCancelEvent(unWrappingRequest, customerId, newEventId);
                return "Swap Operation cannot be performed,because the cancellation process is rejected for event "
                        + oldEventId + " and the new registered event is rollback by server " + cancelResponse;
            }
        } else {
            return "Swap Operation cannot be performed, because the booking event rejected due to " + addResponse;
        }
    }

    /**
     * Function only responsible to perform book event operation during swap
     * operation
     *
     * @param unWrappingRequest
     * @param customerId
     * @param newEventId
     * @param newEventType
     * @return String Response from Server
     */
    private String swapOperationBookEvent(String[] unWrappingRequest, String customerId, String newEventId,
                                          String newEventType) {
        Servers server = Helper.getServerFromId(newEventId);
        String addResponse;
        String addUserRequest;
        if (server.equals(Servers.MONTREAL)) {
            addUserRequest = "ottawa,montreal," + EventOperation.BOOK_EVENT + "," + customerId + "," + newEventId + ","
                    + newEventType;
            addResponse = this.sendBookEventMessageToMontreal(unWrappingRequest, addUserRequest);
        } else if (server.equals(Servers.TORONTO)) {
            addUserRequest = "ottawa,toronto," + EventOperation.BOOK_EVENT + "," + customerId + "," + newEventId + ","
                    + newEventType;
            addResponse = this.sendBookEventMessageToToronto(unWrappingRequest, addUserRequest);
        } else {
            addUserRequest = "ottawa,ottawa," + EventOperation.BOOK_EVENT + "," + customerId + "," + newEventId + ","
                    + newEventType;
            addResponse = ottawaUDPClient.sendMessageToOttawaUDP(addUserRequest);
        }
        return addResponse;
    }

    /**
     * Function only responsible to perform cancel event operation during swap
     * operation
     *
     * @param unWrappingRequest
     * @param customerId
     * @return String Response from Server
     */
    private String swapOperationCancelEvent(String[] unWrappingRequest, String customerId, String oldEventId) {
        Servers server = Helper.getServerFromId(oldEventId);
        String cancelUserRequest, cancelResponse;
        if (server.equals(Servers.MONTREAL)) {
            cancelUserRequest = "ottawa,montreal," + EventOperation.CANCEL_EVENT + "," + customerId + "," + oldEventId;
            cancelResponse = ottawaUDPClient.sendMessageToMontrealUDP(cancelUserRequest);
        } else if (server.equals(Servers.TORONTO)) {
            cancelUserRequest = "ottawa,toronto," + EventOperation.CANCEL_EVENT + "," + customerId + "," + oldEventId;
            cancelResponse = ottawaUDPClient.sendMessageToTorontoUDP(cancelUserRequest);
        } else {
            cancelUserRequest = "ottawa,ottawa," + EventOperation.CANCEL_EVENT + "," + customerId + "," + oldEventId;
            cancelResponse = ottawaUDPClient.sendMessageToOttawaUDP(cancelUserRequest);
        }
        return cancelResponse;
    }

    // for resolving xml parsing error issue
    private String clearWhiteSpaces(String responseFromUDP) {
        return responseFromUDP.trim().replaceAll("[\\000]*", "");
    }
}
