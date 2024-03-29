package furhatos.app.agent.flow.recipes

import furhatos.app.agent.current_user
import furhatos.app.agent.flow.Parent
import furhatos.app.agent.flow.main.DayPreference
import furhatos.app.agent.flow.memory.data.Cuisine
import furhatos.app.agent.flow.memory.data.Ingredient
import furhatos.app.agent.flow.memory.data.Meal
import furhatos.app.agent.userUpdates
import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.nlu.common.No
import furhatos.nlu.common.Yes
import java.time.LocalDate

val BASE_URL = "https://api.spoonacular.com" // Spoonacular API url
val API_KEY = "9194c2c9ed884032b824fbbde7714ab2" // Key to free account
//e9eeb0d76f024efcaf7cd32ae444c899
//9194c2c9ed884032b824fbbde7714ab2
val TIMEOUT = 5000 // 5 seconds
var recommendations: MutableList<Meal> = mutableListOf()

// Start state containing everything except the query to the API
val Recommendation : State = state(Parent) {
    onEntry {
        current_user.last_step = "recommendation"
        try {
            recommendations = call(query("recipes", "complexSearch")) as MutableList<Meal>
            if (recommendations.isEmpty()) {
                print("Specific")
                goto(toSpecific)
            } else {
                random(
                    {furhat.say("I have some ideas on which recipes you might like.")},
                    {furhat.say("I found some recipes for you.")},
                    {furhat.say("I have some recipes that match your preferences.")}
                )
                random(
                    {furhat.ask("Would you like to know?")},
                    {furhat.ask("Should I tell?")},
                    {furhat.ask("Are you curious?")}
                )
            }
        } catch(e: Exception) {
            furhat.say("I'm sorry. Something went wrong with finding a recipe.")
            val bool = furhat.askYN("Do you want to try again?")
            if(bool!!) {
               reentry()
            } else {
                // current_user.last_step = "greeting", moved to foodjoke
                goto(FoodJoke)
            }
        }
    }

    onResponse<Yes> {
        goto(GiveRecommendation)
    }

    onResponse<No> {
        furhat.say("Alright.")
        // current_user.last_step = "greeting", moved to foodjoke
        goto(FoodJoke)
    }
}

val GiveRecommendation = state(Parent) {
    onEntry {
        current_user.last_step = "give_recommendation"
        if (recommendations.isNotEmpty()) {
            val recipe = recommendations.first()
            print(recipe.javaClass.name)
            recommendations = if (recommendations.size >= 2) {
                recommendations.drop(1) as MutableList<Meal>
            } else {
                mutableListOf()
            }

            // Propose recipe
            furhat.say("I think you might like " + recipe.name)

            // Provide additional information
            val moreInfo = furhat.askYN("Would you like some more information?")
            if (moreInfo!! && moreInfo) {
                val string = recipe.summary.replace(Regex("(<b>)|(</b>)|(<a.*?>)|(</a>)"), "")
                furhat.say(string)
                furhat.say("It takes " + recipe.prepTime + " minutes to cook.")
            } else {
                goto(EvaluateRecommendation(recipe))
            }

            goto(EvaluateRecommendation(recipe))
        } else {
            furhat.say("Unfortunately, I'm out of recommended recipes.")
            // current_user.last_step = "greeting", moved to foodjoke
            goto(FoodJoke)
        }
    }
}

fun EvaluateRecommendation(recipe: Meal) : State = state(Parent) {
    onEntry {
        random(
            {furhat.ask("Do you like it?")},
            {furhat.ask("Are you excited about this recipe?")},
            {furhat.ask("Will you enjoy this meal?")}
        )
    }

    onResponse<Yes> {
        furhat.say("Awesome!")
        recipe.last_selected = LocalDate.now().toString()
        recipe.likes += 2
        addRecipeToUser(recipe)
        goto(EndRecommendation)
    }

    onResponse<No> {
        val another = furhat.askYN("Too bad, would you like another recipe?")
        addRecipeToUser(recipe)
        userUpdates.updateMeal(-2, recipe, current_user)
        if (another!! && another) {
            if (recommendations.isEmpty()) {
                goto(toSpecific)
            } else {
                goto(GiveRecommendation)
            }
        } else {
            furhat.say("Okay")
            goto(FoodJoke)
        }
    }
}

val toSpecific : State = state(Parent) {
    onEntry {
        current_user.last_step = "toSpecific"
        furhat.say("Sorry I could not comply with all requirements.")
        current_user.left_overs.clear()
        recommendations = call(query("recipes", "complexSearch")) as MutableList<Meal>
        if (!recommendations.isEmpty()) {
            furhat.say("I did find some recipes but was not able to fit in all you left overs.")
            random(
                {furhat.ask("Would you like to know?")},
                {furhat.ask("Should I tell?")},
                {furhat.ask("Are you curious?")}
            )
        } else {
            recommendations = call(query("recipes", "complexSearch", "cuis")) as MutableList<Meal>
            if (!recommendations.isEmpty()) {
                furhat.say("There are no recipes that use your left overs, that are ${current_user.prefered_cuisine}")
                furhat.say("I did find some recipes that fit your diet and time constraints.")

                random(
                    { furhat.ask("Would you like to know?") },
                    { furhat.ask("Should I tell?") },
                    { furhat.ask("Are you curious?") }
                )
            }
        }

        furhat.say("I was not able to find a ${current_user.preferred_meal_type} that met your diet and time constraints.")
        val newEval = furhat.askYN(random(
            "Do you want to change your preferences?",
            "Is it possible to change your demands?"
            )
        )
        if (newEval!! && newEval) {
            goto(DayPreference)
        } else {
            furhat.say("Okay")
            goto(FoodJoke)
        }

    }

    onResponse<Yes> {
        goto(GiveRecommendation)
    }

    onResponse<No> {
        furhat.say("Okay")
        goto(FoodJoke)
    }
}

val EndRecommendation : State = state(Parent) {
    onEntry {
        random(
            {furhat.say("Enjoy your meal!")},
            {furhat.say("Bon appetite!")}
        )
        // current_user.last_step = "greeting", moved this to FoodJoke.
        goto(FoodJoke)
    }
}

fun addRecipeToUser(recipe: Meal){
    current_user.meals = userUpdates.addMeal(recipe, current_user.meals)
    for (i in recipe.ingredients){
        current_user.ingredients = userUpdates.addIngredient(Ingredient(i, 0), current_user.ingredients)
    }
    for (j in recipe.cuisines){
        current_user.cuisines = userUpdates.addCuisine(Cuisine(j, 0), current_user.cuisines)
    }
}
fun main(args: Array<String>) {
    val recipe = "If you want to add more <b>Mediterranean<\\/b> recipes to your recipe box, Broccolini Quinoa Pilaf might be a recipe you should try. One portion of this dish contains around <b>20g of protein<\\/b>, <b>31g of fat<\\/b>, and a total of <b>625 calories<\\/b>. This recipe serves 2 and costs $4.14 per serving. A few people really liked this main course. 95 people have made this recipe and would make it again. Head to the store and pick up quinoa, garlic clove, olive oil, and a few other things to make it today. From preparation to the plate, this recipe takes roughly <b>30 minutes<\\/b>. It is a good option if you're following a <b>gluten free, dairy free, lacto ovo vegetarian, and vegan<\\/b> diet. It is brought to you by Pick Fresh Foods. All things considered, we decided this recipe <b>deserves a spoonacular score of 98%<\\/b>. This score is awesome."
    var new = recipe.replace(Regex("[(<b>)(<\\\\/b>]"), "")
    print(new)
}
