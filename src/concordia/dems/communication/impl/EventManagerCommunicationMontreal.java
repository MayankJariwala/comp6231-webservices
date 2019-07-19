package concordia.dems.communication.impl;

import concordia.dems.communication.IEventManagementCommunicationServer;
import concordia.dems.helpers.Constants;
import concordia.dems.helpers.EventOperation;
import concordia.dems.helpers.Helper;
import concordia.dems.helpers.Logger;
import concordia.dems.model.enumeration.Servers;
import concordia.dems.servers.MontrealUDPClient;

import javax.jws.WebService;

@WebService(endpointInterface = "concordia.dems.communication.IEventManagementCommunicationServer")
public class EventManagerCommunicationMontreal implements IEventManagementCommunicationServer {

    private MontrealUDPClient montrealUDPClient = new MontrealUDPClient();

    /**
     * The one which is responsible to call business layer of montreal or to call an
     * UDP of other server
     *
     * @param userRequest: Either Manager or Customer
     */
    @Override
    public String performOperation(String userRequest) {
//         Checking whether string is empty
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
                String response = montrealUDPClient.sendMessageToMontrealUDP(userRequest);
                Logger.writeLogToFile("client", "2213232", userRequest, response, Constants.TIME_STAMP);
                return clearWhiteSpaces(response);
            case "toronto":
                if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.BOOK_EVENT)) {
                    return clearWhiteSpaces(this.sendBookEventMessageToToronto(unWrappingRequest, userRequest));
                } else
                    return clearWhiteSpaces(montrealUDPClient.sendMessageToTorontoUDP(userRequest));
            case "ottawa":
                if (unWrappingRequest[Constants.ACTION_INDEX].equals(EventOperation.BOOK_EVENT)) {
                    return clearWhiteSpaces(this.sendBookEventMessageToOttawa(unWrappingRequest, userRequest));
                } else
                    return clearWhiteSpaces(montrealUDPClient.sendMessageToOttawaUDP(userRequest));
        }
        return "";
    }

    private String generateStringForUnwrappingRequest(String[] unWrappingRequest) {
        return String.join(",", unWrappingRequest[0], unWrappingRequest[1], unWrappingRequest[2], unWrappingRequest[3]);
    }

    private String getEventAvailabilityFromAllServers(String userRequest) {
        String torontoEvents = montrealUDPClient.sendMessageToTorontoUDP(userRequest);
        String ottawaEvents = montrealUDPClient.sendMessageToOttawaUDP(userRequest);
        String montrealEvents = montrealUDPClient.sendMessageToMontrealUDP(userRequest);
        return String.join("\n", torontoEvents.equals("") ? "No toronto Events" : torontoEvents,
                ottawaEvents.equals("") ? "No Ottawa Events" : ottawaEvents,
                montrealEvents.equals("") ? "No Montreal Events" : montrealEvents);
    }

    private String getBookingScheduleForClients(String userRequest) {
        String torontoEventsSchedule = montrealUDPClient.sendMessageToTorontoUDP(userRequest);
        String ottawaEventsSchedule = montrealUDPClient.sendMessageToOttawaUDP(userRequest);
        String montrealEventsSchedule = montrealUDPClient.sendMessageToMontrealUDP(userRequest);
        return String.join("\n", torontoEventsSchedule.equals("") ? "No toronto schedule" : torontoEventsSchedule,
                ottawaEventsSchedule.equals("") ? "No Ottawa schedule" : ottawaEventsSchedule,
                montrealEventsSchedule.equals("") ? "No Montreal schedule" : montrealEventsSchedule);
    }

    private boolean isCustomerEligibleForBookingEvent(String[] unWrappingRequest) {
        String torontoEvents = montrealUDPClient
                .sendMessageToTorontoUDP(generateStringForUnwrappingRequest(unWrappingRequest));
        String ottawaEvents = montrealUDPClient
                .sendMessageToOttawaUDP(generateStringForUnwrappingRequest(unWrappingRequest));
        return Helper.checkIfEqualMoreThanThree(torontoEvents, ottawaEvents,
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
        return montrealUDPClient.sendMessageToTorontoUDP(userRequest);
    }

    /**
     * This function help communication layer to pass message to ottawa server
     *
     * @param unWrappingRequest Unwrapping Request
     * @param userRequest       Request of User
     * @return String
     */
    private String sendBookEventMessageToOttawa(String[] unWrappingRequest, String userRequest) {
        unWrappingRequest[Constants.ACTION_INDEX] = EventOperation.GET_BOOKING_SCHEDULE;
        boolean isNotEligible = isCustomerEligibleForBookingEvent(unWrappingRequest);
        if (isNotEligible)
            return "Rejected - Limit Exceeded! You have been already registered for 3 events for a specific month";
        return montrealUDPClient.sendMessageToOttawaUDP(userRequest);
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
                return addResponse + "||" + cancelResponse;
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
     * @param unWrappingRequest: Raw Request
     * @param customerId:        Customer ID
     * @param newEventId:        New Event ID
     * @param newEventType:      Event Type
     * @return String Response from Server
     */
    private String swapOperationBookEvent(String[] unWrappingRequest, String customerId, String newEventId,
                                          String newEventType) {
        Servers server = Helper.getServerFromId(newEventId);
        String addResponse;
        String addUserRequest;
        if (server.equals(Servers.OTTAWA)) {
            addUserRequest = "montreal,ottawa," + EventOperation.BOOK_EVENT + "," + customerId + "," + newEventId + ","
                    + newEventType;
            addResponse = this.sendBookEventMessageToOttawa(unWrappingRequest, addUserRequest);
        } else if (server.equals(Servers.TORONTO)) {
            addUserRequest = "montreal,toronto," + EventOperation.BOOK_EVENT + "," + customerId + "," + newEventId + ","
                    + newEventType;
            addResponse = this.sendBookEventMessageToToronto(unWrappingRequest, addUserRequest);
        } else {
            addUserRequest = "montreal,montreal," + EventOperation.BOOK_EVENT + "," + customerId + "," + newEventId
                    + "," + newEventType;
            addResponse = montrealUDPClient.sendMessageToMontrealUDP(addUserRequest);
        }
        return addResponse;
    }

    /**
     * Function only responsible to perform cancel event operation during swap
     * operation
     *
     * @param unWrappingRequest: Raw Request
     * @param customerId:        Customer ID
     * @return String Response from Server
     */
    private String swapOperationCancelEvent(String[] unWrappingRequest, String customerId, String oldEventId) {
        Servers server = Helper.getServerFromId(oldEventId);
        String cancelUserRequest, cancelResponse;
        if (server.equals(Servers.OTTAWA)) {
            cancelUserRequest = "montreal,ottawa," + EventOperation.CANCEL_EVENT + "," + customerId + "," + oldEventId;
            cancelResponse = montrealUDPClient.sendMessageToOttawaUDP(cancelUserRequest);
        } else if (server.equals(Servers.TORONTO)) {
            cancelUserRequest = "montreal,toronto," + EventOperation.CANCEL_EVENT + "," + customerId + "," + oldEventId;
            cancelResponse = montrealUDPClient.sendMessageToTorontoUDP(cancelUserRequest);
        } else {
            cancelUserRequest = "montreal,montreal," + EventOperation.CANCEL_EVENT + "," + customerId + ","
                    + oldEventId;
            cancelResponse = montrealUDPClient.sendMessageToMontrealUDP(cancelUserRequest);
        }
        return cancelResponse;
    }

    // for resolving xml parsing error issue
    private String clearWhiteSpaces(String responseFromUDP) {
        return responseFromUDP.trim().replaceAll("[\\000]*", "");
    }
}
