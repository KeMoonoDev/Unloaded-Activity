# Using `SimulationMethod`s

## What is a `SimulationMethod`?
A `SimulationMethod` handles the way blocks are simulated. It can be registered to blocks/tags using datapacks. Unloaded Activity comes with a bunch of `SimulationMethod`s with many configurations that you can use.

## How to assign `SimulationMethod`s to blocks/tags
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

The structure inside these files are called `SimulationData` internally, not to be confused with `SimulationMethod`.
Here's an example of how it looks like:
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

## todo

talk about the different traits the simulation methods can have