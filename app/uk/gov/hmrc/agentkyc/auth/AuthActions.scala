/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentkyc.auth

import play.api.mvc._
import uk.gov.hmrc.agentkyc.controllers.ErrorResults.NoPermission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

trait AuthActions extends AuthorisedFunctions {
  me: Results =>

  override def authConnector: AuthConnector

  private def getEnrolmentInfo(enrolment: Set[Enrolment], enrolmentKey: String, identifier: String): Option[String] =
    enrolment.find(_.key equals enrolmentKey).flatMap(_.identifiers.find(_.key equals identifier).map(_.value))

  private type AuthorisedRequest = Request[AnyContent] => Future[Result]

  def AuthorisedIRSAAgent[A](body: AuthorisedRequest): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc = fromHeadersAndSession(request.headers, None)
      authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments) {
        enrol =>
          getEnrolmentInfo(enrol.enrolments, "IR-SA-AGENT", "IRAgentReference") match {
            case Some(_) => body(request)
            case _ => Future successful NoPermission
          }
      }
  }
}
