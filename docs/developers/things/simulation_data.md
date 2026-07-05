# `SimulationData`

## What is a `SimulationData`?

A `SimulationData` holds all the `SimulationMethod`s for a specific block. A block can only have one `SimulationData` assigned to it, but if several datapacks/mods tries to assign a `SimulationData` to the same block, or if a datapack/mod has assigned a `SimulationData` to a block and a tag which the block is assigned to, it will merge the data and end up with just one `SimulationData`. 

## How to assign `SimulationData`s to blocks/tags

Firstly, create a json file for your block/tag in your datapack/resources folder.
<br>
For blocks the path will be
<br>
`data/<namespace>/simulate_info/blocks/<block_name>.json`,
<br>
and for tags the path will be
<br>
`data/<namespace>/simulate_info/tags/<tag_name>.json`.

For example, the `minecraft:beetroots` block will be in
<br>
`data/minecraft/simulate_info/blocks/beetroots.json`,
<br>
and the `minecraft:crops` tag will be in
<br>
`data/minecraft/simulate_info/tags/crops.json`.

Inside these files is where you define your `SimulationData`. Here's an example:
```json
{
    "priority": 999, // Optional. Default value is 1000
    "replace": true, // Optional. Default value is false
    "custom_key_name": {  /* Define SimulationMethod properties here */ },
    "the_key_can_be_anything": { /* Define SimulationMethod properties here */ },
    "except_for_priority_and_replace": { /* Define SimulationMethod properties here */ }
}
```

## `SimulationData` properties

| Field | Type | Description |
| - | - | - |
| `priority`? | Number | The priority the entry. Lower values will be applied later, and higher values will be applied earlier. Entries on blocks will always be applied after entries on tags, no matter the priority. (Default: 1000) |
| `replace`? | Boolean | If it should discard all old data and start fresh. By default, if there's 2 different entries affecting the same block, the one with lower priority will overlay it's data on top of the previous entry. (Default: false) |
| . . . | SimulationMethod | Any other field that isn't `priority` or `replace` should have a `SimulationMethod`. The field name may be used to identify the SimulationMethod from other SimulationMethods, or to modify/override it from other entries. |

## `SimulationData` merging

### Simple merging

If you got the following `SimulationData` definition on the same block:
```json
{
    "method1": {  /* Define SimulationMethod properties here */ }
}
```
```json
{
    "method2": {  /* Define SimulationMethod properties here */ }
}
```
The final `SimulationData` for that block will look like this:

```json
{
    "method1": {  /* Define SimulationMethod properties here */ },
    "method2": {  /* Define SimulationMethod properties here */ }
}
```

### Overriding methods

If you now got these following `SimulationData` definition on the same block:
```json
{
    "old_method": {  /* Define SimulationMethod properties here */ }
}
```
```json
{
    "priority": 999,
    "replace": true,
    "new_method": {  /* Define SimulationMethod properties here */ }
}
```
The final `SimulationData` for that block will look like this:

```json
{
    "new_method": {  /* Define SimulationMethod properties here */ }
}
```
This is because lower priority gets applied later, and because replace was set to true, it cleared out any old `SimulationData` for that block.

### Overriding fields

If you now got these following `SimulationData` definition on the same block:
```json
{
    "method": {
        "value": "hello",
        "other_value": 5
    }
}
```
```json
{
    "priority": 999,
    "method": {
        "value": "world"
    }
}
```
The final `SimulationData` for that block will look like this:

```json
{
    "method": {
        "value": "world",
        "other_value": 5
    }
}
```
This is because lower priority gets applied later, and any fields that gets defined overriddes the old data, unless it's a JSON object or Array. Then the fields/elements gets merged.