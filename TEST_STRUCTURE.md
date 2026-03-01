# Test Structure

This project uses separate source sets for different types of tests.

## Directory Structure

```
monorepo-build-release-plugin/src/test/
├── unit/
│   └── kotlin/
│       └── io/github/doughawley/monorepo/
│           ├── build/       # Change detection unit tests
│           ├── release/     # Release/versioning unit tests
│           └── git/         # GitCommandExecutor tests
├── integration/
│   └── kotlin/
│       └── io/github/doughawley/monorepo/release/git/
│           └── ... (integration tests against a real git backend)
└── functional/
    └── kotlin/
        └── io/github/doughawley/monorepo/
            ├── build/       # Change detection functional tests
            └── release/     # Release functional tests
```

## Test Types

### Unit Tests (`src/test/unit/`)
Unit tests focus on testing individual components in isolation:
- Domain model tests (ProjectMetadata, MonorepoProjects)
- Service/utility class tests
- Fast execution
- No external dependencies

### Integration Tests (`src/test/integration/`)
Integration tests verify components against a real git backend without Gradle TestKit:
- Real git repository operations
- No mocking of git commands
- Faster than functional tests

### Functional Tests (`src/test/functional/`)
Functional tests verify end-to-end functionality:
- Plugin integration tests
- Full workflow tests
- Uses Gradle TestKit to create real test projects
- Tests real-world scenarios with actual git operations

**Current Functional Tests:**
- `MonorepoPluginDetectionFunctionalTest.kt` - Tests the `printChangedProjectsFromBranch` task
- `BuildChangedProjectsFunctionalTest.kt` - Tests the `buildChangedProjectsFromBranch` task
- `MonorepoPluginConfigurationTest.kt` - Configuration and exclude pattern scenarios
- `PrintChangedProjectsFromRefFunctionalTest.kt` - Tests the ref-mode task
- `WriteChangedProjectsFromRefFunctionalTest.kt` - Tests the write task
- `ReleaseChangedProjectsFunctionalTest.kt` - Tests the `releaseChangedProjects` task
- `ReleaseTaskFunctionalTest.kt` - Release task edge cases

**Test Utilities:**
- `TestProjectBuilder.kt` - Helper for creating test Gradle projects
  - Programmatically builds multi-module projects
  - Configures dependencies
  - Initializes git repositories
  - Provides git operations (commit, stage, modify files)
  - Runs Gradle tasks with TestKit
  - Parses build output for assertions

## Running Tests

### Run All Tests
```bash
./gradlew :monorepo-build-release-plugin:check
```
This runs unit, integration, and functional tests in order.

### Run Only Unit Tests
```bash
./gradlew :monorepo-build-release-plugin:unitTest
```

### Run Only Integration Tests
```bash
./gradlew :monorepo-build-release-plugin:integrationTest
```

### Run Only Functional Tests
```bash
./gradlew :monorepo-build-release-plugin:functionalTest
```

### Run Tests in Specific Order
Integration tests run after unit tests; functional tests run last when using `check`.

## Adding New Tests

### Adding a Unit Test
Create your test file under the appropriate package in:
```
monorepo-build-release-plugin/src/test/unit/kotlin/io/github/doughawley/monorepo/
```

### Adding an Integration Test
Create your test file under the appropriate package in:
```
monorepo-build-release-plugin/src/test/integration/kotlin/io/github/doughawley/monorepo/
```

### Adding a Functional Test
Create your test file under the appropriate package in:
```
monorepo-build-release-plugin/src/test/functional/kotlin/io/github/doughawley/monorepo/
```

## Test Configuration

All test types:
- Use JUnit Platform (Kotest)
- Run with `outputs.upToDateWhen { false }` to always execute
- Have access to main source set output

## Benefits of Separation

1. **Faster Feedback**: Run quick unit tests first
2. **Clear Organization**: Easy to find tests by type
3. **Selective Execution**: Run only the tests you need
4. **Better CI**: Can run unit tests in parallel, functional tests separately
5. **Clearer Intent**: Test names and locations indicate their purpose
