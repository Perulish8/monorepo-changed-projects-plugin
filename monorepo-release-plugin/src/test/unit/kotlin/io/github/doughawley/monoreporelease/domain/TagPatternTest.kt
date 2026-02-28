package io.github.doughawley.monoreporelease.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class TagPatternTest : FunSpec({

    test("formatTag produces globalPrefix/projectPrefix/vVersion") {
        // given
        val version = SemanticVersion(1, 2, 0)
        // when
        val tag = TagPattern.formatTag("release", "api", version)
        // then
        tag shouldBe "release/api/v1.2.0"
    }

    test("formatTag with custom global prefix") {
        val version = SemanticVersion(0, 1, 0)
        TagPattern.formatTag("custom-prefix", "service", version) shouldBe "custom-prefix/service/v0.1.0"
    }

    test("formatReleaseBranch produces release/projectPrefix/vMajor.Minor.x") {
        // given
        val version = SemanticVersion(1, 2, 0)
        // when
        val branch = TagPattern.formatReleaseBranch("api", version)
        // then
        branch shouldBe "release/api/v1.2.x"
    }

    test("formatReleaseBranch for v0.1.0") {
        TagPattern.formatReleaseBranch("app", SemanticVersion(0, 1, 0)) shouldBe "release/app/v0.1.x"
    }

    test("deriveProjectTagPrefix strips leading colon for single-level path") {
        // given
        val gradlePath = ":api"
        // when
        val prefix = TagPattern.deriveProjectTagPrefix(gradlePath)
        // then
        prefix shouldBe "api"
    }

    test("deriveProjectTagPrefix replaces inner colons with dashes for nested path") {
        // given
        val gradlePath = ":services:auth"
        // when
        val prefix = TagPattern.deriveProjectTagPrefix(gradlePath)
        // then
        prefix shouldBe "services-auth"
    }

    test("deriveProjectTagPrefix for three-level path") {
        TagPattern.deriveProjectTagPrefix(":a:b:c") shouldBe "a-b-c"
    }

    test("parseVersionFromTag round-trips with formatTag") {
        // given
        val version = SemanticVersion(2, 3, 4)
        val tag = TagPattern.formatTag("release", "app", version)
        // when
        val parsed = TagPattern.parseVersionFromTag(tag, "release", "app")
        // then
        parsed shouldBe version
    }

    test("parseVersionFromTag returns null for wrong prefix") {
        // given
        val tag = "other/app/v1.0.0"
        // when
        val result = TagPattern.parseVersionFromTag(tag, "release", "app")
        // then
        result.shouldBeNull()
    }

    test("parseVersionFromTag returns null for wrong project prefix") {
        val tag = "release/other-project/v1.0.0"
        TagPattern.parseVersionFromTag(tag, "release", "app").shouldBeNull()
    }

    test("parseVersionFromTag returns null for malformed version") {
        val tag = "release/app/vnotaversion"
        TagPattern.parseVersionFromTag(tag, "release", "app").shouldBeNull()
    }

    test("isReleaseBranch returns true for valid release branch") {
        TagPattern.isReleaseBranch("release/api/v1.2.x") shouldBe true
    }

    test("isReleaseBranch returns true for nested project release branch") {
        TagPattern.isReleaseBranch("release/services-auth/v0.1.x") shouldBe true
    }

    test("isReleaseBranch returns false for main") {
        TagPattern.isReleaseBranch("main") shouldBe false
    }

    test("isReleaseBranch returns false for master") {
        TagPattern.isReleaseBranch("master") shouldBe false
    }

    test("isReleaseBranch returns false for feature branch") {
        TagPattern.isReleaseBranch("feature/my-feature") shouldBe false
    }

    test("isReleaseBranch returns false for a release tag (not a branch pattern)") {
        // "release/api/v1.2.0" has no .x suffix — that's a tag, not a branch
        TagPattern.isReleaseBranch("release/api/v1.2.0") shouldBe false
    }

    test("parseVersionLineFromBranch extracts major and minor") {
        // given
        val branch = "release/api/v0.2.x"
        // when
        val (major, minor) = TagPattern.parseVersionLineFromBranch(branch)
        // then
        major shouldBe 0
        minor shouldBe 2
    }

    test("parseVersionLineFromBranch for v1.10.x") {
        val (major, minor) = TagPattern.parseVersionLineFromBranch("release/app/v1.10.x")
        major shouldBe 1
        minor shouldBe 10
    }
})
