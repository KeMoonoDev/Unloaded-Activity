package dev.moono.unloadedactivity.impl;


import java.util.Optional;

public enum Comparison {
    EQ {
        @Override
        public boolean compare(float v1, float v2) {
            return v1 == v2;
        }
    },
    NE {
        @Override
        public boolean compare(float v1, float v2) {
            return v1 != v2;
        }
    },
    LT {
        @Override
        public boolean compare(float v1, float v2) {
            return v1 < v2;
        }
    },
    LE {
        @Override
        public boolean compare(float v1, float v2) {
            return v1 <= v2;
        }
    },
    GT {
        @Override
        public boolean compare(float v1, float v2) {
            return v1 > v2;
        }
    },
    GE {
        @Override
        public boolean compare(float v1, float v2) {
            return v1 >= v2;
        }
    };

    abstract public boolean compare(float v1, float v2);

    public static Optional<Comparison> fromString(String comparisonString) {
        switch (comparisonString.toLowerCase()) {
            case "equal" -> {
                return Optional.of(Comparison.EQ);
            }
            case "not_equal" -> {
                return Optional.of(Comparison.NE);
            }
            case "less_than" -> {
                return Optional.of(Comparison.LT);
            }
            case "less_or_equal" -> {
                return Optional.of(Comparison.LE);
            }
            case "greater_than" -> {
                return Optional.of(Comparison.GT);
            }
            case "greater_or_equal" -> {
                return Optional.of(Comparison.GE);
            }
        }

        return Optional.empty();
    }
}