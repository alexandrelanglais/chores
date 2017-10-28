import org.scalajs.jquery.jQuery
import utest._

object TutorialTest extends TestSuite {

  // Initialize App
  TutorialApp.setupUI()

  def tests = TestSuite {
    'HelloWorld {
      assert(jQuery("p:contains('Hello World')").length == 1)
    }
  }
}