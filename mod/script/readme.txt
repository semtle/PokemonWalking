This is a readme generated by Java code.

Put your scripts in this folder, and start the game. This will allow the game to load your
custom scripts into the game. But, the game will then no longer load the default scripts, unless
all of the custom scripts have been removed, or the game has been placed in a separate folder.

All custom scripts must follow the layout set forth by the documentation. It is strictly
adhered to the game codes, which makes it more difficult to modify and edit.

------

Automation Script Format:

Entities that can walk, or run, must be required to have movements for the gameto feel lively.                                                                

More commands to come.

_: Whitespaces.
@: Trigger name.
^: [Direction, Steps]. Can be chained for delaying scripted movements.
$: Start of script. Appears at beginning of script. Uses numeric ID values as Trigger IDs
%: Script delimiter. Always appear at end of script.
#: Speech Dialogue.
/: Comments. Whole line gets ignored, even if it's somewhere in the middle of the line.
?: Question Dialogue.
+: Affirmative dialogue.
-: Negative dialogue.
[: Affirmative Action.
]: Negative Action.
;: Repeat Flag. If contains ';', it means it's enabled by default.

Example:

$0
@Eraser
%