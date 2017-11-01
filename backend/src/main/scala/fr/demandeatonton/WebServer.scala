package fr.demandeatonton

import better.files.File
import better.files._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import fr.demandeatonton.api.ChoreInput
import fr.demandeatonton.dao.ChoresDaoImpl
import fr.demandeatonton.model.Chore
import fr.demandeatonton.model.Chores
import spray.json.DefaultJsonProtocol

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
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
  implicit val choreInputFormat = jsonFormat3(ChoreInput)
  implicit val choreFormat      = jsonFormat4(Chore)
  implicit val choresFormat     = jsonFormat1(Chores)
}

object WebServer extends Directives with JsonSupport with CorsWorkAround {

  def main(args: Array[String]): Unit = {
    val testConf: Config = ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.log-dead-letters = off""")

    implicit val system       = ActorSystem("my-system", testConf)
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
                    case Failure(ex)    => complete(BadRequest, ex.getMessage)
                  }
                }
              }

              //              entity(as[Multipart.FormData]) { (formData) =>
              //                val processedParts = formData.parts.mapAsync(1) { part =>
              //                  part.filename match {
              //                    case Some("xyz.bin") => processBinaryPart(part)
              //                    case Some("abc.json") => processJsonPart(json)
              //                  }
              //                } // result of mapAsync is Source[ProcessingResult, ...]
              //                  // now collect all results into a map for further processing
              //                  .runFold(Map.empty[String, ProcessingResult])((map, res) => map += res.partFileName -> res)
              //                // result is a Future[Map[String, ProcessingResult]]
              //
              //                // wait for processing to have finished and then create response
              //                onSuccess(processedParts) { resultMap =>
              //                  complete(Created, "Ok")
              //                }
              //              }
            }
        } ~
          path("upload") {
            entity(as[Multipart.FormData]) { (formdata: Multipart.FormData) =>
              val fileNamesFuture = formdata.parts
                .mapAsync(1) { p ⇒
                  println(s"Got part. name: ${p.name} filename: ${p.filename}")
                  val tmpFile = file"frontend/images/${p.filename.getOrElse("nope")}"
                  if (p.name == "daFile") {
                    p.entity.dataBytes
                      .runWith(FileIO.toPath(tmpFile.path))
                      .filter(_.wasSuccessful)
                      .map(_ => (Some(tmpFile), None))

                  } else {
                    p.toStrict(5.seconds).map { v =>
                      (Option.empty[File], Some((p.name -> v.entity.data.utf8String)))
                    }
                  }
                }
                .runFold((Option.empty[File], Map.empty[String, String])) { (acc, res) =>
                  res match {
                    case (s @ Some(_), _) => (s, acc._2)
                    case (_, Some(tuple)) => (acc._1, acc._2 + tuple)
                    case (None, _)        => acc
                  }
                }

              onComplete(fileNamesFuture) {
                case Success(data) => {
                  val choreInput = ChoreInput(
                    name        = data._2.get("name").getOrElse(""),
                    description = data._2.get("description").getOrElse(""),
                    imgPath     = Option(data._1.getOrElse(File.newTemporaryFile()).name)
                  )
                  onComplete(
                    choresApi.createChore(choreInput.toChore)
                  ) {
                    _ match {
                      case Success(chore) => complete(Created, "Ok")
                      case Failure(ex)    => complete(BadRequest, ex.getMessage)
                    }
                  }
                }
                case Failure(ex) => complete(BadRequest, ex.getMessage)
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
