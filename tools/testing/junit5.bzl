"""JUnit 5 test macros for Bazel.

Preferred usage — zero BUILD edits when adding new test files:

    load("//tools/testing:junit5.bzl", "java_tests")

    java_tests(
        srcs = glob(["src/test/java/**/*.java"]),
        deps = [":example_service_lib"],
    )

Explicit usage (non-standard layouts, or when you need fine-grained control):

    load("//tools/testing:junit5.bzl", "junit5_test")

    junit5_test(
        name = "FooTest",
        srcs = ["src/test/java/com/example/FooTest.java"],
        test_class = "com.example.FooTest",
        deps = [":example_service_lib"],
    )
"""

load("@rules_java//java:defs.bzl", "java_test")

def junit5_test(name, srcs, test_class, deps = [], size = "small", **kwargs):
    """One junit5_test per test class. Results are cached and run in parallel by Bazel."""
    java_test(
        name = name,
        srcs = srcs,
        test_class = test_class,
        use_testrunner = False,
        main_class = "Junit5Runner",
        args = [test_class],
        size = size,
        deps = deps + [
            "//tools/testing:junit5_runner",
            "@maven//:org_junit_jupiter_junit_jupiter_api",
        ],
        **kwargs
    )

def java_tests(srcs, deps = [], size = "small", **kwargs):
    """Auto-create one junit5_test per file in srcs.

    Class names are inferred from the standard Maven layout:
      src/test/java/com/example/FooTest.java  →  com.example.FooTest

    When a new test file is added to the glob, Bazel picks it up automatically —
    no BUILD file changes needed.
    """
    for src in srcs:
        # Strip everything up to and including src/test/java/ to get the class path
        parts = src.split("src/test/java/")
        class_name = parts[-1].replace("/", ".").replace(".java", "")
        test_name = src.split("/")[-1].replace(".java", "")
        junit5_test(
            name = test_name,
            srcs = [src],
            test_class = class_name,
            deps = deps,
            size = size,
            **kwargs
        )
