package fr.demandeatonton

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import fr.demandeatonton.api.ChoreInput
import fr.demandeatonton.dao.ChoresDaoImpl
import fr.demandeatonton.model.Chore
import fr.demandeatonton.model.Chores
import spray.json.DefaultJsonProtocol

import scala.collection.immutable
import scala.io.StdIn
import scala.util.Failure
import scala.util.Success

sealed trait SrvResponses {}

object SrvResponses {
  final case class SrvChoreCreated(chore: Chore) extends SrvResponses
}

trait CorsWorkAround {

  def corsWorkAround(route: Route): Route =
    respondWithDefaultHeaders(
      immutable.Seq(
        `Access-Control-Allow-Origin`(HttpOriginRange.*),
        `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With", "Access-Control-Allow-Origin"),
        `Access-Control-Expose-Headers`("Set-Authorization"),
        `Access-Control-Allow-Methods`(PATCH, GET, POST, PUT, DELETE, OPTIONS)
      ))(route)

  implicit def rejectionHandler =
    RejectionHandler
      .newBuilder()
      .handleAll[MethodRejection] { rejections =>
        val methods    = rejections map (_.supported)
        lazy val names = methods map (_.name) mkString ", "
        respondWithHeader(Allow(methods)) {
          options {
            complete(s"Supported methods : $names.")
          } ~
            complete(MethodNotAllowed, s"HTTP method not allowed, supported methods: $names!")
        }
      }
      .result()
}

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val choreInputFormat = jsonFormat2(ChoreInput)
  implicit val choreFormat      = jsonFormat3(Chore)
  implicit val choresFormat     = jsonFormat1(Chores)
}

object WebServer extends Directives with JsonSupport with CorsWorkAround {

  def main(args: Array[String]) {
    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
    val settings                  = CorsSettings.defaultSettings.copy(allowedOrigins = HttpOriginRange.*)
    val choresApi                 = ChoresDaoImpl()

    val routes: Route = cors(settings) {
      pathPrefix("api") {
        path("chores") {
          get {
            onComplete(
              choresApi
                .allChores()
            ) {
              _ match {
                case Success(list: Chores) => complete(list)
                case Failure(ex) => complete(BadRequest, HttpEntity(`application/json`, ex.getMessage))
              }
            }
          } ~
            post {
              entity(as[ChoreInput]) { choreInput =>
                onComplete(
                  choresApi
                    .createChore(choreInput.toChore)
                ) {
                  _ match {
                    case Success(chore) => complete(Created, "Ok")
                    case Failure(ex) => complete(BadRequest, ex.getMessage)
                  }
                }
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
