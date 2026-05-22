package cp.serverSim

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import scala.util.matching.Regex

object Routes {
  // Logger object, printing to the file logs/logs.txt
  private val logger = LoggerFactory.getLogger(getClass)
  private val state = new ServerState()
  private val executor = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())

  private case class ParsedInstruction(index: Int, message: String, delaySeconds: Int, dependencies: List[Int])

  @volatile private var paused = false

  private val instructionPattern: Regex =
    """(?i)^print\s+\"([^\"]*)\"\s*(?:@\s*(\d+))?\s*(?:after\s*([0-9\s,]+))?\s*$""".r

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

    // Endpoints for volatile variable example
    case GET -> Root / "pause" =>
      paused = true
      Ok("Simulation paused")
        .map(addCORSHeaders)

    case GET -> Root / "resume" =>
      paused = false
      Ok("Simulation resumed")
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

    val parsed = parseInstructions(cmds)

    if (parsed.length != cmds.length) {
      state.addPrintEntry(s"req=$cnt ip=$userIp parse-error=invalid instruction format")
      return s"[$cnt] Invalid instruction format. Expected: print 'msg' [@ seconds] [after i,j,...]"
    }

    validateDependencies(parsed) match {
      case Some(error) =>
        state.addPrintEntry(s"req=$cnt ip=$userIp dependency-error=$error")
        return s"[$cnt] $error"
      case None =>
    }

    val latches = Array.fill(parsed.length)(new CountDownLatch(1))

    parsed.foreach { inst =>
      executor.submit(new Runnable {
        override def run(): Unit = executeInstruction(inst, cnt, userIp, latches)
      })
    }

    s"[${cnt}] Submitted ${parsed.length} instruction(s) from $userIp"
  }

  private def parseInstructions(rawInstructions: Array[String]): List[ParsedInstruction] = {
    rawInstructions.zipWithIndex.flatMap { case (raw, idx) =>
      raw match {
        case instructionPattern(msg, delay, afterRaw) =>
          val delaySeconds = Option(delay).map(_.toInt).getOrElse(0)
          val deps = Option(afterRaw)
            .map(_.split(",").map(_.trim).filter(_.nonEmpty).map(_.toInt).toList)
            .getOrElse(Nil)
          Some(ParsedInstruction(idx + 1, msg, delaySeconds, deps))
        case _ => None
      }
    }.toList
  }

  private def validateDependencies(instructions: List[ParsedInstruction]): Option[String] = {
    val maxIndex = instructions.length
    val invalidIndex = instructions.exists { inst =>
      inst.dependencies.exists(d => d < 1 || d > maxIndex || d == inst.index)
    }

    if (invalidIndex) {
      return Some("Invalid dependency index in after-clause")
    }

    if (hasDependencyCycle(instructions)) {
      return Some("Invalid dependency graph: cycle detected in after-clause")
    }

    None
  }

  private def hasDependencyCycle(instructions: List[ParsedInstruction]): Boolean = {
    val depsByInstruction = instructions.map(i => i.index -> i.dependencies).toMap
    val visitState = Array.fill(instructions.length + 1)(0)

    def dfs(node: Int): Boolean = {
      if (visitState(node) == 1) {
        return true
      }
      if (visitState(node) == 2) {
        return false
      }

      visitState(node) = 1
      val deps = depsByInstruction.getOrElse(node, Nil)
      val hasCycle = deps.exists(dfs)
      visitState(node) = 2
      hasCycle
    }

    instructions.exists(inst => visitState(inst.index) == 0 && dfs(inst.index))
  }

  private def executeInstruction(
    instruction: ParsedInstruction,
    reqId: Int,
    userIp: String,
    latches: Array[CountDownLatch]
  ): Unit = {


    while (paused) {
      Thread.sleep(10) // Check every 10ms if we should resume
    }

    try {
      instruction.dependencies.foreach { dep =>
        latches(dep - 1).await()
      }

      if (instruction.delaySeconds > 0) {
        Thread.sleep(instruction.delaySeconds.toLong * 1000L)
      }

      val rendered =
        s"req=$reqId ip=$userIp instr=${instruction.index} print=${instruction.message} delay=${instruction.delaySeconds}s after=${instruction.dependencies.mkString(",")}"
      state.addPrintEntry(rendered)
      logger.info(s"[$reqId] executed: $rendered")
    } catch {
      case e: InterruptedException =>
        logger.warn(s"[$reqId] Instruction ${instruction.index} interrupted", e)
        Thread.currentThread().interrupt()
    } finally {
      latches(instruction.index - 1).countDown()
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


