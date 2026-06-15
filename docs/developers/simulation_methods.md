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

Once you've created the file, you can put in something like this:
```json
{
    "method_key": {
        "simulation_method": "simulation_method_name"
        // Put SimulationMethod properties here.
        // Check the documentation for the SimulationMethod you want to use.
    }
}
```
"method_key" can be anything. It is used to be able to identify itself. If a block has several entries (which can occur if there are several mods/datapacks, or if you have a json file for a block and one or more json files for its tags), Unloaded Activity will sort all entries based on if they are an entry for a block or a tag, and then based on their priority. Higher priority entries gets applied first, and tag entries gets applied before block entries. 

If "method_key" gets assigned to several times, the later assignments will only replace fields that are defined. If the field holds an array or an object, it will get merged instead of replaced.


## Available `SimulationMethod`s

### Base `SimulationMethod`

This cannot be assigned to anything. This is what every other `SimulationMethod` builds upon.


### `SeparableSimulationMethod`

This cannot be assigned to anything. Some `SimulationMethod`s may build upon this.

### `GroupableSimulationMethod`

This cannot be assigned to anything. Some `SimulationMethod`s may build upon this. This method builds upon `SeparableSimulationMethod`.

### `PropertyMethod`

### `MaxPropertyGrowthMethod`

### `IncrementPropertyGrowthMethod`

### `DecayMethod`

### `ReplaceMethod`

### `HatchMethod`

### `GrowFruitMethod`

### `GrowBambooMethod`

### `GrowTreeMethod`

### `BuddingMethod`

### `DripstoneMethod`