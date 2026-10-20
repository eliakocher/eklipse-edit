# Eklipse Edit

VS Code style editing for Eclipse: `Cmd+C` / `Cmd+X` with nothing selected copy or cut the whole line, `Cmd+V` puts such
a line above the current one.

## Features

| Key | Nothing selected | Text selected |
|---|---|---|
| `Cmd+C` | copies the line, the caret stays | copies the selection, as before |
| `Cmd+X` | cuts the line, the caret keeps its column on the next line | cuts the selection, as before |
| `Cmd+V` | a line copied or cut like that goes above the current line; other text is pasted at the caret | pastes over the selection, as before |

Use `Ctrl` instead of `Cmd` on Windows and Linux. A copied line always ends with a line break, also the last line of a
file, and gets the line breaks of the file it's pasted into. Undo brings a cut line back in one step.

It works in every text editor (Java, XML, properties, ...) while its text has the focus. Everything else, like a
selection, a text field inside an editor (the find overlay), several carets or block selection, stays with Eclipse's
own Copy, Cut and Paste, so JDT's copy and paste with imports keeps working.

## Installation

Requires Eclipse 2024-03 or newer on macOS, Windows or Linux (tested on macOS so far).

1. *Help > Install New Software...*, enter `https://eliakocher.github.io/eklipse-edit/` in *Work with*.
2. Check *Eklipse Edit* and uncheck *Contact all update sites during install to find required software*, otherwise
   Eclipse may also upgrade unrelated plugins.
3. Finish the wizard. When asked, trust the signer *Eklipse Search* with the fingerprint
   `bd16c7887e6a370ece3c202e4a232a6d4ce4a426`, then restart. It's the same signer as
   [Eklipse Search](https://github.com/eliakocher/eklipse-search), if you trust it already Eclipse doesn't ask.

To update, repeat these steps. *Help > Check for Updates* contacts all update sites, like leaving the box in step 2
checked.

From the command line, with Eclipse closed (`Eclipse.app/Contents/MacOS/eclipse` on macOS, `eclipsec.exe` on Windows):

```sh
eclipse -nosplash -application org.eclipse.equinox.p2.director \
    -repository https://eliakocher.github.io/eklipse-edit/ \
    -installIU com.eklipse.edit.feature.feature.group \
    -trustedPGPKeys bd16c7887e6a370ece3c202e4a232a6d4ce4a426 -trustSignedContentOnly
```

## Usage

Nothing to set up. The commands are called *Copy, Whole Line If Nothing Selected*, *Cut, Whole Line If Nothing
Selected* and *Paste, Whole Line Above* in *Preferences > General > Keys*, bound in *Editing Text*. Remove their
bindings there to get Eclipse's plain Copy, Cut and Paste back.

## Building

Needs Java 21, Maven is downloaded by the wrapper (`mvnw.cmd` on Windows).

```sh
./mvnw verify                                          # plugin, tests and update site
./mvnw verify -Pui-tests                               # plus UI tests, they use the clipboard
./mvnw verify -Declipse.release=2026-09 -Dbuild.ee=JavaSE-21   # against a newer Eclipse
```

The update site ends up in `com.eklipse.edit.site/target/`, as a folder and a zip you can add in *Install New
Software*. Your own build isn't signed, so Eclipse asks whether to trust unsigned content.

## Known limitations

- Only the keys change, *Edit > Copy* and the context menu keep Eclipse's behavior.
- Only lines copied or cut with these keys paste above the current line, a line copied in another application pastes at
  the caret.

## License

[MIT](LICENSE)
