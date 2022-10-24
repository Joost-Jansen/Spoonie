package furhatos.app.agent.nlu

import furhatos.nlu.Intent
import furhatos.nlu.TextGenerator
import furhatos.util.Language
import furhatos.nlu.common.PersonName


open class TellPersonalInformation : Intent(), TextGenerator {
    var diets: ListOfDiets? = null
    var allergies: ListOfAllergies? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf(
            listOf(
                "I am following a @diets diet and I am allergic to @allergies",
                "I am allergic to @allergies and I am following a @diets diet",
                "I am on the @diets diet and my allergies are @allergies",
                "My allergies are @allergies and I am on the @diets diet"
            ),
            getDietExamples(),
            getAllergyExamples()
        ).flatten()
    }

    override fun toText(lang : Language) : String {
        return generate(lang, "[allergic to $allergies] [following the $diets diet]")
    }

    override fun toString(): String {
        return toText()
    }
}

class TellDiets : Intent() {
    var diets: ListOfDiets? = null

    override fun getExamples(lang: Language): List<String> {
        return getDietExamples()
    }
}

fun getDietExamples(): List<String> {
    return listOf(
        "I am following a @diets diet",
        "I am following the @diets diet",
        "I am on a @diets diet",
        "I am on the @diets diet",
        "@diets diet",
        "@diets"
    )
}

class TellAllergies : Intent() {
    var allergies: ListOfAllergies? = null

    override fun getExamples(lang: Language): List<String> {
        return getAllergyExamples()
    }
}

fun getAllergyExamples(): List<String> {
    return listOf(
        "I am allergic to @allergies",
        "I am intolerant to @allergies",
        "my allergy is @allergies",
        "my allergies are @allergies",
        "my intolerance is @allergies",
        "my intolerances are @allergies",
        "@allergies allergy",
        "@allergies intolerance",
        "@allergies intolerant",
        "@allergies"
    )
}

open class RemovePersonalInformation : Intent(), TextGenerator {
    var diets: ListOfDiets? = null
    var allergies: ListOfAllergies? = null

    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "not following a @diets diet",
            "not on the @diets diet",
            "not allergic to @allergies",
            "not intolerant to @allergies",
            "@diets is not my diet",
            "@diets are not my diets",
            "@allergies is not my allergy",
            "@allergies are not my allergies",
        )
    }

    override fun toText(lang : Language) : String {
        return generate(lang, "[not allergic to $allergies] [not following the $diets diet]")
    }

    override fun toString(): String {
        return toText()
    }
}
class UserIdentification(
    val name : PersonName? = null
) : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf("@name", "I am @name", "they call me @name", "my name is @name", "this is @name", "@name is my name", "@name, is what I am")
    }
//
//    override fun getNegativeExamples(lang: Language?): List<String> {
//        return listOf("I am not @name", "they don't call me @name", "my name is not @name", "this is not @name")
//    }
}

class WrongPerson(
    val name : PersonName? = null
) : Intent() {

    override fun getExamples(lang: Language): List<String> {
        return listOf("Sorry, I am not @name", "my name is not @name", "you are talking to the wrong person", "you have the wrong person", "You have the wrong name", "this is not @name", "you have me mistaken by someone else")
    }
}

class positiveFlavourMeal(var flavours : flavourListPostive? = null) : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf("I liked the taste", "I like the taste",  "@flavours", "I like the @flavours", "I loved the @flavours",
            "I think it was @flavours", "I like the @flavours flavour", "It was @flavours","What I liked was that @flavours",
            "It was @flavours", "It had a @flavours taste"
        )
    }
}


class negativeFlavourMeal(var flavours : flavourListNegative? = null) : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf("I didn't like the taste", "@flavours", "I did not like the @flavours", "I found the @flavours not nice",
            "I think it was @flavours", "I like the @flavours flavour", "It was @flavours", "What I didn't like was that @flavours",
            "It was @flavours", "It had a @flavours taste"  )
    }
}
