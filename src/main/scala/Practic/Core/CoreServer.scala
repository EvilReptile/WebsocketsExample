package Practic.Core

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object CoreServer extends App {
    implicit val system = ActorSystem("app")
    implicit val materializer = Materializer.matFromSystem
    implicit val executionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

    val coreService = new CoreService
    
    val chatApi = path("api" / "chat")(get {
        parameters("name") { name =>
            handleWebSocketMessages(coreService.flow(name))
        }
    })

    Http().bindAndHandle(chatApi, "localhost", 8080)
        .map { _ =>
            println(s"Server is running at http://localhost:8080/")
        }

}
