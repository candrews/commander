# Dictation For Unknown Grammar Plugin

This plugin's goal is to improve grammar recognition accuracy and usefulness.

This plugin delegates all requests to another grammar engine. If the result is a  match, then the match is passed along and nothing else is done. If there is no match, then the audio is passed on to a dictation engine, and the resulting dictation text is checked to see if it matches the grammar.

To check the dictation text against the grammar, the [Double Metaphone](https://en.wikipedia.org/wiki/Metaphone#Double_Metaphone) algorithm is used.

## Usage
Example configuration:
```
"grammarEngine": {
  "loader": "maven[com.integralblue.commander.plugins:dictationforunknowngrammar:0.0.1-SNAPSHOT]",
  "className": "com.integralblue.commander.plugins.dictationforunknowngrammar.DictationForUnknownGrammarPlugin",
  "config": {
    "jsgfParser": "jsgfParser",
    "grammarEngine": "grammarEngine2",
    "dictationEngine": "dictationEngine"
  }
}
```
### Configuration Options
| Name          | Description           | Default Value  |
| ------------- |:-------------:| -----:|
| jsgfParser      | Name of the JsgfParser plugin to use | jsgfParser |
| grammarEngine      | Name of the GrammarEngine plugin to use      |   <none> |
| dictationEngine | Name of the DictationEngine plugin to use      |    dictationEngine |

# TODO / Improvements
Instead of using [Double Metaphone](https://en.wikipedia.org/wiki/Metaphone#Double_Metaphone), use the phonetic distance algorithm described at http://www.jaivox.com/static/phoneticdistance.html . This approach should result in much greater accuracy (fewer false positives and false negatives) and be able to prioritize results instead of the simple approach currently implemented which can't rank match quality.