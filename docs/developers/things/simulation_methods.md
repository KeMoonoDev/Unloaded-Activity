# `SimulationMethod`s

## What is a `SimulationMethod`?

A `SimulationMethod` is a way of simulating a block. For example, "unloadedactivity:property" simulates increasing a property value of a block, and "unloadedactivity:replace" simulates replacing a block with another.

## How to assign `SimulationMethod`s to blocks/tags

In the file where you define your `SimulationData`, you can add your `SimulationMethod`s by creating a field with any name except for "priority" and "replace" and then defining it there.

The "simulation_method" field must be set to a valid `SimulationMethod` Identifier. To see all available `SimulationMethod`s and their Identifier, see the pages underneath <nobr>`For Developers`>`Available SimulationMethods`</nobr>. 

Here's an example from the sweet berry bush:
```json
{
    "age": {
        "simulation_method": "property",
        "property_name": "age",
        "update_type": "update_clients",

        "conditions": [
            {
                "check": "raw_brightness_above",
                "comparison": "greater_or_equal",
                "value": 9
            }
        ],

        "advance_probability": 0.2
    }
}
```
It creates a `SimulationMethod` with the key "age" with the SimulationMethod "unloadedactivity:property".

You can have several `SimulationMethod`s in one `SimulationData`, and they don't have to be defined in the same file. If you assign one `SimulationMethod` to a tag and one to a block which has that tag, that block will have both `SimulationMethod`s. But if they have the same key, one `SimulationMethod` is gonna merge its data with the other `SimulationMethod`.