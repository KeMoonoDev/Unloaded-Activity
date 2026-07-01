# `PropertyMethod`

## Implementation Info

When using this SimulationMethod, it is gonna increase the value of the target property. It will use the property whose name matches what you set in the `property_name` field. If the property is an IntegerProperty, it will increase it for every successful trial. If it's a BooleanProperty, there will only be a maximum of 1 successful trial, which will turn its value from false to true.

| Trait | Value | Description |
| - | - | - |
| <nobr>Property ID<nobr> | <nobr>`unloadedactivity:property`<nobr> | Set the [`simulation_method` property](#simulation_method) to `"unloadedactivity:property"` or just `"property"` to use this `SimulationMethod`. |
| <nobr>Dependable?<nobr> | ✅ | You are able to set this as a dependency for another `SimulationMethod`. The other `SimulationMethod` will be able to run once this `SimulationMethod` reaches its max value. |
| <nobr>Groupable?<nobr> | ✅ | You are able to set this as a dependency for another `SimulationMethod`. The other `SimulationMethod` will be able to run once this `SimulationMethod` reaches its max value. |

## Properties

| Field | Type | Description |
| - | - | - |
| <nobr>`property_name`<nobr> | String | The name of the property you want to simulate. |
| <nobr>`update_type`?<nobr> | Integer | Which update type to use when updating the block.<br>(Default: 4) |
| <nobr>`update_neighbors`?<nobr> | boolean | If this is set to true, it will call `level.neighborChanged()` and schedule a tick on the block.<br>(Default: false) |
| <nobr>`max_value`<nobr> | FixedNumberExpression<Number>? | The max value. If it's not defined it will simply use the max value for the IntegerProperty, or just use 1 if it's a BooleanProperty. |


??? "Properties extended from `GroupableSimulationMethod`"
    --8<-- "docs/developers/api_fields/groupable.md"

??? "Properties extended from the base `SimulationMethod`"
    --8<-- "docs/developers/api_fields/base.md"

## Implementing this SimulationMethod in Java
todo