package Practic.Print

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{get, handleWebSocketMessages, path}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object PrintServer extends App {
    implicit val system = ActorSystem("app")
    implicit val materializer = Materializer.matFromSystem
    implicit val executionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

    val printService = new PrintService
    val chatApi = path("generatePf")(get {
        handleWebSocketMessages(printService.flow)
    })

    Http().bindAndHandle(chatApi, "localhost", 8090)
        .map { _ =>
            println(s"Server is running at http://localhost:8090/")
        }

}
