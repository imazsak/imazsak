package hu.ksisu.imazsak.util

import spray.json.{JsField, JsObject}

object SpraySyntax {

  implicit class JsObjectExtender(obj: JsObject) {

    def +(nv: JsField): JsObject = {
      JsObject(obj.fields + nv)
    }

  }

}
