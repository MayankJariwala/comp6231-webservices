package concordia.dems.client;

import concordia.dems.communication.IEventManagementCommunicationServer;
import concordia.dems.helpers.EventOperation;
import concordia.dems.model.WebServiceObjectFactory;
import concordia.dems.model.enumeration.EventBatch;
import concordia.dems.model.enumeration.EventType;
import concordia.dems.model.enumeration.Servers;

/**
 * This class is just to represent the multithreading demo
 *
 * @author MayankJariwala
 * @version 1.0.0
 */
public class MultithreadingDemo {

    private IEventManagementCommunicationServer iEventManagerCommunication;

    public static void main(String[] args) {
        MultithreadingDemo multithreadingDemo = new MultithreadingDemo();
        multithreadingDemo.executeThreadOperations();
    }

    private void executeThreadOperations() {
        iEventManagerCommunication = WebServiceObjectFactory.getInstance(Servers.MONTREAL);
        Runnable manager = () -> {
            String requestBody = "montreal,montreal," + EventOperation.ADD_EVENT + ",MTLA181019," + EventType.SEMINAR
                    + "," + EventBatch.AFTERNOON + "," + 2;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Added by Manager : " + response);
        };

        Runnable manager1AddEvent = () -> {
            String requestBody = "toronto,toronto," + EventOperation.ADD_EVENT + ",TORA120519," + EventType.CONFERENCE
                    + "," + EventBatch.AFTERNOON + "," + 2;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Added by Toronto Manager : " + response);
        };

        Runnable manager1 = () -> {
            String requestBody = "montreal,montreal," + EventOperation.REMOVE_EVENT + ",MTLA181019,"
                    + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Removed by Manager : " + response);
        };

        Runnable client1 = () -> {
            String requestBody = "montreal,montreal," + EventOperation.BOOK_EVENT + ",MTLC1234,MTLA181019,"
                    + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Booked by MTLC1234 [Client1] : " + response);
        };

        Runnable client2 = () -> {
            String requestBody = "montreal,montreal," + EventOperation.BOOK_EVENT + ",MTLC2234,MTLA181019,"
                    + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Booked by MTLC2234 [Client2] : " + response);
        };

        Runnable client3 = () -> {
            String requestBody = "montreal,montreal," + EventOperation.BOOK_EVENT + ",MTLC3234,MTLA181019,"
                    + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Booked by MTLC3234 [Client3] : " + response);
        };

        Runnable client4 = () -> {
            String requestBody = "montreal,montreal," + EventOperation.BOOK_EVENT + ",MTLC9234,MTLA181019,"
                    + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Booked by MTLC9234 [Client4] : " + response);
        };

        Runnable client5 = () -> {
            String requestBody = "montreal,montreal," + EventOperation.BOOK_EVENT + ",MTLC7234,MTLA181019,"
                    + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for Event Booked by MTLC7234 [Client5] : " + response);
        };

        Runnable client5Swap = () -> {
            String requestBody = "montreal,," + EventOperation.SWAP_EVENT + ",MTLC7234,MTLA181019," + EventType.SEMINAR
                    + ",TORA120519," + EventType.CONFERENCE + "," + EventType.SEMINAR;
            String response = iEventManagerCommunication.performOperation(requestBody);
            System.out.println("Response for SwapEvent by MTLC7234 [Client5] : " + response);
        };

        // Start all threads
        Thread thread = new Thread(manager);
        Thread threadManager = new Thread(manager1);
        Thread torontoManager = new Thread(manager1AddEvent);
        Thread thread1 = new Thread(client1);
        Thread thread2 = new Thread(client2);
        Thread thread3 = new Thread(client3);
        Thread thread4 = new Thread(client4);
        Thread thread5 = new Thread(client5);
        Thread torontoManagerThread = new Thread(torontoManager);
        Thread thread5Swap = new Thread(client5Swap);
        thread.start();
        threadManager.start();
        torontoManagerThread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
        thread5Swap.start();
    }
}
