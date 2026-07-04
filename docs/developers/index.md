# Introduction

Unloaded Activity makes use of datapacks to know which blocks/blockentities to simulate and how to simulate them. You use datapacks to assign and configure `SimulationMethod`s onto different blocks. If you need more control, you are able to register your own custom `SimulationMethod`s from Java to use inside your datapack.

## Why datapacks?

For flexibility and (hopefully) easier mod support.

Previously, this mod would use an interface to add custom simulation behavior to blocks. The problem with this would be that if a mod wanted to modify the behavior of vanilla blocks it would have to make a new mixin and override the simulation method implemented by one of this mods mixin and copy over the implementation to then do the needed changes.

Another problem is that if a custom crop changes the probability for crop growth, it would also have to copy and paste the simulation method implementation. This could be made easier by making another method specifically for returning growth probability so it'd only need to override that, but it'd still be a bit annoying.

Additionally, if a mod wanted to have Unloaded Activity be optional for the user, it'd have to register different block classes depending on if Unloaded Activity is present or not. One with the simulation methods overridden, and one without the methods.

With datapacks however, you are able to change different values, vanilla block or not, without much trouble, and without having to check if Unloaded Activity is installed or not.

Here's how `data/minecraft/simulate_info/blocks/beetroots.json` looks like:

```json
{
    "age": {
        "advance_probability": {
            "value1": "super",
            "operator": "/",
            "value2": 3
        }
    }
}
```

Basically, this takes the `SimulationMethod` assigned to `age` on the beetroot block and only changes the `advance_probability` field while keeping the rest the same. The rest is defined in `minecraft/simulate_info/tags/crops.json`, which gets applied to all blocks with the `crops` tag.

Now, if a mod wanted to change the behaviour of all crops so that it'd only grow during a certain time of the day, they could make a file in `minecraft/simulate_info/tags/crops.json` and do something like this:

```json
{
    "priority": 999, // Make sure this gets applied AFTER Unloaded Activity applies its SimulationData.
    "age": {
        "advance_probability": {
            "predicate": {
                "check": "current_time",
                "comparison": "less_than",
                "value": 12000
            },
            "success": "super",
            "fail": 0
        }
    }
}
```