import dao.TicketDao;
import entity.Ticket;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class DaoRunner {
    public static void main(String[] args) {
        findAllTest();
    }

    private static void findAllTest() {
        List<Ticket> tickets = TicketDao.getInstance().findAll();
        for (Ticket ticket :
                tickets) {
            System.out.println(ticket);
        }
    }

    private static void findAndUpdateTest(Long id) {
        TicketDao ticketDao = TicketDao.getInstance();

        Optional<Ticket> maybeTicket = ticketDao.findById(id);

        System.out.println(maybeTicket);

        // Если maybeTicket существует (не Optional.empty), то обновляем cost:
        maybeTicket.ifPresent(ticket -> {
            ticket.setCost(BigDecimal.valueOf(188.88));
            ticketDao.update(ticket);
        });
    }

    private static void deleteTest(Long id) {
        TicketDao ticketDao = TicketDao.getInstance();

        boolean deleteResult = ticketDao.delete(id);
        System.out.println(deleteResult);
    }

    private static void saveTest(
            String passengerNo,
            String passengerName,
            Long flightId,
            String seatNo,
            BigDecimal cost
    ) {
        TicketDao ticketDao = TicketDao.getInstance();

        Ticket ticket = new Ticket();
        ticket.setPassengerNo(passengerNo);
        ticket.setPassengerName(passengerName);
        ticket.setFlightId(flightId);
        ticket.setSeatNo(seatNo);
        ticket.setCost(cost);

        Ticket saveTicket = ticketDao.save(ticket);
        System.out.println(saveTicket);
    }
}
