"""jooq_library: generate a java_library from Flyway DDL migrations via JOOQ DDLDatabase.

Usage:
    load("//tools/jooq:jooq.bzl", "jooq_library")

    jooq_library(
        name = "db",
        srcs = glob(["db/migrations/*.sql"]),
        package = "com.example.db",
    )

The macro produces:
  :<name>_gen   — genrule that runs codegen, emits a .srcjar
  :<name>       — java_library over those sources, ready to depend on
"""

load("@rules_java//java:defs.bzl", "java_library")

def jooq_library(name, srcs, package, visibility = None):
    native.genrule(
        name = name + "_gen",
        srcs = srcs,
        outs = [name + "_gen.srcjar"],
        tools = ["//tools/jooq:codegen_bin"],
        # argv: <output.srcjar> <java.package> <file1.sql> [file2.sql ...]
        cmd = "$(location //tools/jooq:codegen_bin) $@ " + package + " $(SRCS)",
    )

    java_library(
        name = name,
        srcs = [":" + name + "_gen"],
        deps = ["@maven//:org_jooq_jooq"],
        visibility = visibility,
    )
