package io.github.doughawley.monoreporelease.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SemanticVersionTest : FunSpec({

    test("should parse version with v prefix") {
        // given
        val raw = "v1.2.3"
        // when
        val result = SemanticVersion.parse(raw)
        // then
        result shouldBe SemanticVersion(1, 2, 3)
    }

    test("should parse version without v prefix") {
        // given
        val raw = "1.2.3"
        // when
        val result = SemanticVersion.parse(raw)
        // then
        result shouldBe SemanticVersion(1, 2, 3)
    }

    test("should parse version with zeros") {
        // given
        val raw = "0.0.0"
        // when
        val result = SemanticVersion.parse(raw)
        // then
        result shouldBe SemanticVersion(0, 0, 0)
    }

    test("should return null for invalid format") {
        // given / when / then
        SemanticVersion.parse("not-a-version") shouldBe null
        SemanticVersion.parse("1.2") shouldBe null
        SemanticVersion.parse("1.2.3.4") shouldBe null
        SemanticVersion.parse("") shouldBe null
        SemanticVersion.parse("a.b.c") shouldBe null
    }

    test("should return null for empty string") {
        SemanticVersion.parse("").shouldBeNull()
    }

    test("bump MAJOR resets minor and patch to zero") {
        // given
        val version = SemanticVersion(1, 2, 3)
        // when
        val result = version.bump(Scope.MAJOR)
        // then
        result shouldBe SemanticVersion(2, 0, 0)
    }

    test("bump MINOR resets patch to zero") {
        // given
        val version = SemanticVersion(1, 2, 3)
        // when
        val result = version.bump(Scope.MINOR)
        // then
        result shouldBe SemanticVersion(1, 3, 0)
    }

    test("bump PATCH increments patch only") {
        // given
        val version = SemanticVersion(1, 2, 3)
        // when
        val result = version.bump(Scope.PATCH)
        // then
        result shouldBe SemanticVersion(1, 2, 4)
    }

    test("bump from 0.0.0 MAJOR gives 1.0.0") {
        SemanticVersion(0, 0, 0).bump(Scope.MAJOR) shouldBe SemanticVersion(1, 0, 0)
    }

    test("bump from 0.0.0 MINOR gives 0.1.0") {
        SemanticVersion(0, 0, 0).bump(Scope.MINOR) shouldBe SemanticVersion(0, 1, 0)
    }

    test("comparison: higher major wins") {
        // given
        val lower = SemanticVersion(1, 9, 9)
        val higher = SemanticVersion(2, 0, 0)
        // then
        higher shouldBeGreaterThan lower
        lower shouldBeLessThan higher
    }

    test("comparison: higher minor wins when major equal") {
        val lower = SemanticVersion(1, 2, 9)
        val higher = SemanticVersion(1, 3, 0)
        higher shouldBeGreaterThan lower
    }

    test("comparison: higher patch wins when major and minor equal") {
        val lower = SemanticVersion(1, 2, 3)
        val higher = SemanticVersion(1, 2, 4)
        higher shouldBeGreaterThan lower
    }

    test("equal versions compare as equal") {
        val v1 = SemanticVersion(1, 2, 3)
        val v2 = SemanticVersion(1, 2, 3)
        v1.compareTo(v2) shouldBe 0
    }

    test("max selection picks correct version from list") {
        // given
        val versions = listOf(
            SemanticVersion(0, 1, 0),
            SemanticVersion(0, 2, 0),
            SemanticVersion(0, 1, 5),
            SemanticVersion(1, 0, 0)
        )
        // when
        val max = versions.max()
        // then
        max shouldBe SemanticVersion(1, 0, 0)
    }

    test("toString returns major.minor.patch without v prefix") {
        // given
        val version = SemanticVersion(1, 2, 3)
        // when
        val result = version.toString()
        // then
        result shouldBe "1.2.3"
    }

    test("toString for 0.0.0") {
        SemanticVersion(0, 0, 0).toString() shouldBe "0.0.0"
    }
})
