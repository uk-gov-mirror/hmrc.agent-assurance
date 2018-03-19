/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Request}
import uk.gov.hmrc.agentassurance.model.Value
import uk.gov.hmrc.agentassurance.repositories.R2dwRepository
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class R2dwController @Inject()(repository: R2dwRepository) extends PropertiesController(repository) {

  override def key = "refusal-to-deal-with"

  def createProperty = Action.async(parse.json) { implicit request =>
    withJsonBody[Value] { value =>
      baseCreateProperty(value)
    }
  }

  def updateProperty = Action.async(parse.json) { implicit request =>
    withJsonBody[Value] { value =>
      baseUpdateProperty(key, value)
    }
  }

  def isOnR2dwList(identifier: String) = Action.async { implicit request =>
    repository.findProperty(key).map { mayBeProperty =>
      if (mayBeProperty.isDefined && mayBeProperty.get.value.contains(identifier)) Forbidden else Ok
    }
  }

  def getFullR2dwList = Action.async { implicit request =>
    repository.findProperty(key).map { maybeProperty =>
      if (maybeProperty.isDefined && maybeProperty.get.value.length > 0) Ok(maybeProperty.get.value) else NoContent
    }
  }

  def deleteEntireProperty = Action.async { implicit request =>
    baseDeleteEntireProperty(key)
  }

  def deleteIdentifierInProperty(identifier: String) = Action.async { implicit request =>
    baseDeleteIdentifierInProperty(key, identifier)
  }
}
