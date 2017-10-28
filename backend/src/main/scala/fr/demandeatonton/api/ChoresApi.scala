package fr.demandeatonton.api

import fr.demandeatonton.model.Chore

import scala.concurrent.Future

trait ChoresApi {
  def createChore(chore: Chore): Future[Unit]
  def deleteChore(chore: Chore): Future[Int]
  def updateChore(chore: Chore): Future[Int]
  def findChoreByName(name: String): Future[List[Chore]]
  def findChoreById(id: Int): Future[Option[Chore]]
}