package cp.serverSim

import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class ServerState {
  private val counter = new AtomicInteger(0)
  private val printLog = new ConcurrentLinkedQueue[String]()
  private val logSize = new AtomicInteger(0)
  private val maxEntries = 500

  def nextRequestId(): Int = counter.incrementAndGet()

  def currentCounter: Int = counter.get

  def addPrintEntry(message: String): Unit = {
    val ts = Instant.now().toString
    val entry = s"[$ts] $message"
    printLog.add(entry)
    val size = logSize.incrementAndGet()

    if (size > maxEntries) {
      while (logSize.get > maxEntries) {
        val removed = printLog.poll()
        if (removed != null) {
          logSize.decrementAndGet()
        } else {
          return
        }
      }
    }
  }

  def clear(): Unit = {
    counter.set(0)
    while (printLog.poll() != null) {}
    logSize.set(0)
  }

  def toHtml: String = {
    val items = printLog.toArray(new Array[String](0)).mkString("<li>", "</li><li>", "</li>")
    val logsHtml = if (items.nonEmpty) s"<ul>$items</ul>" else "<p><em>No print entries yet.</em></p>"

    s"""
       |<p><strong>counter:</strong> ${currentCounter}</p>
       |<p><strong>print log:</strong></p>
       |$logsHtml
       |""".stripMargin
  }
}
