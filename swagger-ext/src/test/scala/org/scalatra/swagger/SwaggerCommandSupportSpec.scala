package org.scalatra
package swagger

import org.specs2.mutable._
import org.scalatra.databinding._
import org.json4s.DefaultFormats
import org.json4s.Formats
import test.specs2.MutableScalatraSpec

object SwaggerCommandSupportSpec  {
  class SimpleCommand extends ParamsOnlyCommand {
    val name: Field[String] = asString("name").notBlank
    val age: Field[Int] = "age"
  }
  
  class FullCommand extends JsonCommand {
    protected implicit val jsonFormats: Formats = DefaultFormats
    import ValueSource._
    
    val name: Field[String] = asString("name").notBlank
    val age: Field[Int] = "age"
    val token: Field[String] = (
        asString("API-TOKEN").notBlank
	        sourcedFrom Header 
	        description "The API token for this request"
	        notes "Invalid data kills kittens"
	        allowableValues "123")
    val skip: Field[Int] = asInt("skip").sourcedFrom(Query).description("The offset for this collection index")
    val limit: Field[Int] = asType[Int]("limit").sourcedFrom(Query).withDefaultValue(20).description("the max number of items to return")
  }

  class CommandSupportServlet(protected implicit val swagger: Swagger) extends ScalatraServlet with ParamsOnlyCommandSupport with SwaggerSupport with SwaggerCommandSupport {

    get("/all", parameters[SimpleCommand], endpoint("all"), nickname("all")) { "OK" }
    get("/new", parameters(new SimpleCommand), endpoint("new"), nickname("new")) { "OK" }

    protected def applicationDescription: String = "The command support servlet"

    override protected def applicationName: Option[String] = Some("support")

  }
}
class SwaggerCommandSupportSpec extends MutableScalatraSpec {

  import SwaggerCommandSupportSpec._
  implicit val swagger = new Swagger("1.1", "1")
  addServlet(new CommandSupportServlet, "/")

  "SwaggerCommandSupport" should {
    "generate a model and parameters for a simple command" in {
      val (parameters, model) = SwaggerCommandSupport.parametersFromCommand(new SimpleCommand)
      parameters must_== List(Parameter("body", "", DataType("SimpleCommand"), paramType = ParamType.Body))
      model must beSome[Model]
      model.get.id must_== "SimpleCommand"
      model.get.description must beEmpty
      model.get.properties must_== Map("name" -> ModelField("name", "", DataType.String), "age" -> ModelField("age", "", DataType.Int, defaultValue = Some("0"), required = false))
    }
    
    "generate a model and parameters for a full command" in {
      val parameterList = List(
        Parameter("body", "", DataType("FullCommand"), paramType = ParamType.Body),
        Parameter("limit", "the max number of items to return", DataType.Int, paramType = ParamType.Query, defaultValue = Some("20"), required = false),
        Parameter("skip", "The offset for this collection index", DataType.Int, paramType = ParamType.Query, defaultValue = Some("0"), required = false),
        Parameter("API-TOKEN", "The API token for this request", DataType.String, notes = Some("Invalid data kills kittens"), paramType = ParamType.Header, allowableValues = AllowableValues("123"))
      )
      val (parameters, model) = SwaggerCommandSupport.parametersFromCommand(new FullCommand)
      parameters.size must_== 3 // parameterList.size // disabled for swagger codegen for now
      parameters must contain(parameterList(0))
      parameters must contain(parameterList(1))
      parameters must contain(parameterList(2))
      // Disabled headers for now, until swagger codegen mangles header names
//      parameters must contain(parameterList(3))
      model must beSome[Model]
      model.get.id must_== "FullCommand"
      model.get.description must beEmpty
      model.get.properties must_== Map("name" -> ModelField("name", "", DataType.String), "age" -> ModelField("age", "", DataType.Int, defaultValue = Some("0"), required = false))
    }
  }
  
}