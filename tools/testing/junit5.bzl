"""java_tests: auto-discover and run JUnit 5 tests with Gradle-like experience.

Usage in any service BUILD.bazel — this is the only declaration needed:

    load("//tools/testing:junit5.bzl", "java_tests")

    java_tests(
        srcs = glob(["src/test/java/**/*.java"]),
        deps = [":example_service_lib"],
    )

Produces:
  - :FooTest, :BarTest, …  — one target per class, individually cached and parallelised
  - :tests                 — test_suite that runs all of them at once

Adding a new test file requires no BUILD changes.
"""

load("@contrib_rules_jvm//java:defs.bzl", "java_junit5_test")

def java_tests(srcs, deps = [], size = "small", **kwargs):
    test_names = []

    for src in srcs:
        # src/test/java/com/example/FooTest.java → com.example.FooTest / FooTest
        parts = src.split("src/test/java/")
        class_name = parts[-1].replace("/", ".").replace(".java", "")
        test_name = src.split("/")[-1].replace(".java", "")
        test_names.append(test_name)

        java_junit5_test(
            name = test_name,
            srcs = [src],
            test_class = class_name,
            size = size,
            deps = deps + ["@maven//:org_junit_jupiter_junit_jupiter_api"],
            runtime_deps = [
                "@maven//:org_junit_jupiter_junit_jupiter_engine",
                "@maven//:org_junit_platform_junit_platform_commons",
                "@maven//:org_junit_platform_junit_platform_engine",
                "@maven//:org_junit_platform_junit_platform_launcher",
                "@maven//:org_junit_platform_junit_platform_reporting",
            ],
            **kwargs
        )

    if test_names:
        native.test_suite(
            name = "tests",
            tests = test_names,
        )
