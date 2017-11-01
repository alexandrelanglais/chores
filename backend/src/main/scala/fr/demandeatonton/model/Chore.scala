package fr.demandeatonton.model

import java.util.UUID

final case class Chore(id:      String, name: String, description: String)
final case class Chores(chores: List[Chore])
