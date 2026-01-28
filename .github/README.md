# GitHub Configuration

This directory contains GitHub-specific configuration files for the project.

## Workflows

### CI Workflow (`.github/workflows/ci.yml`)

The main continuous integration pipeline that runs on every push and pull request.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Manual workflow dispatch

**Jobs:**

1. **Build and Test**
   - Runs on Ubuntu, macOS, and Windows
   - Tests with Java 11, 17, and 21
   - Executes unit and functional tests
   - Uploads test reports as artifacts
   - Uploads plugin JAR (Ubuntu + Java 17 only)

2. **Code Quality**
   - Runs code style checks
   - Validates plugin descriptor

3. **Test Local Publishing**
   - Verifies the plugin can be published to Maven Local
   - Ensures artifact structure is correct

### Release Workflow (`.github/workflows/release.yml`)

Automates the release process when a version tag is pushed.

**Triggers:**
- Tags matching `v*.*.*` pattern (e.g., `v1.0.0`)
- Manual workflow dispatch with version input

**Process:**
1. Builds the plugin
2. Runs all tests
3. Creates a GitHub release
4. Attaches JAR files and changelog

**To create a release:**
```bash
# Update version in build.gradle.kts and CHANGELOG.md
git add build.gradle.kts CHANGELOG.md
git commit -m "Release v1.0.0"
git tag v1.0.0
git push origin main --tags
```

**Publishing to Gradle Plugin Portal:**
- Currently commented out in the workflow
- To enable: uncomment the `publish-plugin` job
- Add these repository secrets:
  - `GRADLE_PUBLISH_KEY`
  - `GRADLE_PUBLISH_SECRET`

## Dependabot

Automated dependency updates are configured in `dependabot.yml`:
- Weekly updates for GitHub Actions
- Weekly updates for Gradle dependencies
- Automatic pull requests with proper labels

## Issue Templates

- **Bug Report** (`ISSUE_TEMPLATE/bug_report.md`): For reporting bugs
- **Feature Request** (`ISSUE_TEMPLATE/feature_request.md`): For suggesting new features

## Pull Request Template

The PR template (`pull_request_template.md`) provides a checklist to ensure:
- Proper description of changes
- Tests are included
- Documentation is updated
- Code review checklist is followed

## Required Repository Settings

For optimal CI/CD operation:

1. **Branch Protection Rules** (recommended for `main`):
   - Require pull request reviews
   - Require status checks to pass (CI workflow)
   - Require branches to be up to date

2. **Repository Secrets** (for publishing):
   - `GRADLE_PUBLISH_KEY`: Your Gradle Plugin Portal API key
   - `GRADLE_PUBLISH_SECRET`: Your Gradle Plugin Portal secret

3. **Actions Permissions**:
   - Allow GitHub Actions to create pull requests (for Dependabot)
   - Allow write access for creating releases

## Monitoring

- **Test Reports**: Check the "Actions" tab for detailed test results
- **Artifacts**: Download test reports and JARs from workflow runs
- **Release Notes**: Auto-generated from CHANGELOG.md on release
