package cp.serverSim

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.io.Source

object VolatileTest extends App {

  private val baseUrl = "http://127.0.0.1:8080"

  private def readUrl(target: String): String = {
    val src = Source.fromURL(target)
    try src.mkString
    finally src.close()
  }

  // Program used to demonstrate cross-thread visibility of `paused`.
  // It includes dependencies so blocked workers are easy to observe.
  private val inputProgram =
    "print \"A\" @1; print \"B\" after 1; print \"C\" after 1; print \"D\" after 2,3; print \"E\"; print \"F\" after 5"

  private val encodedCmd = URLEncoder.encode(inputProgram, StandardCharsets.UTF_8.name())

  println("Step 1: pausing server...")
  println(readUrl(s"$baseUrl/pause"))

  println("Step 2: submitting run-simulation while paused...")
  val submitThread = new Thread(() => {
    val response = readUrl(s"$baseUrl/run-simulation?cmd=$encodedCmd")
    println(s"run-simulation response: $response")
  })
  submitThread.start()

  println("Step 3: waiting so worker threads enter the paused loop...")
  Thread.sleep(1500)

  println("Step 4: resuming server...")
  println(readUrl(s"$baseUrl/resume"))

  submitThread.join()

  println("Step 5: waiting for execution to complete...")
  Thread.sleep(3000)

  println("Step 6: fetching /status...")
  val status = readUrl(s"$baseUrl/status")
  println(status)

  println("If @volatile is removed from paused, this scenario may hang or miss outputs.")
}
