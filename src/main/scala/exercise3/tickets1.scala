package exercise3
import akka.actor._
import akka.event.Logging
object TicketOfficeTest extends App {
    // bilheteira pode disponibilizar para venda uma quantidade adicional de bilhetes (n),
    // sendo m os já disponíveis para venda
    // podem comprar uma certa quantidade de bilhetes

    case class ToSell(n: Int)
    case class Buy(n: Int)

    // Testing the system
    val sys = akka.actor.ActorSystem("TicketSys")
    val ticketOffice = sys.actorOf(Props[SellerActor], "main-office")

    class SellerActor extends Actor {
    val log = Logging(context.system, this)
    // Initial state: no tickets (n=0)
    def receive: Actor.Receive = selling(0)
    
    // Selling state: n tickets available
    def selling(n: Int): Actor.Receive = {
        case ToSell(m) => 
            log.info(s"Received to sell message: $m")
            context.become(selling(n + m)) // adiciona m bilhetes à venda
        case Buy(m) => 
            log.info(s"Received buy message: $m")
            if (n >= m) context.become(selling(n - m)) // tem stock suficiente para vender m bilhetes
            else log.error(s"Not enough tickets to sell $m tickets")
        case "Bye" => 
            log.info("Received bye message, stopping actor")
            context.stop(self)
        case _ => 
            log.info("Received unknown message")
    }
    }

    ticketOffice ! ToSell(2000)

    for (x <- 0 until 101) ticketOffice ! Buy(20)

    println(s"Tried to buy many ${20*101} tickets.") 

    ticketOffice ! "Bye"
    Thread.sleep(3000)

    sys.terminate()
}