const MAX_VALUE = 100;
const MIN_VALUE = 0;

annotation AnnotationData Data on record field;

type AnnotationData record {
    int max;
    int min;
    string pattern;
    boolean active;
};
