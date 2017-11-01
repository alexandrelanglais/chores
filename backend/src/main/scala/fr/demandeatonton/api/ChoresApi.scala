package fr.demandeatonton.api

import java.util.UUID

import fr.demandeatonton.model.Chore
import fr.demandeatonton.model.Chores

import scala.concurrent.Future

trait ChoresApi {
  def createChore(chore:    Chore):  Future[Chore]
  def deleteChore(chore:    Chore):  Future[Int]
  def updateChore(chore:    Chore):  Future[Int]
  def findChoreByName(name: String): Future[List[Chore]]
  def findChoreById(id:     String): Future[Option[Chore]]
  def allChores(): Future[Chores]
}

final case class ChoreInput(
    name:        String,
    description: String,
    imgPath:     Option[String] = Option.empty
) {
  def toChore() = Chore(UUID.randomUUID().toString, name, description, imgPath.getOrElse(""))
}
