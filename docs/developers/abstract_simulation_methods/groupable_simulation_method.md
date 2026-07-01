# `GroupableSimulationMethod`

!!! note warning "Cannot be assigned"
    This `SimulationMethod` is abstract. Some `SimulationMethod`s may build upon this.

This SimulationMethod allows blocks to simulate together at the same time. This is needed for simulating stuff like ice freezing, or grass growing, where it only grows from the edges.

## Properties

--8<-- "docs/developers/api_fields/groupable.md"

??? "Properties extended from the base `SimulationMethod`"
    --8<-- "docs/developers/api_fields/base.md"

## Implementing this SimulationMethod in Java
todo