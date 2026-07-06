# `ValueExpression`s

## What are `ValueExpression`s?

`ValueExpression`s are values that gets evaluated when simulating. When they get evaluated, they have information about the world and the block it's simulating, so it's able to return different values based on the context.

## `ValueExpression` containers

There are different types of containers for `ValueExpression`s. There is `FixedExpression`, `UpdatingExpression` and `RandomizedExpression`.

### `FixedExpression`

The `ValueExpression` may only get affected stuff that doesn't change over time or isn't random. For example, surrounding blocks or block brightness. Something like local brightness would not be valid because that changes based on the time of day. If you try to create a `FixedExpression` with a `ValueExpression` that doesn't meet the requirements, an error will be thrown.

### `UpdatingExpression`

The `ValueExpression` may only get affected stuff that is predictable. So anything that isn't random. If you try to create a `FixedExpression` with a `ValueExpression` that doesn't meet the requirements, an error will be thrown.

### `RandomizedExpression`

Anything goes