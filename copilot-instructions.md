# Copilot Instructions for Projects Changed Plugin

## Project Overview

This is a Gradle plugin written in Kotlin that detects which projects have changed based on git history. It helps optimize CI/CD pipelines by enabling selective building and testing of only affected modules in multi-module projects.

## General Guidelines

- **Keep it simple**: Focus on code quality, not excessive documentation
- **Make changes directly**: Update code and tests, don't create summary files
- **Minimal documentation**: Only update README.md and CHANGELOG.md as needed
- **No status reports**: Don't create migration guides, scan reports, or summary files
- **Let the code speak**: Use clear naming and KDoc instead of separate documentation files

## Code Style and Standards

### General Principles
- Follow Kotlin coding conventions and idiomatic Kotlin style
- Use meaningful variable and function names
- Keep functions focused and single-purpose
- Add KDoc comments for public APIs
- Use immutability by default (prefer `val` over `var`)

### Class Decomposition and Single Responsibility

**Decompose non-trivial private methods into separate classes:**

- Extract complex private methods into dedicated classes with focused responsibilities
- Each class should have a single, well-defined purpose
- This improves testability, maintainability, and readability

**Benefits:**
- **Testability**: Separate classes can be unit tested independently
- **Clarity**: Each class has a clear, focused responsibility
- **Reusability**: Extracted classes can be reused in other contexts
- **Maintainability**: Smaller, focused classes are easier to understand and modify

**Example Pattern:**

```kotlin
// ❌ BAD: Large task class with complex private methods
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun execute() {
        val data = complexPrivateMethod1()
        val result = complexPrivateMethod2(data)
        val final = complexPrivateMethod3(result)
    }
    
    private fun complexPrivateMethod1(): Data { /* 50 lines */ }
    private fun complexPrivateMethod2(data: Data): Result { /* 40 lines */ }
    private fun complexPrivateMethod3(result: Result): Final { /* 30 lines */ }
}

// ✅ GOOD: Focused classes with single responsibilities
class DataExtractor(private val logger: Logger) {
    fun extract(): Data { /* 50 lines */ }
}

class ResultProcessor {
    fun process(data: Data): Result { /* 40 lines */ }
}

class FinalComputer {
    fun compute(result: Result): Final { /* 30 lines */ }
}

abstract class MyTask : DefaultTask() {
    private val extractor by lazy { DataExtractor(logger) }
    private val processor by lazy { ResultProcessor() }
    private val computer by lazy { FinalComputer() }
    
    @TaskAction
    fun execute() {
        val data = extractor.extract()
        val result = processor.process(data)
        val final = computer.compute(result)
    }
}
```

**When to Extract:**
- Private method is non-trivial (>20 lines or complex logic)
- Method has multiple responsibilities
- Method would benefit from independent testing
- Method logic could be reused elsewhere

**Naming Convention:**
- Name classes based on their responsibility (e.g., `GitChangedFilesDetector`, `DependencyAnalyzer`)
- Use descriptive names that indicate what the class does
- Prefer noun phrases for classes (e.g., `ProjectFileMapper` not `MapProjectFiles`)

**Project Examples:**
- `GitChangedFilesDetector` - Handles all git operations for detecting changed files
- `ProjectFileMapper` - Maps changed files to their containing Gradle projects
- `DependencyAnalyzer` - Analyzes project dependencies and finds transitive dependents
- `DetectChangedProjectsTask` - Orchestrates the above classes (thin coordinator)

### Testing Standards
- **Use Kotest for all unit tests**
- Follow the FunSpec style for test organization
- Use Kotest matchers (`shouldBe`, `shouldNotBe`, etc.) instead of assertions
- Test names should be descriptive sentences using backticks or strings
- Organize tests logically within `FunSpec` blocks
- Mock external dependencies appropriately

#### Test Structure: Given/When/Then

**All tests must follow the Given/When/Then structure:**

```kotlin
test("should do something when condition is met") {
    // given
    val input = setupTestData()
    val service = MyService()
    
    // when
    val result = service.process(input)
    
    // then
    result shouldBe expectedValue
}
```

**Sections:**
- **`// given`** - Set up test data, create objects, configure mocks (test preconditions)
- **`// when`** - Execute the action being tested (the behavior under test)
- **`// then`** - Assert the expected outcomes (verify results)

**Guidelines:**
- Use lowercase comments: `// given`, `// when`, `// then`
- Each section should be clearly separated
- Given section sets up all prerequisites
- When section should typically be a single action or call
- Then section contains all assertions
- Blank lines between sections are optional but improve readability

**Example:**
```kotlin
test("should identify subproject when files in subproject directory change") {
    // given
    val rootProject = ProjectBuilder.builder().build()
    val subproject = ProjectBuilder.builder()
        .withParent(rootProject)
        .withName("submodule")
        .build()
    val mapper = ProjectFileMapper()
    val changedFiles = setOf("submodule/src/main/kotlin/App.kt")

    // when
    val result = mapper.findProjectsWithChangedFiles(rootProject, changedFiles)

    // then
    result shouldContain ":submodule"
}
```

#### Kotest Test Example

```kotlin
class MyFeatureTest : FunSpec({
    test("should do something when condition is met") {
        // given
        val input = prepareInput()
        
        // when
        val result = doSomething(input)
        
        // then
        result shouldBe expectedValue
    }
    
    context("when specific condition") {
        test("should behave this way") {
            // given
            val setup = prepareSpecificCondition()
            
            // when
            val outcome = performAction(setup)
            
            // then
            outcome shouldBe expected
        }
    }
})
```

### Gradle Plugin Development
- Use `ProcessBuilder` instead of `Runtime.exec()` for external commands
- Always check exit codes when executing external processes
- Log errors and warnings appropriately using Gradle's logging system
- Handle null cases explicitly
- Use Gradle's configuration avoidance APIs where possible

### Error Handling
- Check exit codes for all external process executions
- Log meaningful error messages with context
- Use `?.let {}` for null-safe operations
- Prefer returning empty collections over null for collection types
- Catch and handle specific exceptions rather than generic `Exception`

### Git Operations
- Always verify git repository exists before executing git commands
- Check exit codes from git commands
- Handle cases where branches don't exist
- Filter blank lines from git output
- Use `ProcessBuilder` with proper error stream handling

### Path Handling
- Normalize paths for comparison (handle trailing slashes)
- Use `relativeTo()` for computing relative paths
- Handle both root project and subproject cases explicitly
- Consider cross-platform path separators

## Project Structure

```
src/
├── main/kotlin/com/douglan/projectschanged/
│   ├── ProjectsChangedPlugin.kt        # Main plugin entry point
│   ├── DetectChangedProjectsTask.kt    # Core task implementation
│   └── ProjectsChangedExtension.kt     # Configuration DSL
└── test/kotlin/com/douglan/projectschanged/
    └── ProjectsChangedPluginTest.kt    # Kotest-based tests
```

## Key Design Decisions

### 1. Task Registration
Use `.configure {}` syntax for setting task properties:
```kotlin
project.tasks.register("taskName", TaskClass::class.java).configure {
    group = "category"
    description = "Description"
}
```

### 2. Root Project Handling
Root project should only be marked as changed if files in the root directory (not in subprojects) are changed:
```kotlin
if (normalizedProjectPath.isEmpty()) {
    val isInSubproject = project.rootProject.subprojects.any { sub ->
        val subPath = sub.projectDir.relativeTo(project.rootDir).path
        file.startsWith("$subPath/")
    }
    if (!isInSubproject) {
        affectedProjects.add(subproject.path)
    }
}
```

### 3. Git Command Execution
Always use `ProcessBuilder` with proper error handling:
```kotlin
val process = ProcessBuilder(*command)
    .directory(workingDir)
    .redirectErrorStream(true)
    .start()

val exitCode = process.waitFor()
if (exitCode == 0) {
    // Process successful output
} else {
    // Log error with exit code and output
    logger.warn("Command failed with exit code $exitCode: $errorOutput")
}
```

## Dependencies

### Runtime
- Kotlin stdlib
- Gradle API (provided)

### Testing
- Kotest runner (JUnit 5)
- Kotest assertions
- Kotest property testing
- Gradle TestKit (for plugin testing)

## Common Tasks

### Running Tests
```bash
./gradlew test
```

### Building the Plugin
```bash
./gradlew build
```

### Publishing Locally
```bash
./gradlew publishToMavenLocal
```

## When Adding New Features

1. **Write tests first** using Kotest FunSpec style
2. **Add configuration options** to `ProjectsChangedExtension` if needed
3. **Update documentation** in README.md
4. **Add entries** to CHANGELOG.md
5. **Consider backward compatibility** when changing public APIs
6. **Validate with integration tests** if touching git operations

## Code Review Checklist

Before submitting changes, verify:
- [ ] All tests pass (`./gradlew test`)
- [ ] Code compiles without warnings
- [ ] New features have Kotest tests
- [ ] Public APIs have KDoc comments
- [ ] Error cases are handled appropriately
- [ ] Git operations use ProcessBuilder with exit code checks
- [ ] Paths are normalized for comparison
- [ ] README.md is updated if user-facing behavior changes
- [ ] CHANGELOG.md has an entry for the change (version history only)
- [ ] No unnecessary documentation files were created

## Anti-Patterns to Avoid

❌ **Don't use `Runtime.exec()`** - Use `ProcessBuilder` instead
❌ **Don't ignore exit codes** - Always check command results
❌ **Don't use JUnit** - Use Kotest for all tests
❌ **Don't use `assertEquals`** - Use Kotest matchers like `shouldBe`
❌ **Don't hardcode paths** - Use Gradle's path APIs
❌ **Don't swallow exceptions** - Log them with context
❌ **Don't forget null checks** - Use safe calls or explicit null handling

## Useful Kotest Matchers

```kotlin
// Equality
result shouldBe expected
result shouldNotBe unexpected

// Null checks
value shouldNotBe null
value shouldBe null

// Type checks
instance.shouldBeInstanceOf<Type>()

// Collections
list shouldContain element
list.size shouldBe 3
list shouldBe emptyList()

// Boolean
condition shouldBe true
condition shouldBe false

// Strings
text shouldContain "substring"
text shouldStartWith "prefix"
text shouldEndWith "suffix"
```

## Documentation Standards

- Use KDoc for all public classes, functions, and properties
- Include `@param` and `@return` tags for non-obvious parameters/returns
- Add usage examples in KDoc for complex APIs
- Keep inline comments minimal - prefer self-documenting code
- Update README.md for user-facing changes

### What NOT to Create

**Do NOT create these types of documentation files:**
- Migration guides (e.g., KOTEST_MIGRATION.md)
- Scan reports (e.g., PROJECT_SCAN_REPORT.md)
- Fix summaries (e.g., FIXES_SUMMARY.md)
- Quick reference guides (e.g., QUICK_REFERENCE.md)
- Status reports or completion summaries

**Only maintain these core documentation files:**
- `README.md` - User-facing documentation, usage examples, getting started
- `CHANGELOG.md` - Version history and change tracking
- `copilot-instructions.md` - This file, coding standards for developers

All other documentation should be in code comments (KDoc) or inline explanations.

## Version Management

- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Update version in `build.gradle.kts`
- Document changes in CHANGELOG.md
- Tag releases in git

## Additional Resources

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Kotest Documentation](https://kotest.io/)
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [ProcessBuilder JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/lang/ProcessBuilder.html)
