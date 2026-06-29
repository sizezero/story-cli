package org.kleemann.storycli.meta

import org.kleemann.storycli.GlobalOptions

class TestStory extends munit.FunSuite {

    test("no title") {
        val in: List[String] = 
        """Line one
        |Line two
        |Line three""".stripMargin.linesIterator.toList

        assertEquals(
            Story.create(in),
            Left("title (# my title) is not defined in the document"))
    }

    test("title only") {
        val in: List[String] = 
        """
        |#   my title 
        |""".stripMargin.linesIterator.toList
        // trailing space after title

        assertEquals(
            Story.create(in),
            Right(Story("my title", Nil)))
    }

    test("title specified twice") {
        val in: List[String] = 
        """line 1
        |# first title
        |
        |# second title
        |""".stripMargin.linesIterator.toList

        assertEquals(
            Story.create(in),
            Left("error(4): title specified twice: 1:first title 2:second title"))
    }

    test("single template incident") {
        val in: List[String] = 
        """
        |#   my title 
        |
        |<!-- begin incident: template
        |
        |Column: first mandatory column: 
        |Column: second mandatory column: 
        |
        |Other text
        |
        |end incident -->
        |
        |Some body text.
        |
        |""".stripMargin.linesIterator.toList
        // trailing space after title

        assertEquals(
            Story.create(in),
            Right(Story("my title", Nil)))
    }

    test("good long story") {
        val in: List[String] = 
        """
        |#   my title 
        |
        |<!-- begin incident: template
        |
        |Column: first mandatory column: 
        |Column: second mandatory column: 
        |
        |Other text
        |
        |end incident -->
        |
        |<!-- begin incident: first incident
        |end incident -->
        |<!-- begin incident: second incident with ???
        |Column:key:value
        |Column: complex Key : long value here
        |end incident -->
        |
        |Some more text.
        |
        |<!-- begin incident: last
        |Column: -? - : -? -
        |end incident -->
        |
        |One line of text.
        |
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |A long line of text that is ten words long.
        |
        |""".stripMargin.linesIterator.toList
        // trailing space after title

        assertEquals(
            Story.create(in),
            Right(Story("my title", 
                List(
                    Incident("first incident", Nil, 0),
                    Incident("second incident with ???",
                        List(
                            Column("key","value"),
                            Column("complex Key","long value here")
                        ),
                        3),
                    Incident("last",
                        List(Column("-? -","-? -")),
                        104)
            ))))
    }

    test("start incident with no end (file ends)") {
        val in: List[String] = 
        """line 1
        |#   my title 
        |
        |<!-- begin incident: template
        |
        |various text
        |
        |""".stripMargin.linesIterator.toList
        // trailing space after title

        Story.create(in) match
            case Left(error) => assertEquals("error(7): file ended while looking for end of incident: template", error)
            case Right(story) => assert(false, story)
    }

    test("start incident with no end (new incident starts)") {
        val in: List[String] = 
        """line 1
        |#   my title 
        |
        |<!-- begin incident: foo
        |
        |various text
        |
        |<!-- begin incident: bar
        |
        |""".stripMargin.linesIterator.toList
        // trailing space after title

        Story.create(in) match
            case Left(error) => assertEquals("error(8): second start of incident. incident: foo", error)
            case Right(story) => assert(false, story)
    }

    test("big csv") {
        val in: List[String] =
        """
        |#   my title
        |
        |<!-- begin incident: breakfast
        |Column: Food: cereal bacon eggs
        |Column: Beverage: coffee orange juice
        |end incident -->
        |
        |<!-- begin incident: second breakfast
        |Column: Beverage: mimosa
        |Column: Conversation: gossip
        |Column: Food: croissant
        |end incident -->
        |
        |<!-- begin incident: lunch
        |Column: Food: hamburger
        |end incident -->
        |
        |<!-- begin incident: dinner
        |Column: Beverage: water
        |Column: Food: salad steak potato
        |end incident -->
        |
        |<!-- begin incident: happy hour
        |Column: Beverage: booze
        |Column: Conversation: jokes
        |end incident -->
        |
        |""".stripMargin.linesIterator.toList

        Story.create(in) match
            case Left(error) => assert(false, error)
            case Right(story) => {
                assertEquals(
                    story.toCsv(),
                    List(
                        List("Chapter","Incident","Words","Percentage","Cumulative","Food","Beverage","Conversation"),
                        List("","","","total","100%","","",""),
                        List("","breakfast","0","","","cereal bacon eggs","coffee orange juice",""),
                        List("","second breakfast","0","","","croissant","mimosa","gossip"),
                        List("","lunch","0","","","hamburger","",""),
                        List("","dinner","0","","","salad steak potato","water",""),
                        List("","happy hour","0","","","","booze","jokes")
                    )
                )
            }
    }

    test("escaped csv") {

        // this is currently untestable as I don't allow quotes in
        // titles, incident names, column labels, or column values

        val in: List[String] =
        """
        |#   my title
        |
        |<!-- begin incident: one
        |Column: quotes: something in quotes here
        |Column: no quotes: an ordinary line
        |end incident -->
        |
        |""".stripMargin.linesIterator.toList

        Story.create(in) match
            case Left(error) => assert(false, error)
            case Right(story) => {
                assertEquals(
                    story.toCsv(),
                    List(
                        List("Chapter","Incident","Words","Percentage","Cumulative","quotes","no quotes"),
                        List("","","","total","100%","",""),
                        List("","one","0","","","""something in quotes here""","an ordinary line"),
                    )
                )
            }
    }
}
