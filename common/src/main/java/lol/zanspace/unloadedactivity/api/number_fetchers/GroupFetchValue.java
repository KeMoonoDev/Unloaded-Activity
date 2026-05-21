package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.api.NumberFetcherRegistry;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import lol.zanspace.unloadedactivity.api.NumberFetcher;


public enum GroupFetchValue implements NumberFetcher {
    GROUP_SUM {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupSum();
        }
    },

    GROUP_COUNT {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 1; // We count ourselves

            return context.activeGroupSimulateData.getGroupCount();
        }
    },

    GROUP_HIGHER_VALUE_COUNT {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupHigherValueCount();
        }
    },

    GROUP_HIGHER_OR_EQUAL_VALUE_COUNT {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 1;

            return context.activeGroupSimulateData.getGroupHigherValueCount() + context.activeGroupSimulateData.getGroupEqualValueCount();
        }
    },

    GROUP_LOWER_VALUE_COUNT {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupLowerValueCount();
        }
    },

    GROUP_LOWER_OR_EQUAL_VALUE_COUNT {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 1;

            return context.activeGroupSimulateData.getGroupLowerValueCount() + context.activeGroupSimulateData.getGroupEqualValueCount();
        }
    },

    GROUP_EQUAL_VALUE_COUNT {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 1;

            return context.activeGroupSimulateData.getGroupEqualValueCount();
        }
    },

    GROUP_RANDOM_HIGHER_VALUE {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupRandomHigherValue();
        }

        @Override
        public boolean isRandom() {
            return true;
        }
    },

    GROUP_RANDOM_HIGHER_OR_EQUAL_VALUE {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupRandomHigherOrEqualValue();
        }

        @Override
        public boolean isRandom() {
            return true;
        }
    },

    GROUP_RANDOM_LOWER_VALUE {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupRandomLowerValue();
        }

        @Override
        public boolean isRandom() {
            return true;
        }
    },

    GROUP_RANDOM_LOWER_OR_EQUAL_VALUE {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupRandomLowerOrEqualValue();
        }

        @Override
        public boolean isRandom() {
            return true;
        }
    },

    GROUP_RANDOM_NOT_EQUAL_VALUE {
        @Override
        public Number evaluate(ValueContext context) {
            if (context.activeGroupSimulateData == null)
                return 0;

            return context.activeGroupSimulateData.getGroupRandomNotEqualValue();
        }

        @Override
        public boolean isRandom() {
            return true;
        }
    };

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ValueContext context) {
        return Long.MAX_VALUE;
    }

    public static void register(NumberFetcherRegistry registry) {
        registry.register(
            UnloadedActivity.id("group_sum"),
            GROUP_SUM
        );

        registry.register(
            UnloadedActivity.id("group_count"),
            GROUP_COUNT
        );

        registry.register(
            UnloadedActivity.id("group_higher_value_count"),
            GROUP_HIGHER_VALUE_COUNT
        );

        registry.register(
            UnloadedActivity.id("group_higher_or_equal_value_count"),
            GROUP_HIGHER_OR_EQUAL_VALUE_COUNT
        );

        registry.register(
            UnloadedActivity.id("group_lower_value_count"),
            GROUP_LOWER_VALUE_COUNT
        );

        registry.register(
            UnloadedActivity.id("group_lower_or_equal_value_count"),
            GROUP_LOWER_OR_EQUAL_VALUE_COUNT
        );

        registry.register(
            UnloadedActivity.id("group_equal_value_count"),
            GROUP_EQUAL_VALUE_COUNT
        );

        registry.register(
            UnloadedActivity.id("group_random_higher_value"),
            GROUP_RANDOM_HIGHER_VALUE
        );

        registry.register(
            UnloadedActivity.id("group_random_higher_or_equal_value"),
            GROUP_RANDOM_HIGHER_OR_EQUAL_VALUE
        );

        registry.register(
            UnloadedActivity.id("group_random_lower_value"),
            GROUP_RANDOM_LOWER_VALUE
        );

        registry.register(
            UnloadedActivity.id("group_random_lower_or_equal_value"),
            GROUP_RANDOM_LOWER_OR_EQUAL_VALUE
        );

        registry.register(
            UnloadedActivity.id("group_random_not_equal_value"),
            GROUP_RANDOM_NOT_EQUAL_VALUE
        );
    };
}