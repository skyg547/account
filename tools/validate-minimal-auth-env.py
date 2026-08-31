#!/usr/bin/env python3
"""Validate the minimal external environment needed by auth development."""

from __future__ import annotations

import argparse
import io
import ipaddress
import os
import re
import stat
import sys
import tempfile
import unicodedata
from contextlib import contextmanager, redirect_stderr, redirect_stdout
from pathlib import Path
from typing import Callable, Iterator, Mapping, TextIO
from urllib.parse import urlsplit


REQUIRED_KEYS = (
    "ENCRYPT_KEY",
    "AUTH_JWT_SECRET",
    "AUTH_INTERNAL_API_TOKEN",
    "BFF_GATEWAY_SHARED_SECRET",
    "AUTH_DB_URL",
    "AUTH_DB_USER",
    "AUTH_DB_PASSWORD",
    "DEV_DB_HOST",
    "DEV_DB_PORT",
    "DEV_DB_NAME",
    "DEV_DB_USER",
    "DEV_DB_PASSWORD",
)
LOCAL_HOSTS = frozenset(
    (
        "account-postgres",
        "postgres",
        "postgres-db",
        "localhost",
        "localhost.localdomain",
        "ip6-localhost",
        "ip6-loopback",
        "host.docker.internal",
        "gateway.docker.internal",
        "host.containers.internal",
        "gateway.containers.internal",
        "127.0.0.1",
        "0.0.0.0",
    )
)
ALLOWED_SSL_MODES = frozenset(
    ("disable", "allow", "prefer", "require", "verify-ca", "verify-full")
)
NAME_PATTERN = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
LEGACY_IPV4_COMPONENT = r"(?:0[xX][0-9A-Fa-f]+|[0-9]+)"
LEGACY_IPV4_PATTERN = re.compile(
    rf"^{LEGACY_IPV4_COMPONENT}(?:\.{LEGACY_IPV4_COMPONENT}){{0,3}}$"
)
DNS_LABEL_PATTERN = re.compile(r"^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
ASCII_PORT_PATTERN = re.compile(r"^[0-9]+$")
PLACEHOLDER_PATTERN = re.compile(
    r"replace-with-|replace[-_]?me|example\.invalid|change[-_]?me|<secret>"
    r"|your[-_]secret[-_]here|\btbd\b",
    re.IGNORECASE,
)


class ValidationError(Exception):
    def __init__(self, subject: str, reason: str) -> None:
        super().__init__(subject, reason)
        self.subject = subject
        self.reason = reason


class RedactingArgumentParser(argparse.ArgumentParser):
    def error(self, message: str) -> None:
        del message
        raise ValidationError(
            "ARGUMENTS", "select exactly one of --env-file or --self-test"
        )


def render_error(error: ValidationError) -> str:
    return f"ERROR: {error.subject}: {error.reason}"


def contains_control_character(value: str) -> bool:
    return any(unicodedata.category(character) == "Cc" for character in value)


def parse_env(stream: io.TextIOBase) -> dict[str, str]:
    values: dict[str, str] = {}
    for line_number, raw_line in enumerate(stream, start=1):
        raw_content = raw_line[:-1] if raw_line.endswith("\n") else raw_line
        if raw_content.endswith("\r"):
            raw_content = raw_content[:-1]
        line = raw_content.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValidationError(f"line {line_number}", "malformed assignment")
        raw_name, raw_value = line.split("=", 1)
        name = raw_name.strip()
        if not NAME_PATTERN.fullmatch(name):
            raise ValidationError(f"line {line_number}", "malformed assignment")
        if name in values:
            raise ValidationError(name, "duplicate key")
        original_raw_value = raw_content.split("=", 1)[1]
        if contains_control_character(original_raw_value):
            raise ValidationError(name, "control characters are not allowed")
        value = raw_value.strip()
        if "'" in value or '"' in value:
            raise ValidationError(name, "quoted values are not supported")
        if "$" in value:
            raise ValidationError(name, "interpolation is not supported")
        if "#" in value:
            raise ValidationError(name, "inline comments are not supported")
        values[name] = value
    return values


def is_secure_posix_mode(mode: int) -> bool:
    return mode & 0o077 == 0


def check_file_permissions(mode: int) -> None:
    if os.name == "posix" and not is_secure_posix_mode(stat.S_IMODE(mode)):
        raise ValidationError("ENV_FILE", "group or other permissions are not allowed")


def parse_legacy_ipv4(host: str) -> ipaddress.IPv4Address:
    components = host.split(".")
    widths = {
        1: (32,),
        2: (8, 24),
        3: (8, 8, 16),
        4: (8, 8, 8, 8),
    }[len(components)]
    values: list[int] = []
    for component, width in zip(components, widths):
        if component.lower().startswith("0x"):
            if len(component) == 2:
                raise ValueError
            base = 16
        elif len(component) > 1 and component.startswith("0"):
            base = 8
        else:
            base = 10
        try:
            value = int(component, base)
        except ValueError:
            raise ValueError from None
        if value < 0 or value >= 1 << width:
            raise ValueError
        values.append(value)

    combined = 0
    for value, width in zip(values, widths):
        combined = (combined << width) | value
    return ipaddress.IPv4Address(combined)


def is_disallowed_host(host: str) -> bool:
    if not host or not host.isascii() or "%" in host:
        raise ValueError
    if host.endswith(".."):
        raise ValueError
    normalized = host.lower()
    if normalized.endswith("."):
        normalized = normalized[:-1]
    if normalized.startswith("[") and normalized.endswith("]"):
        normalized = normalized[1:-1]
    if not normalized:
        raise ValueError
    if normalized in LOCAL_HOSTS or normalized.endswith(".localhost"):
        return True

    try:
        address = ipaddress.ip_address(normalized)
    except ValueError:
        address = None
    if address is not None:
        if address.is_loopback or address.is_unspecified:
            return True
        if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped is not None:
            return address.ipv4_mapped.is_loopback or address.ipv4_mapped.is_unspecified
        return False

    if LEGACY_IPV4_PATTERN.fullmatch(normalized):
        legacy_address = parse_legacy_ipv4(normalized)
        return legacy_address.is_loopback or legacy_address.is_unspecified

    if len(normalized) > 253:
        raise ValueError
    labels = normalized.split(".")
    if not labels or any(not DNS_LABEL_PATTERN.fullmatch(label) for label in labels):
        raise ValueError
    return False


def validate_host(host: str, subject: str) -> None:
    try:
        disallowed = is_disallowed_host(host)
    except ValueError:
        raise ValidationError(subject, "host syntax is invalid") from None
    if disallowed:
        raise ValidationError(subject, "local database hosts are not allowed")


def validate_dev_host(host: str) -> None:
    if host.startswith("[") or host.endswith("]"):
        if not (host.startswith("[") and host.endswith("]")):
            raise ValidationError("DEV_DB_HOST", "host syntax is invalid")
        try:
            ipaddress.IPv6Address(host[1:-1])
        except ValueError:
            raise ValidationError("DEV_DB_HOST", "host syntax is invalid") from None
    elif ":" in host:
        raise ValidationError(
            "DEV_DB_HOST", "IPv6 addresses must use bracketed notation"
        )
    validate_host(host, "DEV_DB_HOST")


def validate_postgresql_url(value: str) -> None:
    prefix = "jdbc:"
    if not value.startswith("jdbc:postgresql://") or any(
        character.isspace() or ord(character) < 0x20 or ord(character) == 0x7F
        for character in value
    ):
        raise ValidationError("AUTH_DB_URL", "must be a PostgreSQL JDBC URL")
    if "#" in value:
        raise ValidationError("AUTH_DB_URL", "query configuration is invalid")
    has_query_delimiter = "?" in value
    try:
        parsed = urlsplit(value[len(prefix) :])
    except ValueError:
        raise ValidationError("AUTH_DB_URL", "must be a PostgreSQL JDBC URL") from None

    authority = parsed.netloc.rsplit("@", 1)[-1]
    if "," in parsed.netloc:
        raise ValidationError("AUTH_DB_URL", "multiple database hosts are not allowed")
    if authority.endswith(":"):
        raise ValidationError("AUTH_DB_URL", "contains an invalid port")
    try:
        port = parsed.port
    except ValueError:
        raise ValidationError("AUTH_DB_URL", "must be a PostgreSQL JDBC URL") from None

    if (
        parsed.scheme.lower() != "postgresql"
        or not parsed.hostname
        or parsed.fragment
        or parsed.path != "/auth_dev"
        or parsed.username is not None
        or parsed.password is not None
    ):
        raise ValidationError(
            "AUTH_DB_URL", "database target or credential placement is invalid"
        )
    validate_host(parsed.hostname, "AUTH_DB_URL")
    if port is not None and not 1 <= port <= 65535:
        raise ValidationError("AUTH_DB_URL", "contains an invalid port")
    if has_query_delimiter:
        query_parts = parsed.query.split("&")
        if len(query_parts) != 1 or "=" not in query_parts[0]:
            raise ValidationError("AUTH_DB_URL", "query configuration is invalid")
        raw_key, raw_value = query_parts[0].split("=", 1)
        if raw_key != "sslmode" or raw_value not in ALLOWED_SSL_MODES:
            raise ValidationError("AUTH_DB_URL", "query configuration is invalid")


def validate_values(values: Mapping[str, str]) -> None:
    for key in REQUIRED_KEYS:
        if key not in values:
            raise ValidationError(key, "required key is missing")
        if not values[key].strip():
            raise ValidationError(key, "blank value is not allowed")
        if contains_control_character(values[key]):
            raise ValidationError(key, "control characters are not allowed")
        if PLACEHOLDER_PATTERN.search(values[key]):
            raise ValidationError(key, "placeholder value is not allowed")

    if len(values["AUTH_JWT_SECRET"]) < 32:
        raise ValidationError("AUTH_JWT_SECRET", "does not meet the minimum length")
    if len(values["AUTH_INTERNAL_API_TOKEN"]) < 32:
        raise ValidationError("AUTH_INTERNAL_API_TOKEN", "does not meet the minimum length")
    if len(values["BFF_GATEWAY_SHARED_SECRET"].encode("utf-8")) < 32:
        raise ValidationError("BFF_GATEWAY_SHARED_SECRET", "does not meet the minimum length")
    if len(values["BFF_GATEWAY_SHARED_SECRET"].encode("utf-8")) > 512:
        raise ValidationError("BFF_GATEWAY_SHARED_SECRET", "exceeds the maximum length")

    validate_postgresql_url(values["AUTH_DB_URL"])
    if values["AUTH_DB_USER"] != "auth_dev_app":
        raise ValidationError("AUTH_DB_USER", "does not match the required runtime user")

    validate_dev_host(values["DEV_DB_HOST"])
    if not ASCII_PORT_PATTERN.fullmatch(values["DEV_DB_PORT"]):
        raise ValidationError("DEV_DB_PORT", "must be a valid TCP port")
    try:
        dev_port = int(values["DEV_DB_PORT"], 10)
    except ValueError:
        raise ValidationError("DEV_DB_PORT", "must be a valid TCP port") from None
    if not 1 <= dev_port <= 65535:
        raise ValidationError("DEV_DB_PORT", "must be a valid TCP port")
    if values["DEV_DB_NAME"] != "master_data_dev":
        raise ValidationError("DEV_DB_NAME", "does not match the required database")
    if values["DEV_DB_USER"] != "master_data_dev_app":
        raise ValidationError("DEV_DB_USER", "does not match the required runtime user")


@contextmanager
def open_env_file(path: Path) -> Iterator[TextIO]:
    before = path.lstat()
    if stat.S_ISLNK(before.st_mode) or not stat.S_ISREG(before.st_mode):
        raise ValidationError("ENV_FILE", "must be a regular non-symlink file")

    flags = os.O_RDONLY
    if os.name == "posix":
        flags |= getattr(os, "O_NOFOLLOW", 0)
        flags |= getattr(os, "O_NONBLOCK", 0)
        flags |= getattr(os, "O_CLOEXEC", 0)
    descriptor = os.open(path, flags)
    try:
        after = os.fstat(descriptor)
        if (
            not stat.S_ISREG(after.st_mode)
            or before.st_dev != after.st_dev
            or before.st_ino != after.st_ino
        ):
            raise ValidationError("ENV_FILE", "file identity changed during validation")
        check_file_permissions(after.st_mode)
        with os.fdopen(descriptor, "r", encoding="utf-8") as stream:
            descriptor = -1
            yield stream
    finally:
        if descriptor >= 0:
            os.close(descriptor)


def validate_env_file(path: Path) -> int:
    try:
        with open_env_file(path) as stream:
            values = parse_env(stream)
    except ValidationError:
        raise
    except (OSError, UnicodeError):
        raise ValidationError("ENV_FILE", "could not be read") from None
    validate_values(values)
    return len(REQUIRED_KEYS)


def valid_fixture() -> dict[str, str]:
    return {
        "ENCRYPT_KEY": "fixture-encryption-key",
        "AUTH_JWT_SECRET": "j" * 32,
        "AUTH_INTERNAL_API_TOKEN": "t" * 32,
        "BFF_GATEWAY_SHARED_SECRET": "b" * 32,
        "AUTH_DB_URL": "jdbc:postgresql://auth-db.internal:5432/auth_dev?sslmode=require",
        "AUTH_DB_USER": "auth_dev_app",
        "AUTH_DB_PASSWORD": "fixture-auth-password",
        "DEV_DB_HOST": "master-db.internal",
        "DEV_DB_PORT": "5432",
        "DEV_DB_NAME": "master_data_dev",
        "DEV_DB_USER": "master_data_dev_app",
        "DEV_DB_PASSWORD": "fixture-master-password",
    }


def fixture_text(values: Mapping[str, str]) -> str:
    return "\n".join(f"{key}={value}" for key, value in values.items()) + "\n"


def expect_error(action, subject: str, forbidden: tuple[str, ...] = ()) -> None:
    try:
        action()
    except ValidationError as error:
        rendered = render_error(error)
        if error.subject != subject or any(item and item in rendered for item in forbidden):
            raise AssertionError from None
        return
    raise AssertionError


def run_self_test() -> int:
    checks = 0
    base = valid_fixture()
    validate_values(base)
    checks += 1

    cases = (
        ("ENCRYPT_KEY", None, "ENCRYPT_KEY"),
        ("AUTH_DB_PASSWORD", "replace-with-private-value", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "CHANGE-ME-private-value", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "CHANGE_ME_private_value", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "changeme-private-value", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "replace_me_private_value", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "<secret>", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "your-secret-here", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "your_secret_here", "AUTH_DB_PASSWORD"),
        ("AUTH_DB_PASSWORD", "TBD", "AUTH_DB_PASSWORD"),
        ("DEV_DB_HOST", "database.example.invalid", "DEV_DB_HOST"),
        ("AUTH_JWT_SECRET", "secret-fixture-too-short", "AUTH_JWT_SECRET"),
        ("AUTH_INTERNAL_API_TOKEN", "t" * 31, "AUTH_INTERNAL_API_TOKEN"),
        ("BFF_GATEWAY_SHARED_SECRET", "b" * 31, "BFF_GATEWAY_SHARED_SECRET"),
        ("BFF_GATEWAY_SHARED_SECRET", "b" * 513, "BFF_GATEWAY_SHARED_SECRET"),
        (
            "AUTH_DB_URL",
            "jdbc:postgresql://auth-db.internal:5432/auth_dev?password=credential-fixture",
            "AUTH_DB_URL",
        ),
        (
            "AUTH_DB_URL",
            "jdbc:postgresql://auth-db.internal:5432/wrong_fixture_db",
            "AUTH_DB_URL",
        ),
        (
            "AUTH_DB_URL",
            "jdbc:postgresql://auth-db.internal:/auth_dev",
            "AUTH_DB_URL",
        ),
        ("AUTH_DB_USER", "wrong_fixture_user", "AUTH_DB_USER"),
        ("DEV_DB_NAME", "wrong_fixture_db", "DEV_DB_NAME"),
        ("DEV_DB_USER", "wrong_fixture_dev_user", "DEV_DB_USER"),
    )
    for key, replacement, subject in cases:
        invalid = dict(base)
        if replacement is None:
            del invalid[key]
        else:
            invalid[key] = replacement
        forbidden = tuple(invalid.values())
        expect_error(lambda fixture=invalid: validate_values(fixture), subject, forbidden)
        checks += 1

    token_boundary = dict(base)
    token_boundary["AUTH_INTERNAL_API_TOKEN"] = "t" * 32
    validate_values(token_boundary)
    checks += 1

    bff_secret_boundary = dict(base)
    bff_secret_boundary["BFF_GATEWAY_SHARED_SECRET"] = "b" * 512
    validate_values(bff_secret_boundary)
    checks += 1

    for quoted_value in ('""', "'   '", '"' + ("s" * 30) + '"'):
        expect_error(
            lambda value=quoted_value: parse_env(
                io.StringIO(f"AUTH_JWT_SECRET={value}\n")
            ),
            "AUTH_JWT_SECRET",
            (quoted_value,),
        )
        checks += 1

    for interpolated_value in ("${SENSITIVE_FIXTURE}", "$SENSITIVE_FIXTURE"):
        expect_error(
            lambda value=interpolated_value: parse_env(
                io.StringIO(f"AUTH_INTERNAL_API_TOKEN={value}\n")
            ),
            "AUTH_INTERNAL_API_TOKEN",
            (interpolated_value,),
        )
        checks += 1

    comment_values = (
        ("ENCRYPT_KEY", " # blank-fixture-comment"),
        ("AUTH_DB_PASSWORD", " # blank-db-password-comment"),
        ("AUTH_JWT_SECRET", ("j" * 31) + " # jwt-padding-comment"),
        (
            "AUTH_INTERNAL_API_TOKEN",
            ("t" * 31) + " # token-padding-comment",
        ),
    )
    for key, comment_value in comment_values:
        expect_error(
            lambda name=key, value=comment_value: parse_env(
                io.StringIO(f"{name}={value}\n")
            ),
            key,
            (comment_value,),
        )
        checks += 1

    for control_value in (
        "fixture\x00secret",
        "fixture\tsecret",
        "fixture\x1fsecret",
        "fixture\x7fsecret",
        "fixture\x85secret",
    ):
        expect_error(
            lambda value=control_value: parse_env(
                io.StringIO(f"AUTH_DB_PASSWORD={value}\n")
            ),
            "AUTH_DB_PASSWORD",
            (control_value,),
        )
        checks += 1

    printable_unicode = parse_env(
        io.StringIO("AUTH_DB_PASSWORD=fixture-한글-password\n")
    )
    if printable_unicode["AUTH_DB_PASSWORD"] != "fixture-한글-password":
        raise AssertionError
    checks += 1

    blocked_hosts = (
        "localhost",
        "localhost.",
        "localhost.localdomain",
        "ip6-localhost",
        "ip6-loopback",
        "host.docker.internal",
        "gateway.docker.internal",
        "host.containers.internal",
        "gateway.containers.internal",
        "service.localhost",
        "service.localhost.",
        "account-postgres",
        "postgres",
        "postgres-db",
        "127.0.0.1",
        "127.0.0.2",
        "127.1",
        "2130706433",
        "0.0.0.0",
        "0",
        "0x7f000001",
        "0177.1",
        "[::1]",
        "[::]",
        "[::ffff:127.0.0.1]",
        "[::ffff:0.0.0.0]",
    )
    for host in blocked_hosts:
        invalid_auth_host = dict(base)
        invalid_auth_host["AUTH_DB_URL"] = (
            f"jdbc:postgresql://{host}:5432/auth_dev?sslmode=require"
        )
        expect_error(
            lambda fixture=invalid_auth_host: validate_values(fixture),
            "AUTH_DB_URL",
            tuple(invalid_auth_host.values()),
        )
        checks += 1

        invalid_dev_host = dict(base)
        invalid_dev_host["DEV_DB_HOST"] = host.strip("[]")
        expect_error(
            lambda fixture=invalid_dev_host: validate_values(fixture),
            "DEV_DB_HOST",
            tuple(invalid_dev_host.values()),
        )
        checks += 1

    private_hosts = dict(base)
    private_hosts["AUTH_DB_URL"] = (
        "jdbc:postgresql://10.20.30.40:5432/auth_dev?sslmode=require"
    )
    private_hosts["DEV_DB_HOST"] = "192.168.10.20"
    validate_values(private_hosts)
    checks += 1

    ipv6_hosts = dict(base)
    ipv6_hosts["AUTH_DB_URL"] = (
        "jdbc:postgresql://[2001:db8::10]:5432/auth_dev?sslmode=require"
    )
    ipv6_hosts["DEV_DB_HOST"] = "[2001:db8::20]"
    validate_values(ipv6_hosts)
    checks += 1

    for invalid_dev_ipv6_host in ("2001:db8::20", "[master-db.internal]"):
        invalid_dev_ipv6 = dict(base)
        invalid_dev_ipv6["DEV_DB_HOST"] = invalid_dev_ipv6_host
        expect_error(
            lambda fixture=invalid_dev_ipv6: validate_values(fixture),
            "DEV_DB_HOST",
            tuple(invalid_dev_ipv6.values()),
        )
        checks += 1

    unbracketed_auth_ipv6 = dict(base)
    unbracketed_auth_ipv6["AUTH_DB_URL"] = (
        "jdbc:postgresql://2001:db8::10:5432/auth_dev"
    )
    expect_error(
        lambda: validate_values(unbracketed_auth_ipv6),
        "AUTH_DB_URL",
        tuple(unbracketed_auth_ipv6.values()),
    )
    checks += 1

    bracketed_dev_loopback = dict(base)
    bracketed_dev_loopback["DEV_DB_HOST"] = "[::1]"
    expect_error(
        lambda: validate_values(bracketed_dev_loopback),
        "DEV_DB_HOST",
        tuple(bracketed_dev_loopback.values()),
    )
    checks += 1

    no_query = dict(base)
    no_query["AUTH_DB_URL"] = "jdbc:postgresql://auth-db.internal:5432/auth_dev"
    validate_values(no_query)
    checks += 1
    for ssl_mode in sorted(ALLOWED_SSL_MODES):
        ssl_fixture = dict(base)
        ssl_fixture["AUTH_DB_URL"] = (
            "jdbc:postgresql://auth-db.internal:5432/auth_dev"
            f"?sslmode={ssl_mode}"
        )
        validate_values(ssl_fixture)
        checks += 1

    ambiguous_urls = (
        "jdbc:postgresql://auth-db.internal:5432/auth_dev?",
        "jdbc:postgresql://auth-db.internal:5432/auth_dev#",
        "jdbc:postgresql://auth-db.internal:5432/auth_dev?sslmode=require#",
    )
    for ambiguous_url in ambiguous_urls:
        ambiguous_fixture = dict(base)
        ambiguous_fixture["AUTH_DB_URL"] = ambiguous_url
        expect_error(
            lambda fixture=ambiguous_fixture: validate_values(fixture),
            "AUTH_DB_URL",
            tuple(ambiguous_fixture.values()),
        )
        checks += 1

    uppercase_prefix = dict(base)
    uppercase_prefix["AUTH_DB_URL"] = (
        "JDBC:postgresql://auth-db.internal:5432/auth_dev"
    )
    expect_error(
        lambda: validate_values(uppercase_prefix),
        "AUTH_DB_URL",
        tuple(uppercase_prefix.values()),
    )
    checks += 1

    userinfo_fixture = dict(base)
    userinfo_fixture["AUTH_DB_URL"] = (
        "jdbc:postgresql://fixture-user@auth-db.internal:5432/auth_dev"
    )
    expect_error(
        lambda: validate_values(userinfo_fixture),
        "AUTH_DB_URL",
        tuple(userinfo_fixture.values()),
    )
    checks += 1

    rejected_queries = (
        "unknownKey=fixture-value",
        "host=localhost",
        "PGHOST=127.0.0.1",
        "dbname=wrong_fixture_db",
        "PGDBNAME=wrong_fixture_db",
        "port=1",
        "user=fixture-user",
        "password=fixture-password",
        "sslmode=",
        "sslmode=fixture-mode",
        "SSLMODE=require",
        "sslmode=REQUIRE",
        "ssl%6dode=require",
        "%73slmode=require",
        "sslmode=require&sslmode=disable",
        "ho%73t=localhost",
        "%50GHOST=127.0.0.1",
        "%64bname=wrong_fixture_db",
        "%50GDBNAME=wrong_fixture_db",
    )
    for rejected_query in rejected_queries:
        invalid_query = dict(base)
        invalid_query["AUTH_DB_URL"] = (
            "jdbc:postgresql://auth-db.internal:5432/auth_dev?" + rejected_query
        )
        expect_error(
            lambda fixture=invalid_query: validate_values(fixture),
            "AUTH_DB_URL",
            tuple(invalid_query.values()),
        )
        checks += 1

    multi_host_authorities = (
        "db.internal:5432,localhost:5432",
        "localhost:5432,db.internal:5432",
        "db.internal:5432,127.0.0.1:5432",
        "2130706433:5432,db.internal:5432",
        "db.internal:5432,host.docker.internal:5432",
    )
    for authority in multi_host_authorities:
        multi_host_fixture = dict(base)
        multi_host_fixture["AUTH_DB_URL"] = (
            f"jdbc:postgresql://{authority}/auth_dev"
        )
        expect_error(
            lambda fixture=multi_host_fixture: validate_values(fixture),
            "AUTH_DB_URL",
            tuple(multi_host_fixture.values()),
        )
        checks += 1

    invalid_hosts = (
        "local%68ost",
        "local host",
        "local\thost",
        "local\nhost",
        "-invalid.internal",
        "invalid-.internal",
        "invalid..internal",
        "invalid.internal..",
        "invalid_host.internal",
    )
    for host in invalid_hosts:
        invalid_auth_syntax = dict(base)
        invalid_auth_syntax["AUTH_DB_URL"] = (
            f"jdbc:postgresql://{host}:5432/auth_dev"
        )
        expect_error(
            lambda fixture=invalid_auth_syntax: validate_values(fixture),
            "AUTH_DB_URL",
            tuple(invalid_auth_syntax.values()),
        )
        checks += 1

        invalid_dev_syntax = dict(base)
        invalid_dev_syntax["DEV_DB_HOST"] = host
        expect_error(
            lambda fixture=invalid_dev_syntax: validate_values(fixture),
            "DEV_DB_HOST",
            tuple(invalid_dev_syntax.values()),
        )
        checks += 1

    trailing_dot_hosts = dict(base)
    trailing_dot_hosts["AUTH_DB_URL"] = (
        "jdbc:postgresql://auth-db.private.internal.:5432/auth_dev"
    )
    trailing_dot_hosts["DEV_DB_HOST"] = "master-db.private.internal."
    validate_values(trailing_dot_hosts)
    checks += 1

    for valid_port in ("1", "65535"):
        port_fixture = dict(base)
        port_fixture["DEV_DB_PORT"] = valid_port
        validate_values(port_fixture)
        checks += 1
    for invalid_port in ("0", "65536", "+5432", "-1", "٥٤٣٢"):
        port_fixture = dict(base)
        port_fixture["DEV_DB_PORT"] = invalid_port
        expect_error(
            lambda fixture=port_fixture: validate_values(fixture),
            "DEV_DB_PORT",
            tuple(port_fixture.values()),
        )
        checks += 1

    duplicate_secret = "duplicate-fixture-secret"
    expect_error(
        lambda: parse_env(io.StringIO(f"ENCRYPT_KEY=first\nENCRYPT_KEY={duplicate_secret}\n")),
        "ENCRYPT_KEY",
        (duplicate_secret,),
    )
    checks += 1
    malformed_value = "malformed-fixture-value"
    expect_error(
        lambda: parse_env(io.StringIO(f"NOT_AN_ASSIGNMENT_{malformed_value}\n")),
        "line 1",
        (malformed_value,),
    )
    checks += 1

    if not is_secure_posix_mode(0o600) or is_secure_posix_mode(0o640):
        raise AssertionError
    checks += 1
    if os.name == "posix":
        with tempfile.TemporaryDirectory(prefix="minimal-auth-validator-") as temporary_directory:
            env_path = Path(temporary_directory) / "sensitive-fixture.env"
            env_path.write_text(fixture_text(base), encoding="utf-8")
            env_path.chmod(0o600)
            if validate_env_file(env_path) != len(REQUIRED_KEYS):
                raise AssertionError
            checks += 1

            malformed_env_path = Path(temporary_directory) / "malformed-fixture.env"
            malformed_env_path.write_text(
                "NOT_AN_ASSIGNMENT_fixture-value\n", encoding="utf-8"
            )
            malformed_env_path.chmod(0o600)
            captured_stdout = io.StringIO()
            captured_stderr = io.StringIO()
            with redirect_stdout(captured_stdout), redirect_stderr(captured_stderr):
                malformed_return_code = main(
                    ["--env-file", str(malformed_env_path)]
                )
            if (
                malformed_return_code != 1
                or captured_stdout.getvalue() != ""
                or captured_stderr.getvalue()
                != "ERROR: line 1: malformed assignment\n"
            ):
                raise AssertionError
            if (
                str(malformed_env_path) in captured_stderr.getvalue()
                or "fixture-value" in captured_stderr.getvalue()
            ):
                raise AssertionError
            checks += 1

            env_path.chmod(0o644)
            expect_error(
                lambda: validate_env_file(env_path),
                "ENV_FILE",
                (str(env_path), *base.values()),
            )
            checks += 1

            symlink_path = Path(temporary_directory) / "sensitive-fixture-link.env"
            symlink_path.symlink_to(env_path)
            expect_error(
                lambda: validate_env_file(symlink_path),
                "ENV_FILE",
                (str(env_path), str(symlink_path), *base.values()),
            )
            checks += 1

            fifo_path = Path(temporary_directory) / "sensitive-fixture-fifo.env"
            os.mkfifo(fifo_path, 0o600)
            expect_error(
                lambda: validate_env_file(fifo_path),
                "ENV_FILE",
                (str(fifo_path), *base.values()),
            )
            checks += 1

    synthetic_path = "/sensitive/self-test/fixture.env"
    expect_error(
        lambda: run_self_test_safely(
            lambda: (_ for _ in ()).throw(OSError(synthetic_path))
        ),
        "SELF_TEST",
        (synthetic_path,),
    )
    checks += 1

    return checks


def run_self_test_safely(test_runner: Callable[[], int] = run_self_test) -> int:
    try:
        return test_runner()
    except Exception:
        raise ValidationError("SELF_TEST", "self-test failed") from None


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = RedactingArgumentParser(description="Validate a minimal auth environment file.")
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument("--env-file", type=Path, metavar="PATH")
    selection.add_argument("--self-test", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    try:
        args = parse_args(argv)
        count = run_self_test_safely() if args.self_test else validate_env_file(args.env_file)
    except ValidationError as error:
        print(render_error(error), file=sys.stderr)
        return 1
    except Exception:
        print("ERROR: INTERNAL: validation failed", file=sys.stderr)
        return 1
    print(f"PASS: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
