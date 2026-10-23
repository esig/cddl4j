# cddl4j

**CBOR library and CDDL validator. Written in Java.**

`cddl4j` is a Java 8 library that implements CBOR data handling based on
[IETF RFC 8949](https://www.rfc-editor.org/rfc/rfc8949.html) and supports a documented subset of CDDL from
[IETF RFC 8610](https://www.rfc-editor.org/rfc/rfc8610.html). Its main features are:

- CBOR data structures creation;
- CBOR content parsing;
- CBOR verification against a CDDL.

It can be used to create or to verify any type of data in CBOR format, such as
[IETF RFC 9052 COSE](https://www.rfc-editor.org/rfc/rfc9052.html), ISO/IEC 18013-5 Mobile driving licence (mDL), etc.

The project contains two modules:

- `cbor` — a small self-contained CBOR data model, decoder, and encoder that can also be used on its own.
- `validator` — CDDL parsing, schema compilation, and CBOR validation.

> **WIP:** the validator implements a practical subset of RFC 8610 rather than the complete CDDL grammar.
> The supported subset and known limitations are documented below.

## Project design

- **No runtime third-party dependencies:** the project includes its own CBOR representation and codec.
- **Immutability:** `CddlValidator` and `CddlSchema` instances are immutable and thread-safe. `CBORValue` represents
  scalar and container values; explicit editors provide mutation for arrays and maps.
- **Extensible:** allows application-defined CDDL rules and custom control operators without changing the library.
- **Java 8 compatible:** and tested with JDK 26.

## Quick start

Load the CDDL and validate a CBOR payload against it.
A schema can be compiled once and reused for incoming payloads:

```java
import eu.europa.ec.digit.cddl4j.validator.CddlValidator;
import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

String cddl = String.join("\n",
        "sensor-reading = {",
        "    device-id: tstr,",
        "    timestamp: uint,",
        "    value: float,",
        "    unit: \"celsius\" / \"fahrenheit\" / \"humidity\"",
        "}");

// Immutable CddlSchema
CddlSchema schema = CddlValidator.loadSchema(cddl);

byte[] cborPayload = ...; // received from a file, network, database, etc.
ValidationResult result = schema.validate(cborPayload);

if (result.isValid()) {
    System.out.println("valid");
} else {
    result.getErrors().forEach(System.out::println);
}
```

Validation errors carry an instance path, message, and nested causes.

## Maven

For validation, use the `validator` module:

```xml
<dependency>
    <groupId>eu.europa.ec.digit.cddl4j</groupId>
    <artifactId>validator</artifactId>
    <version>0.1.0</version>
</dependency>
```

The `validator` module depends on `cbor` transitively. Applications that only need the CBOR model and codec can depend on:

```xml
<dependency>
    <groupId>eu.europa.ec.digit.cddl4j</groupId>
    <artifactId>cbor</artifactId>
    <version>0.1.0</version>
</dependency>
```

## What the validator supports

The parser covers the following practical subset of CDDL.

| Area | Supported |
|---|---|
| Rule definitions | `name = type` |
| Type updates | `name /= type` |
| Group/socket updates | `name //= group` and RFC 8610 group sockets such as `$$option //= (...)` |
| Prelude types | `any`, `uint`, `nint`, `int`, `float`, `float16`, `float32`, `float64`, `bool`, `tstr`, `bstr`, `nil`, `undefined` |
| Prelude aliases | `text`, `bytes`, `null` |
| Numeric prelude types | `number`, `integer`, `unsigned`, `bigint`, `biguint`, `bignint`, `decfrac`, `bigfloat`, `float16-32`, `float32-64` |
| Semantic-tag prelude types | `tdate`, `time`, `uri`, `b64url`, `b64legacy`, `regexp`, `mime-message`, `encoded-cbor`, `eb64url`, `eb64legacy`, `eb16`, `cbor-any` |
| Boolean literals | `true`, `false` |
| Integer literals | decimal, hexadecimal, and binary, e.g. `42`, `-7`, `0xFF`, `0b101` |
| Floating-point literals | decimal forms such as `1.5`, `3.14` |
| Text literals | `"hello"` with CDDL string escapes |
| Byte-string literals | `h'deadbeef'`, `b64'SGVsbG8='` |
| Type choices | `a / b / c` |
| Inclusive ranges | `0..255` |
| Exclusive upper ranges | `0...256` |
| Arrays | `[uint, tstr, * float]` |
| Maps | `{name: tstr, age: uint}` |
| Typed map keys | `tstr => any` |
| Wildcard/repeated map entries | `* tstr => any` |
| Optional and repeated members | `?`, `*`, `+`, `n*m` |
| Parenthesized types/groups | `(uint / tstr)`, `(foo: uint, bar: tstr)` |
| Group choices | `//` and additive `//=` |
| Group enumeration | `&(red: 1, green: 2)` and named groups |
| Unwrap | `~group-name` / `~type-name` where applicable to groups |
| Cut marker | `^` on typed map entries |
| Named references | including forward references |
| Recursive references | including recursive trees and nested structures |
| CBOR tags | `#6.1(type)`, `#6.32(tstr)`, arbitrary tag numbers |
| Major-type selectors | `#0` through `#7` |
| Bare `#` | equivalent to `any` |
| Comments | `; comment to end of line` |
| Control operators | `.size`, `.cbor`, `.cborseq` |

### Standard prelude

Compiled schemas automatically include the RFC 8610 prelude plus the additional names used by the implementation.
User-defined CDDL rules take precedence over custom rules, and custom rules take precedence over prelude rules.

The prelude definitions are intended to model the CDDL/CBOR concepts represented by the current implementation;
they are not a claim that every part of the corresponding RFC prose is implemented.

## Schema loading

Schemas can come from the common sources used by Java applications:

```java
// String / byte[] / InputStream
CddlSchema schema = CddlValidator.loadSchema(cddlSource);

// Classpath resource
CddlSchema schema = CddlValidator.loadSchemaFromResource("/schemas/device.cddl");

// File
CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("schemas/device.cddl"));
```

The first schema-defined rule is the default validation entry point.

Alternatively, an explicit root rule can be selected during loading or afterwards:

```java
// On schema loading
CddlSchema schema = CddlValidator.loadSchema(cddlSource, "DeviceResponse");

// or

// Equivalent style after loading:
CddlSchema another = CddlValidator.loadSchema(cddlSource)
        .withRootRule("DeviceResponse");
```

`CddlValidator` and `CddlSchema` instances are immutable and **thread-safe**.

`CddlParser` and `CddlTokenizer` are used while loading schemas, and invoked internally during the validation process.
They are stateful not thread-safe; use a separate instance for concurrent parsing.

## Validation

Validation accepts raw CBOR or an already-decoded `CBORValue`:

```java
// Raw CBOR bytes (byte[]), using the schema's effective root rule
ValidationResult result = schema.validate(cborBytes);

// Raw CBOR bytes (byte[]), using a named rule (String)
ValidationResult result = schema.validate(cborBytes, "payload");

// An already-decoded value
CBORValue value = CBOR.decodeFromBytes(cborBytes);
ValidationResult result = schema.validate(value);
```

The API also accepts an `InputStream` for the CBOR payload. Validation consumes the supplied payload stream but does not
close it; stream ownership stays with the caller.

There are also static convenience methods for validating CDDL and CBOR in one call:

```java
// Parses the CDDL and CBOR content and validates it in one call
ValidationResult result = CddlValidator.validate(cddlSource, cborBytes);
```

Malformed CBOR is reported as a failed `ValidationResult` when using the schema validation API. Direct CBOR decoding uses
the checked `CBORDecodeException`.

## Error reporting

Validation is designed to be useful both in application logs and tests:

```java
if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        System.out.println(error.getInstancePath());
        System.out.println(error.getMessage());
        error.getCauses().forEach(System.out::println);
    }
}
```

Paths identify the location in the validated value.
Composite types such as choices can expose nested causes,
so callers can inspect the full reason a value containing an error.

For tests and assertions:

```java
result.assertValid();
```

`assertValid()` throws `java.lang.AssertionError` with the formatted validation errors when validation fails.

## Extending the validator

The validation rules, such as a prelude and/or control operators, can be extended without changing global library state.

### Application-defined rules

```java
Map<String, CddlType> customRules = new HashMap<>();
customRules.put("my-id", PrimitiveType.TSTR);

CddlSchema schema = CddlValidator.loadSchema("root = my-id")
        .withRules(customRules);
```

Custom rules can be combined and are resolved after schema-defined rules and before the built-in prelude.
Built-in prelude rules remain available unless replaced by a rule with the same name.

### Custom control operators

Control operators are explicit extension points. Implement `ControlOperator` and attach it to a schema:

```java
ControlOperator customOperator = new ControlOperator() {
    @Override
    public String getName() {
        return ".example";
    }

    @Override
    public ValidationResult validate(
            CBORValue value, CddlType controller, ValidationContext context) {
        // Application-specific semantics.
        return ValidationResult.success();
    }
};

CddlSchema schema = CddlValidator.loadSchema(
        "root = tstr .example uint")
        .withControlOperators(Collections.singletonList(customOperator));
```

Configured operators are scoped to the schema instance. Built-in operators remain available unless replaced by an operator
with the same name.

## CBOR module

The `cbor` module is independently usable when you need a lightweight CBOR representation without the CDDL validator.

```java
import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;

CBORValue map = CBOR.map()
        .put("id", 42)
        .put("payload", "hello")
        .value();

byte[] encoded = map.encodeToBytes();
CBORValue decoded = CBOR.decodeFromBytes(encoded);
```

### CBOR data model

The in-memory model represents:

- integers using `BigInteger`, including the full unsigned 64-bit CBOR range; values outside the CBOR wire range can be
  represented in memory but cannot be encoded as a standard CBOR integer;
- floating-point numbers as Java `double` values;
- byte strings;
- text strings;
- arrays;
- maps;
- booleans;
- arbitrary CBOR semantic tags, including tag numbers up to `2^64 - 1`;
- null, undefined, and simple values.

`CBOR.value(Object)` converts the Java value types supported by the library (`CBORValue`, `null`, booleans, integral
wrapper types, `BigInteger`, `Float`, `Double`, `String`, and `byte[]`) into `CBORValue` instances.

`CBORMapEditor` and `CBORArrayEditor` provide fluent mutation for programmatically building and
editing values in maps and arrays, respectively.

Byte string inputs and outputs are defensively copied.

### Decoding behavior

The decoder accepts definite and indefinite-length strings, arrays, and maps. Indefinite-length values are normalized into
the regular in-memory representation. Encoded output is always emitted using definite lengths.

The decoder rejects malformed input including truncated items, reserved additional-information values, invalid UTF-8,
invalid indefinite-length chunks, trailing bytes after the first data item, and duplicate map keys.

Currently **CBORDecoder** processes exactly one CBOR data item. If trailing bytes remain, including when a sequence of
CBOR data items is passed, the value is rejected.

Duplicate map keys are rejected rather than silently overwriting one another. RFC 8949 states that a duplicate-key map
may be well-formed but is not valid CBOR and has indeterminate decoding; this library rejects duplicates instead of
choosing an application-specific interpretation.
For mutable array or map keys, uniqueness is checked when the key is inserted; do not mutate such a key afterward,
because its equality can change.

### Safeguards and size limitations

The decoder includes configurable safeguards for processing untrusted CBOR input. Limits are enforced by default and can
be configured through these system properties:

- `eu.europa.ec.digit.cddl4j.cbor.max_byte_string_length` limits the encoded byte length of each CBOR byte or text string,
  including the combined length of indefinite-string chunks. The default is `10 MiB`. Negative values disable this limit.
- `eu.europa.ec.digit.cddl4j.cbor.max_nesting_depth` limits nesting to `32` data items by default. Negative values disable
  this limit.

The string limit is not a total input-size or item-count limit. Callers decoding untrusted data should also bound the
input stream.

### Encoding behavior

Two encoding modes are available:

- `encode(...)` preserves map insertion order;
- `encodeDeterministic(...)` implements RFC 8949 core deterministic CBOR, including preferred serialization, shortest
  floating-point representation that preserves the value, definite lengths, and bytewise lexicographic ordering of
  deterministic map-key encodings.

Deterministic encoding is useful when a document or protocol requires a stable CBOR
serialization.

CBOR sequences are also supported as a value helper for applications working with `.cborseq` payloads.

## Standard CBOR tags

`CBORTags` provides convenient constructors for commonly used semantic tags.

| Tag | Helper | Meaning |
|---|---|---|
| `0` | `tdate(...)` | Date/time as ISO-8601 text |
| `1` | `time(...)` | Epoch-based date/time |
| `2` | `biguint(...)` | Arbitrary-precision non-negative integer |
| `3` | `bignint(...)` | Arbitrary-precision negative integer |
| `4` | `decfrac(...)` | Decimal fraction |
| `5` | `bigfloat(...)` | Binary floating-point value |
| `24` | `encodedCbor(...)` | Encoded CBOR data item |
| `32` | `uri(...)` | URI |
| `33` | `b64url(...)` | Base64url text |
| `34` | `b64legacy(...)` | Legacy Base64 text |
| `35` | `regexp(...)` | Regular expression text |
| `36` | `mimeMessage(...)` | MIME message text |
| `100` | `date(...)` | RFC 8943 numeric date |
| `1004` | `fullDate(...)` | RFC 8943 full-date text |

Example:

```java
CBORValue date = CBORTags.tdate(
        OffsetDateTime.parse("2026-08-31T12:34:56Z"));
```

The convenience API constructs tagged CBOR values; it does not add semantic validation beyond the constraints encoded by
the individual helper.

## Future work

Currently, `cddl4j` omits some RFC 8610 functionalities. The current version covers a subset of main features which are
implemented end-to-end by the validator.

Known limitations include:

- **Generic CDDL parameters and arguments** such as `my-type<T>` are not implemented. The current parser treats the name
  as an ordinary reference instead of instantiating a parameterized rule.
- **Unprefixed byte-string literals** are not implemented. Use `h'...'` or `b64'...'`.
- **Hexadecimal floating-point literals** are not implemented; integer literals support decimal, hexadecimal, and binary
  forms, while floating-point literals currently support decimal forms.
- **Only three built-in control operators** are implemented: `.size`, `.cbor`, and `.cborseq`. Other RFC control operators
  such as `.regexp`, `.bits`, `.cat`, `.det`, `.within`, `.and`, `.lt`, `.le`, `.gt`, `.ge`, `.eq`, `.ne`, and
  `.default` are not built in. Please note that it is possible to supply your own implementation of these or
  other control operators to the validator (see above).
- **Streaming CDDL tokenization** is not implemented. CDDL input streams are currently materialized before tokenization.
- **The CBOR in-memory floating-point model uses `double`.** Wire-level deterministic encoding still chooses the shortest
  representation that preserves the represented value.

## Building and testing

The project is a Maven multi-module build targeting Java 8:

```bash
mvn clean package
```

`package` builds both modules, runs the tests, and attaches source/Javadoc artifacts configured by the parent build.

To build without running tests:

```bash
mvn package -DskipTests
```

Production artifacts target Java 8 bytecode.
Tests use JUnit 6 thus the project packaging requires at least Java 17 to build.

## Modules and packages

```text
cddl4j
├── cbor
│   └── eu.europa.ec.digit.cddl4j.cbor
│       ├── CBOR             public construction/decoding facade
│       ├── CBORValue        mutable CBOR data model
│       ├── CBORDecoder      CBOR parser/decoder
│       ├── CBOREncoder      CBOR encoder
│       ├── CBORTags         semantic-tag helpers
│       └── ...
│
└── validator
    └── eu.europa.ec.digit.cddl4j.validator
        ├── CddlValidator       public entry point
        ├── schema
        │   ├── CddlSchema       compiled immutable schema
        │   ├── CddlRuleSet      rule registry
        │   ├── operator         built-in/custom control operators
        │   ├── parser            tokenizer and recursive-descent parser
        │   └── type              CDDL type model
        └── validation
            ├── ValidationResult  validation outcome
            └── ValidationError   path/message/cause information
```

The package `schema/type` contains the current CDDL type model; `schema/operator` contains the control-operator extension
API and built-in operators.

## References

The implementation is based primarily on:

- [RFC 8610 — Concise Data Definition Language (CDDL)](https://www.rfc-editor.org/rfc/rfc8610.html)
- [RFC 8949 — Concise Binary Object Representation (CBOR)](https://www.rfc-editor.org/rfc/rfc8949.html)
- [RFC 8943 — CBOR Tags for Date and Time](https://www.rfc-editor.org/rfc/rfc8943.html) for the date/full-date helpers.

The library does not attest full compliance with the standards above.

## Examples in the test suite

The test suite includes schemas and payloads exercising pseudo and real multi level protocol structures,
including mdoc and VICAL examples (test samples are provided exclusively for informational purposes,
compliance with the underlying specifications is not guaranteed).

For the most precise examples of the currently supported syntax, the executable tests under `validator/src/test/java`
and the accompanying samples under `validator/src/test/resources` are a useful companion to this README.

## License

The cddl4j project is delivered under the terms of the European Union Public License (EUPL), version 1.2.

[![License (EUPL version 1.2)](https://img.shields.io/badge/license-EUPL%20version%201.2-blue.svg?style=flat-square)](https://opensource.org/license/eupl-1-2)

SPDX-License-Identifier : EUPL-1.2