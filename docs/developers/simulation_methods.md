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

The json structure may look something like this:
```json
{
    "priority": 999, // Optional. Default value is 1000
    "replace": true, // Optional. Default value is false
    "custom_key_name": {  /* Define SimulationMethod properties here */ },
    "the_key_can_be_anything": { /* Define SimulationMethod properties here */ },
    "except_for_priority_and_replace": { /* Define SimulationMethod properties here */ }
}
```

| Field | Type | Description |
| - | - | - |
| `priority`? | Number | The priority the entry. Lower values will be applied later, and higher values will be applied earlier. Entries on blocks will always be applied after entries on tags, no matter the priority. (Default: 1000) |
| `replace`? | Boolean | If it should discard all old data and start fresh. By default, if there's 2 different entries affecting the same block, the one with lower priority will overlay it's data on top of the previous entry. (Default: false) |
| . . . | SimulationMethod | Any other field that isn't `priority` or `replace` should have a `SimulationMethod`. The field name may be used to identify the SimulationMethod from other SimulationMethods, or to modify/override it from other entries. |

## Available `SimulationMethod`s

### Base `SimulationMethod`

!!! note warning "Cannot be assigned"
    This cannot be assigned to anything. This is what every other `SimulationMethod` builds upon.

| Field | Type | Description |
| - | - | - |
| `replace`? | Boolean | If it should discard all old data and start fresh. By default, if there's 2 different entries affecting the same SimulationMethod, the one with lower priority will overlay it's data on top of the previous entry.<br>(Default: false) |
| <nobr>`simulation_method`<nobr> | Identifier | The identifier of the SimulationMethod you want to use. If the identifiers namespace is "unloadedactivity", you can skip out on it. So instead of writing "unloadedactivity:property", you can just write "property". |
| <nobr>`is_precipitation`?<nobr> | Boolean | If the simulation should only be active if it has direct access to the sky.<br>(Default: false) |
| <nobr>`requires_rain`?<nobr> | Boolean | If the simulation should only be active if it's currently raining.<br>(Default: whatever `is_precipitation` is set to) |
| <nobr>`conditions`?<nobr> | FixedCondition[] | Conditions that needs to be valid in order for the simulation to start.<br>(Default: []) |
| <nobr>`dependencies`?<nobr> | String[] | The keys to different SimulationMethods that needs to have been finished before this SimulationMethod can start. This only works on SimulationMethods that are dependable.<br>(Default: []) |
| <nobr>`advance_probability`<nobr> | UpdatingValueExpression<Number> | The probability of the SimulationMethod performing 1 successful step per random/precipitation tick. A "step" can mean different things depending on the SimulationMethod. For `PropertyMethod`, it can mean to increase an IntegerProperty by 1, and for `DecayMethod`, it means that the block disappears. |

### `SeparableSimulationMethod`

!!! note annotate "Builds upon the base `SimulationMethod`"
    This method builds upon the base `SimulationMethod`, and has all the fields that the base `SimulationMethod` has.

!!! note warning "Cannot be assigned"
    This cannot be assigned to anything. Some `SimulationMethod`s may build upon this.

For now, this SimulationMethod doesn't really provide anything noticeable on the datapack side.

### `GroupableSimulationMethod`

!!! note annotate "Builds upon `SeparableSimulationMethod`"
    This method builds upon `SeparableSimulationMethod`, and has all the fields that `SeparableSimulationMethod` has.

!!! note warning "Cannot be assigned"
    This cannot be assigned to anything. Some `SimulationMethod`s may build upon this.


### `PropertyMethod`

!!! note annotate "Builds upon `GroupableSimulationMethod`"
    This method builds upon `GroupableSimulationMethod`, and has all the fields that `GroupableSimulationMethod` has.

### `MaxPropertyGrowthMethod`

!!! note annotate "Builds upon `SeparableSimulationMethod`"
    This method builds upon `SeparableSimulationMethod`, and has all the fields that `SeparableSimulationMethod` has.

### `IncrementPropertyGrowthMethod`

!!! note annotate "Builds upon `SeparableSimulationMethod`"
    This method builds upon `SeparableSimulationMethod`, and has all the fields that `SeparableSimulationMethod` has.

### `DecayMethod`

!!! note annotate "Builds upon `GroupableSimulationMethod`"
    This method builds upon `GroupableSimulationMethod`, and has all the fields that `GroupableSimulationMethod` has.

### `ReplaceMethod`

!!! note annotate "Builds upon `GroupableSimulationMethod`"
    This method builds upon `GroupableSimulationMethod`, and has all the fields that `GroupableSimulationMethod` has.

### `HatchMethod`

!!! note annotate "Builds upon `GroupableSimulationMethod`"
    This method builds upon `GroupableSimulationMethod`, and has all the fields that `GroupableSimulationMethod` has.

### `GrowFruitMethod`

!!! note annotate "Builds upon `SeparableSimulationMethod`"
    This method builds upon `SeparableSimulationMethod`, and has all the fields that `SeparableSimulationMethod` has.

### `GrowBambooMethod`

!!! note annotate "Builds upon the base `SimulationMethod`"
    This method builds upon the base `SimulationMethod`, and has all the fields that the base `SimulationMethod` has.

### `GrowTreeMethod`

!!! note annotate "Builds upon the base `SimulationMethod`"
    This method builds upon the base `SimulationMethod`, and has all the fields that the base `SimulationMethod` has.

### `BuddingMethod`

!!! note annotate "Builds upon the base `SimulationMethod`"
    This method builds upon the base `SimulationMethod`, and has all the fields that the base `SimulationMethod` has.

### `DripstoneMethod`

!!! note annotate "Builds upon the base `SimulationMethod`"
    This method builds upon the base `SimulationMethod`, and has all the fields that the base `SimulationMethod` has.