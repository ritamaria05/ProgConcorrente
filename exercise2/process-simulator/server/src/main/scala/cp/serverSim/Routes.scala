package cp.serverSim

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

object Routes {
  // Logger object, printing to the file logs/logs.txt
  private val logger = LoggerFactory.getLogger(getClass)
  private val state = new ServerState()
  private val executor = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())

  val routes: IO[HttpRoutes[IO]] =
   IO{HttpRoutes.of[IO] {

     // React to a "status" request
     case GET -> Root / "status" =>
       Ok(state.toHtml)
         .map(addCORSHeaders)
         .map(_.withContentType(org.http4s.headers.`Content-Type`(MediaType.text.html)))

     // React to a "reset" request
    case GET -> Root / "reset" =>
      state.clear()
      Ok("State reset!")
        .map(addCORSHeaders)

     // React to a "run-simulation" request
     case req@GET -> Root / "run-simulation" =>
       val cmdOpt = req.uri.query.params.get("cmd")
       val userIp = req.remoteAddr.getOrElse("unknown")

       //// printing to the terminal instead of a logging file
       //println(">>> got run-simulation!")
       //println(s">>> Cmd: ${cmdOpt}")
       //println(s">>> userIP: $userIp")

       cmdOpt match {
         case Some(cmd) =>
          // calling the `runProcess` method, which simulates running a process
           Ok(runProcess(cmd, userIp.toString))
             .map(addCORSHeaders)

         case None =>
           BadRequest("⚠️ Command not provided. Use /run-simulation?cmd=<your_commands>")
             .map(addCORSHeaders)
       }
   }}


  /** Run a given process and collect its output. */
  /**
    * This method simulates running a process. It should be replaced with actual code
    * to simulate the process using a thread pool. The `Thread.sleep` is just mimicking
    * the time to process the comand, and should be removed.
    *
    * @param cmd the command to run, which can be a single command or multiple commands separated by ";"
    * @param userIp the IP address of the user who sent the request, used for logging purposes
    * @return a string confirming the received command and user IP, which will be sent back to the client as a response
    */
  private def runProcess(cmd: String, userIp: String): String = {
    val cnt = state.nextRequestId()
    val cmds = cmd.split(";").map(_.trim).filter(_.nonEmpty)
    // Printing the received command and user IP to the logs
    logger.info(s"🔹 Starting processes (${cnt}) for user $userIp:" +
      s"${cmds.map("\n - "+_).mkString}")

    cmds.foreach { raw =>
      executor.submit(new Runnable {
        override def run(): Unit = executeInstruction(stripAfterClause(raw), cnt, userIp)
      })
    }

    s"[${cnt}] Submitted ${cmds.length} instruction(s) from $userIp"
  }

  private def stripAfterClause(instruction: String): String = {
    val lower = instruction.toLowerCase
    if (lower.startsWith("after ")) {
      val rest = instruction.drop(6).trim
      val firstSpace = rest.indexOf(' ')
      if (firstSpace > 0) {
        rest.substring(firstSpace + 1).trim
      } else {
        ""
      }
    } else {
      instruction
    }
  }

  private def executeInstruction(instruction: String, reqId: Int, userIp: String): Unit = {
    if (instruction.isEmpty) {
      logger.info(s"[$reqId] Ignored empty instruction from $userIp")
      return
    }

    if (instruction.toLowerCase.startsWith("print ")) {
      val message = instruction.drop(6).trim
      val rendered = s"req=$reqId ip=$userIp print=$message"
      state.addPrintEntry(rendered)
      logger.info(s"[$reqId] $rendered")
    } else {
      logger.info(s"[$reqId] Ignored unsupported instruction: $instruction")
    }
  }


  /** Add extra headers, required by the client. */
  def addCORSHeaders(response: Response[IO]): Response[IO] = {
    response.putHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Content-Type, Authorization",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }
}


