package uk.gov.hmrc.agentkyc.controllers

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.agentkyc.stubs.DesStubs
import uk.gov.hmrc.agentkyc.support.{AgentAuthStubs, IntegrationSpec, WireMockSupport}
import uk.gov.hmrc.domain.{Nino, SaAgentReference}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration._


class KycControllerISpec extends IntegrationSpec with GuiceOneServerPerSuite with AgentAuthStubs with DesStubs with WireMockSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.auth.port" -> wireMockPort,
        "microservice.services.des.port" -> wireMockPort)

  implicit val hc = new HeaderCarrier

  val irSaAgentEnrolmentUrl = s"http://localhost:$port/agent-kyc/irSaAgentEnrolment"

  def irSaAgentEnrolmentUrl(nino: String) = s"http://localhost:$port/agent-kyc/activeCesaRelationship/nino/$nino"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override def irAgentReference: String = "IRSA-123"

  feature("/irSaAgentEnrolment") {
    scenario("User is enrolled in IR_SA_AGENT") {
      Given("User is enrolled in IR_SA_AGENT")
      isLoggedInAndIsEnrolledToIrSaAgent

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("204 NO_CONTENT is returned")
      response.status shouldBe 204
    }

    scenario("User is not enrolled in IR_SA_AGENT") {
      Given("User is not enrolled in IR_SA_AGENT")
      isLoggedInAndNotEnrolledInIrSaAgent

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("401 UNAUTHORISED is returned")
      response.status shouldBe 401
    }
  }

  feature("/activeCesaRelationship/nino/:nino") {
    scenario("User is enrolled in IR_SA_AGENT and provides a NINO which has an active relationship in CESA") {
      Given("User is enrolled in IR_SA_AGENT with an IRAgentReference of IRSA-123")
      isLoggedInAndIsEnrolledToIrSaAgent

      And("CESA contains an active agent/client relationship for NINO AA000000A and Agent Reference IRSA-123")
      givenClientHasRelationshipWithAgentInCESA(Nino("AA000000A"), SaAgentReference(irAgentReference))

      When("GET /activeCesaRelationship/nino/AA000000A is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl("AA000000A")).get(), 10 seconds)

      Then("204 NO_CONTENT is returned")
      response.status shouldBe 204
    }

    scenario("User is not enrolled in IR_SA_AGENT") {
      Given("User is not enrolled in IR_SA_AGENT")
      isLoggedInAndNotEnrolledInIrSaAgent

      When("GET /activeCesaRelationship/nino/AA000000A is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl("AA000000A")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("401 UNAUTHORISED is returned")
      response.status shouldBe 401
    }

    scenario("User is enrolled in IR_SA_AGENT and provides a NINO which has no active relationship in CESA") {
      Given("User is enrolled in IR_SA_AGENT with an IRAgentReference of IRSA-123")
      isLoggedInAndIsEnrolledToIrSaAgent

      And("CESA contains no active agent/client relationship for NINO AA000000A and Agent Reference IRSA-123")
      givenClientHasNoActiveRelationshipWithAgentInCESA(Nino("AA000000A"))

      When("GET /activeCesaRelationship/nino/AA000000A is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl("AA000000A")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User is enrolled in IR_SA_AGENT and provides a NINO which has an active relationship in CESA but with a different Agent Reference") {
      Given("User is enrolled in IR_SA_AGENT with an IRAgentReference of IRSA-123")
      isLoggedInAndIsEnrolledToIrSaAgent

      And("CESA contains no active agent/client relationship for NINO AA000000A and Agent Reference IRSA-456")
      givenClientHasRelationshipWithAgentInCESA(Nino("AA000000A"), SaAgentReference("IRSA-456"))

      When("GET /activeCesaRelationship/nino/AA000000A is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl("AA000000A")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }
  }

}
