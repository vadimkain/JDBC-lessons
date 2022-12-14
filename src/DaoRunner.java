import dao.TicketDao;
import entity.Ticket;

import java.math.BigDecimal;

public class DaoRunner {
    public static void main(String[] args) {

    }

    private static void deleteTest() {
        TicketDao ticketDao = TicketDao.getInstance();

        boolean deleteResult = ticketDao.delete(59L);
        System.out.println(deleteResult);
    }

    private static void saveTest() {
        TicketDao ticketDao = TicketDao.getInstance();

        Ticket ticket = new Ticket();
        ticket.setPassengerNo("1234567");
        ticket.setPassengerName("Test");
        ticket.setFlightId(3L);
        ticket.setSeatNo("B3");
        ticket.setCost(BigDecimal.TEN);

        Ticket saveTicket = ticketDao.save(ticket);
        System.out.println(saveTicket);
    }
}
