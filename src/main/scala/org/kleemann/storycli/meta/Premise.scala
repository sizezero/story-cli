package org.kleemann.storycli.meta

case class Premise(oneLine: String)

object Premise {

    val filename = "premise.md"

    def extract(repo: os.Path): Either[String, Premise] =
        iterator.Premise.extract(repo)

    def read(dir: os.Path): Either[String, Premise] =
        iterator.Premise.read(dir)
}
