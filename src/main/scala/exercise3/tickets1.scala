package exercise3
import akka.actor._
import akka.event.Logging
import scala.io.StdIn
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

    // sistema de input de testes
    print("Enter initial stock: ")
    val initialStock = StdIn.readInt()
    print("Enter tickets to buy: ")
    val ticketsToBuy = StdIn.readInt()
    print("Enter number of requests: ")
    val numRequests = StdIn.readInt()
    val sys = akka.actor.ActorSystem("TicketSys")
    print("Starting...")
    val ticketOffice = sys.actorOf(Props[SellerActor](), "main-office")
    ticketOffice ! ToSell(initialStock)
    for (_ <- 0 until numRequests) ticketOffice ! Buy(ticketsToBuy)
    println(s"Tried to buy many ${ticketsToBuy*numRequests} tickets.")
    Thread.sleep(3000)
    ticketOffice ! Bye
    Thread.sleep(1000)
    sys.terminate()
}