package exercise3
import akka.actor._
import akka.event.Logging
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

    // Testing the system
    val sys = akka.actor.ActorSystem("TicketSys")
    // main-office: ator principal
    val ticketOffice = sys.actorOf(Props[SellerActor], "main-office")

    class SellerActor extends Actor {
    val log = Logging(context.system, this)
    var rotateIdx = 0
    // Initial state: no tickets (n=0)
    def receive: Actor.Receive = selling(0)
    
    // Selling state: n tickets available
    def selling(n: Int): Actor.Receive = {
        case ToSell(m) =>
            var stock = n+m 
            log.info(s"Received to sell message: $m")
            while(stock > 100) {
                val child = context.actorOf(Props[SellerActor]) // cria novo ator (filho do SellerActor)
                child ! ToSell(50) // passa valor fixo para tentar vender
                stock -= 50
            }
            context.become(selling(stock)) // atualiza o stock do ator principal
        case Buy(m) => 
            log.info(s"Received buy message: $m")
            if (n >= m) context.become(selling(n - m)) // tem stock suficiente para vender m bilhetes
            else {
                val filhos = context.children.toList
                if (filhos.isEmpty) {
                    log.error(s"Not enough tickets to sell $m tickets")
                    if (self.path.name != "main-office") { // quando o filho n tem stock suficiente, vai aos filhos dele - se nao tem, retorna ao pai
                        log.info(s"Returning remaining stock of $n tickets to parent")
                        context.parent ! Return(n) // retorna o stock restante para o pai
                        context.stop(self) // para o ator atual
                    }
                } else {
                    log.info(s"Not enough tickets to sell $m tickets, asking children for stock")
                    // round robin
                    if (rotateIdx >= filhos.size) rotateIdx = 0
                    val designedChild = filhos(rotateIdx)
                    designedChild.forward(Buy(m)) // encaminha a mensagem de compra para o filho designado
                    rotateIdx = (rotateIdx + 1) % filhos.size // atualiza o índice para o próximo filho na próxima vez
                }
            }
        case Return(m) => 
            log.info(s"Received return message: $m")
            context.become(selling(n + m)) // atualiza o stock do ator principal com o valor retornado pelo filho
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
    Thread.sleep(3000)
    ticketOffice ! "Bye"
    Thread.sleep(1000)
    sys.terminate()
}