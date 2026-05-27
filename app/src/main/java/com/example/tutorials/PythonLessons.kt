package com.example.tutorials

data class Lesson(
    val id: String,
    val title: String,
    val difficulty: String,
    val story: String,
    val codeStub: String,
    val hint: String,
    val xpReward: Int,
    val validationCheck: (outputs: List<String>, variables: Map<String, Any?>) -> Pair<Boolean, String>
)

object PythonLessons {
    val playlist = listOf(
        Lesson(
            id = "lesson_one",
            title = "1. Hello, World!",
            difficulty = "Beginner",
            story = "Welcome to python! Your very first task is to write a script that outputs a friendly greeting to the terminal. In Python, we do this using the print() function. Print the text: 'Welcome to Python' exactly.",
            codeStub = "# Challenge: Output 'Welcome to Python' to the console\n",
            hint = "Use print(\"Welcome to Python\")",
            xpReward = 100,
            validationCheck = { outputs, _ ->
                val success = outputs.any { it.trim().equals("Welcome to Python", ignoreCase = true) }
                if (success) {
                    Pair(true, "First step achieved! You successfully printed a message offline!")
                } else {
                    Pair(false, "To pass, your output must contain exactly: 'Welcome to Python'. Currently printed lines: ${outputs.joinToString(", ")}")
                }
            }
        ),
        Lesson(
            id = "lesson_two",
            title = "2. Variables & Constants",
            difficulty = "Beginner",
            story = "Variables are boxes that hold values. Create an integer variable named 'a' with a value of 15, another named 'b' with a value of 20. Then print the sum of 'a' and 'b' using: print(a + b)",
            codeStub = "# Create variables 'a' & 'b'\n# Print their sum\n",
            hint = "a = 15\nb = 20\nprint(a + b)",
            xpReward = 150,
            validationCheck = { outputs, _ ->
                val success = outputs.any { it.trim() == "35" }
                if (success) {
                    Pair(true, "Correct! Your variable arithmetic worked perfectly.")
                } else {
                    Pair(false, "The sum of 15 and 20 (which is 35) was not output to the console. Make sure to print it.")
                }
            }
        ),
        Lesson(
            id = "lesson_three",
            title = "3. Smart Decisions (if/else)",
            difficulty = "Intermediate",
            story = "If statements help the computer make decisions. Create a variable named 'age' and assign it 18. Write an 'if' condition: if age >= 18, print 'Eligible'. Otherwise (else), print 'Not Eligible'.",
            codeStub = "# Save 18 to age\nage = 18\n# Write if-else condition to print 'Eligible' or 'Not Eligible'\n",
            hint = "if age >= 18:\n    print(\"Eligible\")\nelse:\n    print(\"Not Eligible\")",
            xpReward = 200,
            validationCheck = { outputs, _ ->
                val hasEligible = outputs.any { it.trim().equals("Eligible", ignoreCase = true) }
                val hasNotEligible = outputs.any { it.trim().equals("Not Eligible", ignoreCase = true) }
                if (hasEligible && !hasNotEligible) {
                    Pair(true, "Fantastic! The conditional evaluation was correct.")
                } else {
                    Pair(false, "Did you write the correct check? Since age is 18, it should print 'Eligible' and avoid 'Not Eligible'.")
                }
            }
        ),
        Lesson(
            id = "lesson_four",
            title = "4. Looping Stars",
            difficulty = "Intermediate",
            story = "Loops repeat actions. Write a simple Python 'for i in range(5):' loop, and print the loop counter 'i' in each turn to output numbers 0 through 4.",
            codeStub = "# Use range(5) loop to print numbers 0 to 4\n",
            hint = "for i in range(5):\n    print(i)",
            xpReward = 250,
            validationCheck = { outputs, _ ->
                val numbers = outputs.mapNotNull { it.trim().toIntOrNull() }
                val expected = listOf(0, 1, 2, 3, 4)
                if (numbers.containsAll(expected)) {
                    Pair(true, "Success! You created a responsive structural loop.")
                } else {
                    Pair(false, "Let's align. The output lines should print integers 0, 1, 2, 3, 4 sequentially.")
                }
            }
        ),
        Lesson(
            id = "lesson_five",
            title = "5. Interactive Lists",
            difficulty = "Advanced",
            story = "Lists allow keeping collections of data. Create a variable named 'colors' holding a list of values: ['red', 'green', 'blue']. Append a new color 'yellow' using 'colors.append(\"yellow\")' and print the final colors list.",
            codeStub = "# Create list holding 'red', 'green', 'blue'\n# Append 'yellow'\n# Print colors\n",
            hint = "colors = ['red', 'green', 'blue']\ncolors.append('yellow')\nprint(colors)",
            xpReward = 300,
            validationCheck = { outputs, _ ->
                val success = outputs.any { it.contains("red") && it.contains("green") && it.contains("blue") && it.contains("yellow") }
                if (success) {
                    Pair(true, "Sensational! You completed all Python essentials!")
                } else {
                    Pair(false, "Your printed colors list does not seem to contain red, green, blue, and yellow.")
                }
            }
        )
    )
}
