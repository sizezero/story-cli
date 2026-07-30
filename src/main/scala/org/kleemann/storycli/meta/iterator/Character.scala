package org.kleemann.storycli.meta.iterator

import org.kleemann.storycli.meta

object Characters {

    private case class Builder(
        // attributes from Characters
        characters: List[meta.Character], // reverse order

        // attributes needed while iterating
        name: Option[String]
    ) {

        private val namePrefix = "## "
        private val rolePrefix = "* Role:"
        private def noRoleCharacter(name: String) = meta.Character(name, "N/A")

        def add(line: String): Builder = {
            name match
                case None => {
                    // we are in between character blocks looking for the next character
                    if (line.startsWith(namePrefix)) {
                        if (line.startsWith("## Template")) this
                        else this.copy(name = Some(line.stripPrefix(namePrefix).trim))
                    } else this
                }
                case Some(name) => {
                    // we are within the character block and looking for the role
                    if (line.startsWith("#"))
                        // we have reached the next character block so no role was specified
                        // hand the current line back to the parser to parse the next character name
                        this.copy(characters = noRoleCharacter(name) :: characters, name=None).add(line)
                    else if (line.startsWith(rolePrefix)) {
                        val c = meta.Character(name, line.stripPrefix(rolePrefix).trim)
                        this.copy(characters = c :: characters,  name=None).add(line)
                    } else
                        this
                }
        }

        def toCharacters: List[meta.Character] = {
            val cs = name match
                case None => characters
                // the file has ended while we were in a character block looking for a role
                case Some(name) => noRoleCharacter(name) :: characters
            cs.reverse
        }
    }

    def create(lines: Iterator[String]): Either[String, List[meta.Character]] =  {
        // It's kind of amazing that there is no error case in parsing.
        // We still return Either so that we match the meta.pipe signature.

        // push the Builder object through all the lines of the file
        Right(lines.foldLeft(Builder(Nil, None)){ (b, line) => b.add(line) }.toCharacters)
    }

    def extract(repo: os.Path): Either[String, List[meta.Character]] =
        extractFile(repo, meta.Characters.filename).flatMap{ create(_) }

    def read(dir: os.Path): Either[String, List[meta.Character]] =
        readFile(dir / meta.Characters.filename).flatMap{ create(_) }
}