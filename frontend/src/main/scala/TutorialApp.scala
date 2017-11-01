import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.FormData

import scala.scalajs.js
import org.scalajs.jquery.jQuery
import org.scalajs.jquery._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSGlobalScope
import scala.util.Failure
import scala.util.Success

@js.native
@JSGlobalScope
object FormToJson extends js.Object {
  def objectifyForm(id: String): String = js.native
}

@js.native
trait Chore extends js.Object {
  val id:          String = js.native
  val name:        String = js.native
  val description: String = js.native
  val imgPath:     String = js.native
}

object TutorialApp {

  def main(args: Array[String]): Unit =
    jQuery(() => setupUI())

  def setupUI(): Unit = {
    jQuery("#refreshChores").click(() => showChores())
    jQuery("#createChore").submit((e) => createChore(e))
    showChores()
  }

  def showChores(): Unit = {
    jQuery("#lstChores").empty()
    val req = jQuery
      .ajax(
        js.Dynamic
          .literal(
            url = "http://localhost:8080/api/chores",
            success = { (data: js.Any, textStatus: String, jqXHR: JQueryXHR) =>
              println(s"data=$data,text=$textStatus,jqXHR=$jqXHR")
              val json      = JSON.stringify(data)
              val parsed    = jQuery.parseJSON(json).selectDynamic("chores")
              val subJson   = JSON.stringify(parsed)
              val subParsed = jQuery.parseJSON(subJson)
              val myArray   = subParsed.asInstanceOf[js.Array[Chore]]
              myArray.map(
                x =>
                  jQuery("#lstChores").append(
                    s"<p>Name : ${x.name} - Description : ${x.description} - Image : <a href='images/${x.imgPath}'>${x.imgPath}</a>"))
            },
            error = { (jqXHR: JQueryXHR, textStatus: String, errorThrow: String) =>
              println(s"jqXHR=$jqXHR,text=$textStatus,err=$errorThrow")
            },
            `type`   = "GET",
            dataType = "json"
          )
          .asInstanceOf[JQueryAjaxSettings])

  }

  def createChore(e: JQueryEventObject): Unit = {
    e.preventDefault()
    println("yo")

    val req = jQuery
      .ajax(
        js.Dynamic
          .literal(
            url = "http://localhost:8080/api/chores",
            success = { (data: js.Any, textStatus: String, jqXHR: JQueryXHR) =>
              showChores()
            },
            error = { (jqXHR: JQueryXHR, textStatus: String, errorThrow: String) =>
              println(s"jqXHR=$jqXHR,text=$textStatus,err=$errorThrow")
            },
            `type`      = "POST",
            contentType = "application/json",
            data        = FormToJson.objectifyForm("#createChore")
          )
          .asInstanceOf[JQueryAjaxSettings])
  }

}
