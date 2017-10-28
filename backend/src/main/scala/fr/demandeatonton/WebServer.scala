package fr.demandeatonton

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import fr.demandeatonton.dao.ChoresDaoImpl
import fr.demandeatonton.model.Chore
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn
import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait SrvResponses {}

object SrvResponses {
  final case class SrvChoreCreated(chore: Chore) extends SrvResponses
}

object WebServer {

  implicit val choreFormat = jsonFormat2(Chore)

  def main(args: Array[String]) {

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val choresApi = ChoresDaoImpl()
    val routes: Route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      } ~
        path("api" / "chores") {
          get {
            complete(Chore(1, "Laver le linge"))
          } ~
            post {
              entity(as[Chore]) { chore =>
                onComplete(
                  choresApi
                    .createChore(chore)
                ) {
                  _ match {
                    case Success(()) => complete(Created, HttpEntity(`application/json`, "Created"))
                    case Failure(ex) => complete(BadRequest, HttpEntity(`application/json`, "Created"))
                  }
                }
              }
            }
        }

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    val line = StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
