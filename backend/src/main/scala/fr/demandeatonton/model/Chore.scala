package fr.demandeatonton.model

final case class Chore(id: Int, name: String)
final case class Chores(chores: List[Chore])
