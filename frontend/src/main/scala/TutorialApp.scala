import scala.scalajs.js
import org.scalajs.jquery._

import scala.scalajs.js.JSON

@js.native
trait Chore extends js.Object {
  val id: Int = js.native
  val name: String = js.native
}

object TutorialApp {

  def main(args: Array[String]): Unit =
    jQuery(() => setupUI())

  def setupUI(): Unit = {
    jQuery("body").append("<p>Hello World</p>")
    jQuery("body").append("<p><button id='click-me-button'>Click me!</button></p>")
    jQuery("#click-me-button").click(() => showChores())
  }

  def showChores(): Unit = {
    val req = jQuery
      .ajax(
        js.Dynamic
          .literal(
            url = "http://localhost:8080/api/chores",
            success = { (data: js.Any, textStatus: String, jqXHR: JQueryXHR) =>
              println(s"data=$data,text=$textStatus,jqXHR=$jqXHR")
              val json = JSON.stringify(data)
              val parsed = jQuery.parseJSON(json).selectDynamic("chores")
              val subJson = JSON.stringify(parsed)
              val subParsed = jQuery.parseJSON(subJson)
              val myArray = subParsed.asInstanceOf[js.Array[Chore]]

              myArray.map(x =>
                jQuery("body").append(s"<p>Id: ${x.id} - Name : ${x.name}")
              )
            },
            error = { (jqXHR: JQueryXHR, textStatus: String, errorThrow: String) =>
              println(s"jqXHR=$jqXHR,text=$textStatus,err=$errorThrow")
            },
            `type` = "GET",
            dataType = "json"
          )
          .asInstanceOf[JQueryAjaxSettings])

  }

}
