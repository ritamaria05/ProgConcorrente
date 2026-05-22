package cp.serverSim

import scala.io.Source
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object StressTest extends App {

  private val baseUrl = "http://127.0.0.1:8080"

  private def readUrl(target: String): String = {
    val src = Source.fromURL(target)
    try src.mkString
    finally src.close()
  }

  val threads = (1 to 50).map { i =>
    new Thread(() => {
      try {
        val encodedCmd = URLEncoder.encode(s"print $i", StandardCharsets.UTF_8.name())
        val url = s"$baseUrl/run-simulation?cmd=$encodedCmd"
        val response = readUrl(url)
        println(s"[$i] $response")
      } catch {
        case e: Exception =>
          println(s"[$i] ERROR: ${e.getMessage}")
      }
    })
  }

  threads.foreach(_.start())
  threads.foreach(_.join())
}