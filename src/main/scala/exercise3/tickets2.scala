package exercise3
import akka.actor._
import akka.event.Logging
import scala.io.StdIn
// n > 100 then child Actor is created + send fixed number of tickets to sell (< 50)
// child actors report to parent once they fail to have enough tickets to sell
// returning the possible remaining tickets to sell
object TicketOfficeChild extends App {
    // bilheteira pode disponibilizar para venda uma quantidade adicional de bilhetes (n),
    // sendo m os já disponíveis para venda
    // podem comprar uma certa quantidade de bilhetes

    case class ToSell(n: Int)
    case class Buy(n: Int)
    case class Return(n: Int)
    case object Bye

    class MainSellerActor extends Actor {
        val log = Logging(context.system, this)
        // Initial state: no tickets (n=0)
        def receive: Actor.Receive = selling(0, Vector.empty)

        def excessStock(currentStock: Int, currentChildren: Vector[ActorRef]) : (Int, Vector[ActorRef]) = {
            var stock = currentStock
            var children = currentChildren
            while(stock > 100) {
                val child = context.actorOf(Props[ChildSellerActor]()) // cria novo ator (filho do MainSellerActor)
                child ! ToSell(50) // passa valor fixo para tentar vender
                children :+= child // adiciona o filho à lista de filhos do ator principal
                stock -= 50
            }
            (stock, children)
        }
        
        // Selling state: n tickets available
        def selling(n: Int, children: Vector[ActorRef]): Actor.Receive = {
            case ToSell(m) =>
                val (stock, updatedChildren) = excessStock(n + m, children) // atualiza o stock e a lista de filhos do ator principal
                log.info(s"[Main] Received to sell message: $m, updated stock: $stock, children count: ${updatedChildren.size}")
                context.become(selling(stock, updatedChildren)) // atualiza o stock do ator principal
            case Buy(m) => 
                log.info(s"[Main] Received buy message: $m")
                if (n >= m) context.become(selling(n - m, children)) // tem stock suficiente para vender m bilhetes
                else if (children.nonEmpty) {
                    log.info(s"[Main] Not enough tickets to sell $m tickets, asking children for stock")
                    children.head.forward(Buy(m)) // encaminha a mensagem de compra para o primeiro filho da lista
                }
                else {
                    log.error(s"[Main] Not enough tickets to sell $m tickets and no children to ask for stock")
                }
            case Return(m) => 
                val activeChildren = children.filterNot(_ == sender()) // remove o filho que retornou o stock
                log.info(s"[Main] Received return message: $m from child, updating stock and children list")
                val (updatedStock, updatedChildren) = excessStock(n + m, activeChildren) // atualiza o stock e a lista de filhos do ator principal
                context.become(selling(updatedStock, updatedChildren)) // atualiza o stock do ator principal
            case Bye => 
                log.info("[Main] Received bye message, stopping actor")
                context.stop(self)
            case _ => 
                log.info("[Main] Received unknown message")
        }
    }

    class ChildSellerActor extends Actor {
        val log = Logging(context.system, this)
        // estado inicial: 0 bilhetes disponíveis para venda
        def receive: Actor.Receive = selling(0)

        // estado de venda, com n bilhetes disponíveis para venda
        def selling(n: Int): Actor.Receive = {
            case ToSell(m) => 
                val newStock = n + m
                log.info(s"[Child] Received to sell message: $m, updated stock: $newStock")
                context.become(selling(newStock)) // adicionar m bilhetes à venda
            case Buy(m) => 
                log.info(s"[Child] Received buy message: $m")
                if (n >= m) context.become(selling(n - m)) // tem stock suficiente para vender m bilhetes
                else {
                    log.error(s"[Child] Not enough tickets to sell $m tickets, returning remaining stock to parent")
                    context.parent ! Return(n) // retorna o stock restante para o pai
                    context.parent.forward(Buy(m))
                    context.become(rejecting)
                }
            case _ => 
                log.info("[Child] Received unknown message")
        }

        def rejecting: Actor.Receive = {
            case Buy(m) => 
                log.error(s"[Child] Rejecting buy message: $m, not enough stock")
                context.parent.forward(Buy(m)) // encaminha a mensagem de compra para o pai
            case "AcknowledgeDeath" => 
                log.info("[Child] Received acknowledge death message, stopping actor")
                context.stop(self)
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
    val ticketOffice = sys.actorOf(Props[MainSellerActor](), "main-office")
    ticketOffice ! ToSell(initialStock)
    for (_ <- 0 until numRequests) ticketOffice ! Buy(ticketsToBuy)
    println(s"Tried to buy many ${ticketsToBuy*numRequests} tickets.")
    Thread.sleep(3000)
    ticketOffice ! Bye
    Thread.sleep(1000)
    sys.terminate()
}