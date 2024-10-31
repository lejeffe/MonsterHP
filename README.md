# Monster HP percentage
This plugin shows the current percentage or numeric hp of given NPC's.
The plugin remembers the HP when the NPC doesn't have a HP bar shown.

![Image example](https://i.imgur.com/D08OPHy.png)

## NPC Names - Configurations

The plugin allows for special operators/keywords in the NPC names list, making it easier to customize HP displays per NPC.

### Wildcard Matching
To display HP for all NPCs with names that start with a specific prefix, use the `*` wildcard at the end of the name.  

**Example:**  
To display HP text for all monsters that start with example "skeleton", use:  

`skeleton*`

### Numeric Health Display
To show the numeric health of an NPC (instead of a percentage), add the `:n` keyword after the NPC name. **Note**: Not all NPCs support this option.  

**Example:**  
To display numeric health for "skeleton", use:  

`skeleton:n`

### Or Combine Both!
`skele*:n` - Marks everything starting with skele and set it's display type to `:n` for numeric.

## Usage example:

**Notice**: Comma seperator

`goblin,man:n,skele*,guard*:n`