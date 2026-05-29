package exercise3
import akka.actor._
import akka.event.Logging
object TicketOfficeTest extends App {
    // bilheteira pode disponibilizar para venda uma quantidade adicional de bilhetes (n),
    // sendo m os já disponíveis para venda
    // podem comprar uma certa quantidade de bilhetes

    case class ToSell(n: Int)
    case class Buy(n: Int)
    case object Bye

    class SellerActor extends Actor {
        val log = Logging(context.system, this)
        // estado inicial: 0 bilhetes disponíveis para venda
        def receive: Actor.Receive = selling(0)
        
        // estado de venda, com n bilhetes disponíveis para venda
        def selling(n: Int): Actor.Receive = {
            case ToSell(m) => 
                val newStock = n + m
                log.info(s"Received to sell message: $m")
                context.become(selling(newStock)) // adicionar m bilhetes à venda
            case Buy(m) => 
                val remainingStock = n - m
                log.info(s"Received buy message: $m")
                if (n >= m) context.become(selling(remainingStock)) // tem bilhetes restantes > 0
                else log.error(s"Not enough tickets to sell $m tickets")
            case Bye => 
                log.info("Received bye message, stopping actor")
                context.stop(self)
            case _ => 
                log.info("Received unknown message")
        }
    }

    // testar
    val sys = akka.actor.ActorSystem("TicketSys")
    val ticketOffice = sys.actorOf(Props[SellerActor](), "main-office")

    ticketOffice ! ToSell(2000)

    for (_ <- 0 until 101) ticketOffice ! Buy(20)

    println(s"Tried to buy many ${20*101} tickets.") 

    ticketOffice ! Bye
    Thread.sleep(3000)

    sys.terminate()
}