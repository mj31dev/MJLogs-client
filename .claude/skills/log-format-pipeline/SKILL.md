---
name: log-format-pipeline
description: How MJLogs turns raw log lines into LogEntry values through detect, compile, parse and preview. Use when adding or changing a built-in log format, a timestamp pattern, the format wizard, or anything that reads a log file.
---

# The log format pipeline

A log file arrives as text in an unknown layout. Four ports in `:domain` turn it into `LogEntry`
values, each implemented in `:data`. Change one stage and you almost always have to touch its
neighbours, which is why they are described together.

## The chain

```
sample lines
  → LogFormatDetector.detect()          → FormatDetectionResult   (which built-in format matches?)
  → LogFormatCompiler.compile()         → FormatCompilationResult (user-typed format → LogFormatSpec)
  → LogLineParserFactory.create()       → LogLineParser           (spec + reference date → parser)
  → LogLineParser                       → ParsedLine              (one line → components)
  → LogFormatPreviewer.preview()        → FormatPreview           (live highlighting while typing)
```

| Port                   | `:domain`                  | `:data`                                             |
| ---------------------- | -------------------------- | --------------------------------------------------- |
| `LogFormatDetector`    | `domain/format/detect/`    | `HeuristicLogFormatDetector`, `LogFormatGuesser`      |
| `LogFormatCompiler`    | `domain/format/compile/`   | `TemplateLogFormatCompiler`, `LineFormatCompiler`     |
| `LogLineParserFactory` | `domain/format/parse/`     | `RegexLogLineParserFactory`, `RegexLogLineParser`     |
| `LogFormatPreviewer`   | `domain/format/preview/`   | `RegexLogFormatPreviewer`                             |

Timestamps are their own sub-pipeline in `data/format/timestamp/`: `TimestampShapeInference` guesses
the shape, `TimestampPatternCompiler` turns a pattern into a `CompiledTimestampPattern`, and
`TimestampResolutionContext` supplies what the pattern omits.

## The rules that bite

**A pattern without a date needs a reference date.** `LogLineParserFactory.create` takes
`referenceDate` precisely because Android logcat (`MM-dd HH:mm:ss.SSS`) and bare `HH:mm:ss` cannot
place a line in absolute time on their own. Any new format whose timestamp omits the date has to
thread that reference through, or every entry lands on the wrong day.

**A format is two strings, not one.** `ManualFormatInput` carries `timestampPattern` and
`structureTemplate` separately — for example `dd.MM.yyyy_HH.mm.ss` and
`<{any}>~{timestamp}~{tag}~{message}`. The placeholders are defined in
`domain/format/spec/LogFormatPlaceholders.kt`; `{any}` deliberately discards a component. Adding a
placeholder means touching the compiler, the previewer and `LogFormatGroups` together.

**Compilation can fail, and the failure is a UI concern.** `FormatCompilationResult` reports which
field is wrong through `FormatErrorField`, because the wizard highlights that field. Do not collapse
a compilation failure into a thrown exception or a null.

**The previewer runs on every keystroke.** `LogFormatPreviewer.preview` is called from the format
dialog as the user types, so it must tolerate half-written patterns and must not be expensive.

**Unparseable lines are skipped, not fatal.** A source reports `skippedLineCount`
(`LogSourceUi`, `LogSourceAssembler`); a file that partly parses still loads.

## Adding a built-in format

1. Add the `LogFormatSpec` to `BuiltInLogFormats` in `data/format/detect/`.
2. Check `HeuristicLogFormatDetector` still discriminates it from its neighbours — the risk is a new
   format that shadows an existing one on ambiguous samples.
3. Add sample lines and a test in `data/src/commonTest/…/format/detect/BuiltInLogFormatsTest.kt`
   and `HeuristicLogFormatDetectorTest.kt`.
4. If the timestamp shape is new, extend `TimestampShapeInference` and its test.
5. Real files live in `samples/` — verify against one rather than against invented lines.

Directories under `format/` are already near the 5-file limit; a new stage means a new sub-package.
