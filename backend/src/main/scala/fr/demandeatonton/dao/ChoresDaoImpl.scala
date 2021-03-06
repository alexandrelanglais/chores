package fr.demandeatonton.dao

import java.util.UUID

import fr.demandeatonton.api.ChoresApi
import fr.demandeatonton.model.Chore
import fr.demandeatonton.model.Chores
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.DefaultDB
import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.Macros
import reactivemongo.bson.document

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

final case class ChoresDaoImpl() extends ChoresApi {
  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017/mydb?authMode=scram-sha1"

  // Connect to the database: Must be done only once per application
  val driver     = MongoDriver()
  val parsedUri  = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection: Future[MongoConnection] = Future.fromTry(connection)
  def db:               Future[DefaultDB]       = futureConnection.flatMap(_.database("choresDb"))
  def choresCollection: Future[BSONCollection]  = db.map(_.collection("chores"))

  // Write Documents: insert or update

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit val choresWriter: BSONDocumentWriter[Chore] = Macros.writer[Chore]

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  implicit val choreReader: BSONDocumentReader[Chore] = Macros.reader[Chore]

  override def createChore(chore: Chore): Future[Chore] =
    for {
      coll <- choresCollection
      _ = coll.insert(chore)
    } yield (chore)
//    choresCollection.flatMap(x => x. .insert(person).map(_ => {})) // use personWriter

  override def updateChore(chore: Chore): Future[Int] = {
    val selector = document(
      "name" -> chore.name,
      "id"   -> chore.id
    )
    choresCollection.flatMap(_.update(selector, chore).map(_.n))
  }

  override def deleteChore(chore: Chore): Future[Int] = {
    val selector = document(
      "name" -> chore.name,
      "id"   -> chore.id
    )
    choresCollection.flatMap(_.remove(selector).map(_.n))
  }

  // or provide a custom one

  override def findChoreByName(name: String): Future[List[Chore]] =
    choresCollection.flatMap(
      _.find(document("name" -> name)). // query builder
      cursor[Chore]().collect[List]()) // collect using the result cursor

  override def findChoreById(id: String): Future[Option[Chore]] =
    choresCollection.flatMap(_.find(document("id" -> id)).one[Chore]) // collect using the result cursor

  override def allChores(): Future[Chores] =
    choresCollection
      .flatMap(
        _.find(document). // query builder
        cursor[Chore]().collect[List]())
      .flatMap(x => Future(Chores(x))) // collect using the result cursor

}

object ChoresDaoImpl {

  def main(args: Array[String]): Unit = {
    val dao = ChoresDaoImpl()

//    val f = dao
//      .createChore(Chore(id = UUID.randomUUID().toString, name = "Vaisselle", description = "Faire la vaisselle"))
//
//    f.onComplete(_ match {
//      case Success(s) => println(s)
//      case Failure(e) => println(e)
//    })
  }

}
